package com.example.data.repository

import android.content.Context
import com.example.data.local.BattFoDatabase
import com.example.data.local.SettingsPreferences
import com.example.data.local.entity.BatterySnapshotEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.telemetry.BatteryCalculator
import com.example.data.telemetry.BatteryTelemetryCollector
import com.example.model.BatteryTelemetry
import com.example.model.UserSettings
import com.example.notification.BatteryAlertManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max

class BatteryRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = BattFoDatabase.getInstance(context)
    private val snapshotDao = database.batterySnapshotDao()
    private val sessionDao = database.chargingSessionDao()
    private val collector = BatteryTelemetryCollector(context)
    private val alertManager = BatteryAlertManager(context)
    val preferences = SettingsPreferences(context)

    private val _currentTelemetry = MutableStateFlow(runCatching { collector.readCurrentTelemetry() }.getOrDefault(BatteryTelemetry()))
    val currentTelemetry: StateFlow<BatteryTelemetry> = _currentTelemetry.asStateFlow()

    private var lastSnapshotTimeMs: Long = 0
    private var lastSnapshotPercentage: Int = -1

    init {
        // Collect telemetry flow and update state
        scope.launch {
            collector.telemetryFlow().collect { rawTelemetry ->
                processTelemetry(rawTelemetry)
            }
        }

        // Clean up old data on startup based on retention settings
        scope.launch {
            pruneAccordingToRetention()
        }
    }

    private suspend fun processTelemetry(raw: BatteryTelemetry) {
        val settings = preferences.settings.value

        // Query recent snapshots to calculate rate per hour
        val recentSnapshots = snapshotDao.getRecentSnapshotsList(20)
        val ratePerHour = BatteryCalculator.calculateRatePerHourFromSnapshots(recentSnapshots)

        // Calculate estimated time remaining
        val timeRemaining = BatteryCalculator.calculateEstimatedTimeRemaining(
            telemetry = raw,
            configuredCapacityMah = settings.configuredCapacityMah,
            recentRatePerHour = ratePerHour
        )

        val enriched = raw.copy(
            drainRatePerHour = ratePerHour,
            estimatedTimeRemainingSeconds = timeRemaining
        )

        _currentTelemetry.value = enriched

        // Trigger threshold notifications if configured
        alertManager.checkAndNotify(enriched, settings)

        // Determine if we should record a snapshot:
        // Record if percentage changed, or if 60 seconds passed since last snapshot
        val now = System.currentTimeMillis()
        val percentChanged = lastSnapshotPercentage != enriched.percentage
        val intervalPassed = (now - lastSnapshotTimeMs) >= 60_000L

        if (percentChanged || intervalPassed) {
            recordSnapshot(enriched)
            lastSnapshotTimeMs = now
            lastSnapshotPercentage = enriched.percentage
        }

        // Handle charging session tracking
        handleChargingSessionTracking(enriched)
    }

    private suspend fun recordSnapshot(telemetry: BatteryTelemetry) {
        val entity = BatterySnapshotEntity(
            timestamp = System.currentTimeMillis(),
            percentage = telemetry.percentage,
            status = telemetry.status.name,
            plugType = telemetry.plugType.name,
            voltageMv = telemetry.voltageMv,
            temperatureC = telemetry.temperatureC,
            currentNowMa = telemetry.currentNowMa,
            currentAverageMa = telemetry.currentAverageMa,
            powerWatts = telemetry.powerWatts,
            isScreenOn = telemetry.isScreenOn
        )
        snapshotDao.insert(entity)
    }

    private suspend fun handleChargingSessionTracking(telemetry: BatteryTelemetry) {
        val activeSession = sessionDao.getActiveSession()

        if (telemetry.isCharging) {
            val currentMa = telemetry.currentNowMa?.let { abs(it) } ?: 0
            val wattage = telemetry.powerWatts ?: 0.0
            val temp = telemetry.temperatureC ?: 0f

            if (activeSession == null) {
                // Start new charging session
                val newSession = ChargingSessionEntity(
                    startTime = System.currentTimeMillis(),
                    startPercentage = telemetry.percentage,
                    endPercentage = telemetry.percentage,
                    plugType = telemetry.plugType.name,
                    peakCurrentMa = currentMa,
                    avgCurrentMa = currentMa,
                    peakWattage = wattage,
                    avgWattage = wattage,
                    maxTemperatureC = temp,
                    isCompleted = false
                )
                sessionDao.insert(newSession)
            } else {
                // Update ongoing session
                val updatedPeakCurrent = max(activeSession.peakCurrentMa, currentMa)
                val updatedAvgCurrent = if (activeSession.avgCurrentMa > 0 && currentMa > 0) {
                    (activeSession.avgCurrentMa + currentMa) / 2
                } else {
                    max(activeSession.avgCurrentMa, currentMa)
                }

                val updatedPeakWattage = max(activeSession.peakWattage, wattage)
                val updatedAvgWattage = if (activeSession.avgWattage > 0.0 && wattage > 0.0) {
                    (activeSession.avgWattage + wattage) / 2.0
                } else {
                    max(activeSession.avgWattage, wattage)
                }

                val updatedMaxTemp = max(activeSession.maxTemperatureC, temp)

                val updated = activeSession.copy(
                    endPercentage = telemetry.percentage,
                    peakCurrentMa = updatedPeakCurrent,
                    avgCurrentMa = updatedAvgCurrent,
                    peakWattage = updatedPeakWattage,
                    avgWattage = updatedAvgWattage,
                    maxTemperatureC = updatedMaxTemp
                )
                sessionDao.update(updated)
            }
        } else {
            // Discharging: close any active session
            if (activeSession != null) {
                val completed = activeSession.copy(
                    endTime = System.currentTimeMillis(),
                    endPercentage = telemetry.percentage,
                    isCompleted = true
                )
                sessionDao.update(completed)
            }
        }
    }

    fun getSnapshotsSince(sinceTimestamp: Long): Flow<List<BatterySnapshotEntity>> {
        return snapshotDao.getSnapshotsSince(sinceTimestamp)
    }

    fun getAllChargingSessions(): Flow<List<ChargingSessionEntity>> {
        return sessionDao.getAllSessions()
    }

    suspend fun pruneAccordingToRetention() {
        val days = preferences.settings.value.dataRetentionDays
        if (days > 0) {
            val cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
            snapshotDao.deleteOlderThan(cutoff)
            sessionDao.deleteOlderThan(cutoff)
        }
    }

    suspend fun clearAllHistory() {
        snapshotDao.deleteAll()
        sessionDao.deleteAll()
    }

    suspend fun createBackupJson(): String {
        val snapshots = snapshotDao.getAllSnapshotsList()
        val sessions = sessionDao.getAllSessionsList()
        val currentSettings = preferences.settings.value

        val settingsJson = JSONObject().apply {
            put("themeMode", currentSettings.themeMode.name)
            put("useDynamicColor", currentSettings.useDynamicColor)
            put("amoledMode", currentSettings.amoledMode)
            put("temperatureUnit", currentSettings.temperatureUnit.name)
            put("configuredCapacityMah", currentSettings.configuredCapacityMah)
            put("dataRetentionDays", currentSettings.dataRetentionDays)
            put("samplingIntervalSeconds", currentSettings.samplingIntervalSeconds)
            put("lowBatteryAlertEnabled", currentSettings.lowBatteryAlertEnabled)
            put("lowBatteryThreshold", currentSettings.lowBatteryThreshold)
            put("fullChargeAlertEnabled", currentSettings.fullChargeAlertEnabled)
            put("fullChargeThreshold", currentSettings.fullChargeThreshold)
            put("highTempAlertEnabled", currentSettings.highTempAlertEnabled)
            put("highTempThresholdC", currentSettings.highTempThresholdC.toDouble())
            put("slowChargingAlertEnabled", currentSettings.slowChargingAlertEnabled)
            put("unusualDrainAlertEnabled", currentSettings.unusualDrainAlertEnabled)
        }

        val snapshotArray = JSONArray()
        snapshots.forEach { snapshot ->
            snapshotArray.put(JSONObject().apply {
                put("timestamp", snapshot.timestamp)
                put("percentage", snapshot.percentage)
                put("status", snapshot.status)
                put("plugType", snapshot.plugType)
                put("voltageMv", snapshot.voltageMv ?: JSONObject.NULL)
                put("temperatureC", snapshot.temperatureC?.toDouble() ?: JSONObject.NULL)
                put("currentNowMa", snapshot.currentNowMa ?: JSONObject.NULL)
                put("currentAverageMa", snapshot.currentAverageMa ?: JSONObject.NULL)
                put("powerWatts", snapshot.powerWatts ?: JSONObject.NULL)
                put("isScreenOn", snapshot.isScreenOn)
            })
        }

        val sessionArray = JSONArray()
        sessions.forEach { session ->
            sessionArray.put(JSONObject().apply {
                put("startTime", session.startTime)
                put("endTime", session.endTime ?: JSONObject.NULL)
                put("startPercentage", session.startPercentage)
                put("endPercentage", session.endPercentage)
                put("plugType", session.plugType)
                put("peakCurrentMa", session.peakCurrentMa)
                put("avgCurrentMa", session.avgCurrentMa)
                put("peakWattage", session.peakWattage)
                put("avgWattage", session.avgWattage)
                put("maxTemperatureC", session.maxTemperatureC.toDouble())
                put("isCompleted", session.isCompleted)
            })
        }

        return JSONObject().apply {
            put("backupType", "battfo-backup")
            put("schemaVersion", 1)
            put("appName", "BattFo")
            put("appVersion", com.jbuilds.battfo.BuildConfig.VERSION_NAME)
            put("exportTime", System.currentTimeMillis())
            put("settings", settingsJson)
            put("batteryHistory", JSONObject().apply {
                put("snapshots", snapshotArray)
                put("chargingSessions", sessionArray)
            })
        }.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.optString("backupType") != "battfo-backup") return false
            if (root.optInt("schemaVersion", -1) != 1) return false

            val settingsJson = root.getJSONObject("settings")
            val historyJson = root.getJSONObject("batteryHistory")
            val snapshotArray = historyJson.optJSONArray("snapshots") ?: JSONArray()
            val sessionArray = historyJson.optJSONArray("chargingSessions") ?: JSONArray()

            val themeMode = runCatching {
                com.example.model.ThemeMode.valueOf(
                    settingsJson.optString("themeMode", com.example.model.ThemeMode.SYSTEM.name)
                )
            }.getOrDefault(com.example.model.ThemeMode.SYSTEM)

            val temperatureUnit = runCatching {
                com.example.model.TemperatureUnit.valueOf(
                    settingsJson.optString(
                        "temperatureUnit",
                        com.example.model.TemperatureUnit.CELSIUS.name
                    )
                )
            }.getOrDefault(com.example.model.TemperatureUnit.CELSIUS)

            val restoredSettings = com.example.model.UserSettings(
                themeMode = themeMode,
                useDynamicColor = settingsJson.optBoolean("useDynamicColor", true),
                amoledMode = settingsJson.optBoolean("amoledMode", false),
                temperatureUnit = temperatureUnit,
                configuredCapacityMah = settingsJson.optInt("configuredCapacityMah", 4500),
                dataRetentionDays = settingsJson.optInt("dataRetentionDays", 7),
                samplingIntervalSeconds = settingsJson.optInt("samplingIntervalSeconds", 30),
                lowBatteryAlertEnabled = settingsJson.optBoolean("lowBatteryAlertEnabled", false),
                lowBatteryThreshold = settingsJson.optInt("lowBatteryThreshold", 20),
                fullChargeAlertEnabled = settingsJson.optBoolean("fullChargeAlertEnabled", false),
                fullChargeThreshold = settingsJson.optInt("fullChargeThreshold", 80),
                highTempAlertEnabled = settingsJson.optBoolean("highTempAlertEnabled", false),
                highTempThresholdC = settingsJson.optDouble("highTempThresholdC", 42.0).toFloat(),
                slowChargingAlertEnabled = settingsJson.optBoolean("slowChargingAlertEnabled", false),
                unusualDrainAlertEnabled = settingsJson.optBoolean("unusualDrainAlertEnabled", false)
            )

            val restoredSnapshots = buildList {
                for (i in 0 until snapshotArray.length()) {
                    val obj = snapshotArray.getJSONObject(i)
                    add(
                        BatterySnapshotEntity(
                            timestamp = obj.getLong("timestamp"),
                            percentage = obj.getInt("percentage"),
                            status = obj.optString("status", "UNKNOWN"),
                            plugType = obj.optString("plugType", "UNPLUGGED"),
                            voltageMv = if (obj.isNull("voltageMv")) null else obj.getInt("voltageMv"),
                            temperatureC = if (obj.isNull("temperatureC")) null else obj.getDouble("temperatureC").toFloat(),
                            currentNowMa = if (obj.isNull("currentNowMa")) null else obj.getInt("currentNowMa"),
                            currentAverageMa = if (obj.isNull("currentAverageMa")) null else obj.getInt("currentAverageMa"),
                            powerWatts = if (obj.isNull("powerWatts")) null else obj.getDouble("powerWatts"),
                            isScreenOn = obj.optBoolean("isScreenOn", true)
                        )
                    )
                }
            }

            val restoredSessions = buildList {
                for (i in 0 until sessionArray.length()) {
                    val obj = sessionArray.getJSONObject(i)
                    add(
                        ChargingSessionEntity(
                            startTime = obj.getLong("startTime"),
                            endTime = if (obj.isNull("endTime")) null else obj.getLong("endTime"),
                            startPercentage = obj.getInt("startPercentage"),
                            endPercentage = obj.getInt("endPercentage"),
                            plugType = obj.optString("plugType", "AC"),
                            peakCurrentMa = obj.optInt("peakCurrentMa", 0),
                            avgCurrentMa = obj.optInt("avgCurrentMa", 0),
                            peakWattage = obj.optDouble("peakWattage", 0.0),
                            avgWattage = obj.optDouble("avgWattage", 0.0),
                            maxTemperatureC = obj.optDouble("maxTemperatureC", 0.0).toFloat(),
                            isCompleted = obj.optBoolean("isCompleted", true)
                        )
                    )
                }
            }

            database.withTransaction {
                snapshotDao.deleteAll()
                sessionDao.deleteAll()
                if (restoredSnapshots.isNotEmpty()) {
                    snapshotDao.insertAll(restoredSnapshots)
                }
                if (restoredSessions.isNotEmpty()) {
                    sessionDao.insertAll(restoredSessions)
                }
            }

            preferences.updateSettings(restoredSettings)
            true
        } catch (_: Exception) {
            false
        }
    }

}

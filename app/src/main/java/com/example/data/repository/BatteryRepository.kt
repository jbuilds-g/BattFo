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

    private val _currentTelemetry = MutableStateFlow(collector.readCurrentTelemetry())
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

    suspend fun exportDataJson(): String {
        val snapshots = snapshotDao.getAllSnapshotsList()
        val sessions = sessionDao.getAllSessionsList()

        val root = JSONObject()
        val snapshotArray = JSONArray()
        for (s in snapshots) {
            val obj = JSONObject().apply {
                put("timestamp", s.timestamp)
                put("percentage", s.percentage)
                put("status", s.status)
                put("plugType", s.plugType)
                put("voltageMv", s.voltageMv ?: JSONObject.NULL)
                put("temperatureC", s.temperatureC ?: JSONObject.NULL)
                put("currentNowMa", s.currentNowMa ?: JSONObject.NULL)
                put("powerWatts", s.powerWatts ?: JSONObject.NULL)
                put("isScreenOn", s.isScreenOn)
            }
            snapshotArray.put(obj)
        }

        val sessionArray = JSONArray()
        for (ss in sessions) {
            val obj = JSONObject().apply {
                put("startTime", ss.startTime)
                put("endTime", ss.endTime ?: JSONObject.NULL)
                put("startPercentage", ss.startPercentage)
                put("endPercentage", ss.endPercentage)
                put("plugType", ss.plugType)
                put("peakCurrentMa", ss.peakCurrentMa)
                put("avgCurrentMa", ss.avgCurrentMa)
                put("peakWattage", ss.peakWattage)
                put("avgWattage", ss.avgWattage)
                put("maxTemperatureC", ss.maxTemperatureC)
            }
            sessionArray.put(obj)
        }

        root.put("version", 1)
        root.put("appName", "BattFo")
        root.put("exportTime", System.currentTimeMillis())
        root.put("snapshots", snapshotArray)
        root.put("chargingSessions", sessionArray)

        return root.toString(2)
    }

    suspend fun importDataJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val snapshotArray = root.optJSONArray("snapshots")
            val sessionArray = root.optJSONArray("chargingSessions")

            if (snapshotArray != null) {
                val snapshotList = mutableListOf<BatterySnapshotEntity>()
                for (i in 0 until snapshotArray.length()) {
                    val obj = snapshotArray.getJSONObject(i)
                    snapshotList.add(
                        BatterySnapshotEntity(
                            timestamp = obj.getLong("timestamp"),
                            percentage = obj.getInt("percentage"),
                            status = obj.optString("status", "UNKNOWN"),
                            plugType = obj.optString("plugType", "UNPLUGGED"),
                            voltageMv = if (obj.isNull("voltageMv")) null else obj.getInt("voltageMv"),
                            temperatureC = if (obj.isNull("temperatureC")) null else obj.getDouble("temperatureC").toFloat(),
                            currentNowMa = if (obj.isNull("currentNowMa")) null else obj.getInt("currentNowMa"),
                            currentAverageMa = null,
                            powerWatts = if (obj.isNull("powerWatts")) null else obj.getDouble("powerWatts"),
                            isScreenOn = obj.optBoolean("isScreenOn", true)
                        )
                    )
                }
                if (snapshotList.isNotEmpty()) {
                    snapshotDao.insertAll(snapshotList)
                }
            }

            if (sessionArray != null) {
                val sessionList = mutableListOf<ChargingSessionEntity>()
                for (i in 0 until sessionArray.length()) {
                    val obj = sessionArray.getJSONObject(i)
                    sessionList.add(
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
                            isCompleted = true
                        )
                    )
                }
                if (sessionList.isNotEmpty()) {
                    sessionDao.insertAll(sessionList)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

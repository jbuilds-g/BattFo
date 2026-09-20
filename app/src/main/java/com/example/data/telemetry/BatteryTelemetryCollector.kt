package com.example.data.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.model.BatteryHealth
import com.example.model.BatteryStatus
import com.example.model.BatteryTelemetry
import com.example.model.PlugType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BatteryTelemetryCollector(private val context: Context) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    /**
     * Reads a one-shot snapshot of current battery telemetry directly from system APIs.
     */
    fun readCurrentTelemetry(): BatteryTelemetry {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        return parseTelemetryFromIntent(batteryIntent)
    }

    /**
     * Emits continuous telemetry updates using sticky battery broadcasts + periodic current refresh
     */
    fun telemetryFlow(refreshIntervalMs: Long = 3000L): Flow<BatteryTelemetry> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    trySend(parseTelemetryFromIntent(intent))
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Send initial state immediately
        val initialIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        trySend(parseTelemetryFromIntent(initialIntent))

        // Coroutine loop for refreshing instantaneous current/power while collector is active
        val pollingJob = launch(kotlinx.coroutines.Dispatchers.Default) {
            while (isActive) {
                delay(refreshIntervalMs)
                val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                trySend(parseTelemetryFromIntent(sticky))
            }
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            pollingJob.cancel()
        }
    }

    private fun parseTelemetryFromIntent(intent: Intent?): BatteryTelemetry {
        if (intent == null) {
            return BatteryTelemetry()
        }

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val rawStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val status = when (rawStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
            else -> BatteryStatus.UNKNOWN
        }

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> PlugType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PlugType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && plugged == 8 /* BATTERY_PLUGGED_DOCK */) {
                    PlugType.DOCK
                } else if (plugged > 0) {
                    PlugType.AC
                } else {
                    PlugType.UNPLUGGED
                }
            }
        }

        val rawHealth = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val health = when (rawHealth) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }

        val rawVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val voltageMv = if (rawVoltage > 0) rawVoltage else null

        val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val temperatureC = if (rawTemp > 0) rawTemp / 10f else null

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        val isChargingNow = status == BatteryStatus.CHARGING || status == BatteryStatus.FULL || plugType != PlugType.UNPLUGGED

        // BatteryManager property readings
        var currentNowMa: Int? = null
        var currentAverageMa: Int? = null
        var chargeCounterUah: Long? = null
        var energyCounterNwh: Long? = null
        var cycleCount: Int? = null
        var chargeTimeRemainingMs: Long? = null

        batteryManager?.let { bm ->
            try {
                val rawCurrentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                currentNowMa = BatteryCalculator.normalizeCurrentToMa(rawCurrentNow, isChargingNow)
            } catch (_: Throwable) {}

            try {
                val rawCurrentAvg = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                currentAverageMa = BatteryCalculator.normalizeCurrentToMa(rawCurrentAvg, isChargingNow)
            } catch (_: Throwable) {}

            try {
                val rawChargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                if (rawChargeCounter > 0) {
                    chargeCounterUah = rawChargeCounter
                }
            } catch (_: Throwable) {}

            try {
                val rawEnergyCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
                if (rawEnergyCounter > 0) {
                    energyCounterNwh = rawEnergyCounter
                }
            } catch (_: Throwable) {}

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // BATTERY_PROPERTY_CYCLE_COUNT = 7; requires BATTERY_STATS on some OEM builds
                    val rawCycles = bm.getIntProperty(7)
                    if (rawCycles >= 0) {
                        cycleCount = rawCycles
                    }
                }
            } catch (_: Throwable) {}

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isChargingNow) {
                    val remMs = bm.computeChargeTimeRemaining()
                    if (remMs > 0) {
                        chargeTimeRemainingMs = remMs
                    }
                }
            } catch (_: Throwable) {}
        }

        // Thermal status
        var thermalStatus: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                powerManager?.let { pm ->
                    thermalStatus = when (pm.currentThermalStatus) {
                        PowerManager.THERMAL_STATUS_NONE -> "Nominal"
                        PowerManager.THERMAL_STATUS_LIGHT -> "Light Throttling"
                        PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Throttling"
                        PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttling"
                        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Throttling"
                        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Cooldown"
                        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Shutdown"
                        else -> "Normal"
                    }
                }
            } catch (_: Throwable) {}
        }

        val isScreenOn = try {
            powerManager?.isInteractive ?: true
        } catch (_: Throwable) {
            true
        }
        val powerWatts = BatteryCalculator.calculatePowerWatts(voltageMv, currentNowMa ?: currentAverageMa)

        return BatteryTelemetry(
            percentage = percentage,
            status = status,
            plugType = plugType,
            health = health,
            technology = technology,
            voltageMv = voltageMv,
            temperatureC = temperatureC,
            currentNowMa = currentNowMa,
            currentAverageMa = currentAverageMa,
            powerWatts = powerWatts,
            chargeCounterUah = chargeCounterUah,
            energyCounterNwh = energyCounterNwh,
            cycleCount = cycleCount,
            computedChargeTimeRemainingMs = chargeTimeRemainingMs,
            thermalStatus = thermalStatus,
            isScreenOn = isScreenOn,
            timestamp = System.currentTimeMillis()
        )
    }
}

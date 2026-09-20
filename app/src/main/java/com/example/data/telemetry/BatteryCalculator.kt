package com.example.data.telemetry

import com.example.data.local.entity.BatterySnapshotEntity
import com.example.model.BatteryStatus
import com.example.model.BatteryTelemetry
import kotlin.math.abs

object BatteryCalculator {

    /**
     * Calculates power in Watts given voltage in mV and current in mA.
     * P (Watts) = (V (mV) / 1000.0) * (abs(I (mA)) / 1000.0)
     */
    fun calculatePowerWatts(voltageMv: Int?, currentMa: Int?): Double? {
        if (voltageMv == null || currentMa == null || voltageMv <= 0) return null
        val power = (voltageMv.toDouble() / 1000.0) * (abs(currentMa).toDouble() / 1000.0)
        return if (power > 0.0 && power < 200.0) power else null // Cap at realistic 200W for phones
    }

    /**
     * Calculates drain or charge rate (% per hour) from a list of recent snapshots.
     * Positive means charging (+%/hr), negative means discharging (-%/hr).
     */
    fun calculateRatePerHourFromSnapshots(snapshots: List<BatterySnapshotEntity>): Double? {
        if (snapshots.size < 2) return null
        val newest = snapshots.first()
        val oldest = snapshots.last()

        val timeDiffMs = newest.timestamp - oldest.timestamp
        if (timeDiffMs < 60_000L) return null // Need at least 1 minute of data for meaningful rate

        val hours = timeDiffMs.toDouble() / 3_600_000.0
        if (hours <= 0.0) return null

        val percentDiff = newest.percentage - oldest.percentage
        val rate = percentDiff / hours

        // Clamp to sensible range (-100%/hr to +150%/hr)
        return if (rate >= -100.0 && rate <= 150.0) rate else null
    }

    /**
     * Calculates estimated remaining time in seconds.
     * Discharging: time until 0%.
     * Charging: time until 100%.
     */
    fun calculateEstimatedTimeRemaining(
        telemetry: BatteryTelemetry,
        configuredCapacityMah: Int,
        recentRatePerHour: Double?
    ): Long? {
        // If device provides official charging time remaining via computeChargeTimeRemaining
        if (telemetry.isCharging) {
            telemetry.computedChargeTimeRemainingMs?.let { ms ->
                if (ms > 0) return ms / 1000
            }
        }

        val percentage = telemetry.percentage
        if (percentage <= 0 || percentage >= 100 && telemetry.isCharging) {
            return null
        }

        // When charging:
        if (telemetry.isCharging) {
            val remainingPercent = 100 - percentage
            // Try rate per hour first
            if (recentRatePerHour != null && recentRatePerHour > 0.5) {
                val hours = remainingPercent / recentRatePerHour
                return (hours * 3600.0).toLong().coerceIn(60, 86400)
            }
            // Try current mA
            val currentMa = telemetry.currentNowMa ?: telemetry.currentAverageMa
            if (currentMa != null && abs(currentMa) > 50) {
                val capacityNeededMah = configuredCapacityMah * (remainingPercent / 100.0)
                val hours = capacityNeededMah / abs(currentMa).toDouble()
                return (hours * 3600.0).toLong().coerceIn(60, 86400)
            }
            return null
        }

        // When discharging:
        if (recentRatePerHour != null && recentRatePerHour < -0.2) {
            val hours = percentage / abs(recentRatePerHour)
            return (hours * 3600.0).toLong().coerceIn(300, 345600) // 5 min to 4 days
        }

        // Fallback to instantaneous/average current consumption if available
        val currentMa = telemetry.currentAverageMa ?: telemetry.currentNowMa
        if (currentMa != null && abs(currentMa) > 30) {
            val remainingMah = configuredCapacityMah * (percentage / 100.0)
            val hours = remainingMah / abs(currentMa).toDouble()
            return (hours * 3600.0).toLong().coerceIn(300, 345600)
        }

        return null
    }

    /**
     * Formats remaining time cleanly without misleading precision.
     * E.g. "3h 45m" or "< 10m" or "1d 4h"
     */
    fun formatEstimatedDuration(seconds: Long?): String {
        if (seconds == null || seconds <= 0) return "Calculating..."
        val minutes = (seconds / 60)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> {
                val remHours = hours % 24
                if (remHours > 0) "${days}d ${remHours}h" else "${days}d"
            }
            hours > 0 -> {
                val remMinutes = minutes % 60
                "${hours}h ${remMinutes}m"
            }
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    /**
     * Normalizes current readings across different OEM implementations.
     * Standard BatteryManager returns microamperes (uA). Some OEMs return mA.
     * Also normalizes sign: positive = charging into battery, negative = discharging out.
     */
    fun normalizeCurrentToMa(rawPropertyVal: Int, isCharging: Boolean): Int? {
        if (rawPropertyVal == Int.MIN_VALUE || rawPropertyVal == 0) return null
        
        var ma = if (abs(rawPropertyVal) > 10_000) {
            rawPropertyVal / 1000 // Was microamperes
        } else {
            rawPropertyVal // Was already mA
        }

        // Ensure reasonable limits (-15000mA to 15000mA for fast charging / heavy gaming)
        if (abs(ma) > 20_000) return null

        // OEM sign alignment:
        // Some OEMs return negative current during charging, or positive during discharging.
        // We ensure standard convention: Discharging is negative, Charging is positive.
        if (isCharging && ma < 0) {
            ma = -ma
        } else if (!isCharging && ma > 0) {
            ma = -ma
        }

        return ma
    }
}

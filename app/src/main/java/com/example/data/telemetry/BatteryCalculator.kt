package com.example.data.telemetry

import com.example.data.local.entity.BatterySnapshotEntity
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
        return if (power > 0.0 && power < 200.0) power else null
    }

    /**
     * Calculates the simple battery rate from snapshots.
     * This is useful for telemetry display, but should not be used by itself
     * as a real-world runtime estimate because idle and active usage differ.
     */
    fun calculateRatePerHourFromSnapshots(snapshots: List<BatterySnapshotEntity>): Double? {
        if (snapshots.size < 2) return null
        val newest = snapshots.first()
        val oldest = snapshots.last()

        val timeDiffMs = newest.timestamp - oldest.timestamp
        if (timeDiffMs < 60_000L) return null

        val hours = timeDiffMs.toDouble() / 3_600_000.0
        if (hours <= 0.0) return null

        val percentDiff = newest.percentage - oldest.percentage
        val rate = percentDiff / hours
        return if (rate >= -100.0 && rate <= 150.0) rate else null
    }

    /**
     * Estimates active-use discharge rate from historical snapshots.
     *
     * Only uses periods where the screen was on and the battery was discharging.
     * This intentionally returns null when there is not enough representative
     * history instead of falling back to an instantaneous current reading.
     */
    fun calculateActiveDischargeRatePerHour(
        snapshots: List<BatterySnapshotEntity>,
        minimumMinutes: Long = 15L
    ): Double? {
        if (snapshots.size < 3) return null

        val ordered = snapshots.sortedBy { it.timestamp }
        val rates = mutableListOf<Pair<Double, Double>>() // rate, interval hours

        ordered.zipWithNext().forEach { (previous, current) ->
            if (!previous.isScreenOn || !current.isScreenOn) return@forEach
            if (previous.status != "DISCHARGING" || current.status != "DISCHARGING") return@forEach

            val elapsedMs = current.timestamp - previous.timestamp
            if (elapsedMs !in 30_000L..3_600_000L) return@forEach

            val percentDrop = previous.percentage - current.percentage
            if (percentDrop <= 0) return@forEach

            val hours = elapsedMs.toDouble() / 3_600_000.0
            val rate = percentDrop / hours
            if (rate in 0.1..30.0) {
                rates += rate to hours
            }
        }

        if (rates.size < 3) return null

        val totalHours = rates.sumOf { it.second }
        if (totalHours * 60.0 < minimumMinutes) return null

        // Reject extreme intervals caused by transient/OEM battery percentage jumps.
        val sortedRates = rates.map { it.first }.sorted()
        val median = sortedRates[sortedRates.size / 2]
        val stableRates = rates.filter { it.first in (median * 0.5)..(median * 1.5) }
        if (stableRates.size < 3) return null

        val stableHours = stableRates.sumOf { it.second }
        if (stableHours * 60.0 < minimumMinutes) return null

        return stableRates.sumOf { it.first * it.second } / stableHours
    }

    /**
     * Calculates estimated remaining time in seconds.
     * Discharging estimates use representative active-use history only.
     * Charging prefers Android's official estimate, then historical charge rate.
     */
    fun calculateEstimatedTimeRemaining(
        telemetry: BatteryTelemetry,
        configuredCapacityMah: Int,
        recentRatePerHour: Double?,
        activeDischargeRatePerHour: Double? = null
    ): Long? {
        if (telemetry.isCharging) {
            telemetry.computedChargeTimeRemainingMs?.let { ms ->
                if (ms > 0) return ms / 1000
            }
        }

        val percentage = telemetry.percentage
        if (percentage <= 0 || percentage >= 100 && telemetry.isCharging) return null

        if (telemetry.isCharging) {
            val remainingPercent = 100 - percentage
            if (recentRatePerHour != null && recentRatePerHour > 0.5) {
                val hours = remainingPercent / recentRatePerHour
                return (hours * 3600.0).toLong().coerceIn(60, 86400)
            }

            val currentMa = telemetry.currentNowMa ?: telemetry.currentAverageMa
            if (currentMa != null && abs(currentMa) > 50) {
                val capacityNeededMah = configuredCapacityMah * (remainingPercent / 100.0)
                val hours = capacityNeededMah / abs(currentMa).toDouble()
                return (hours * 3600.0).toLong().coerceIn(60, 86400)
            }
            return null
        }

        // Do not present an idle/current-draw estimate as the user's expected runtime.
        val rate = activeDischargeRatePerHour ?: return null
        if (rate <= 0.2) return null

        val hours = percentage / rate
        return (hours * 3600.0).toLong().coerceIn(300, 345600)
    }

    /**
     * Formats remaining time cleanly without misleading precision.
     */
    fun formatEstimatedDuration(seconds: Long?): String {
        if (seconds == null || seconds <= 0) return "Calculating..."
        val minutes = seconds / 60
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
     * Positive = charging, negative = discharging.
     */
    fun normalizeCurrentToMa(rawPropertyVal: Int, isCharging: Boolean): Int? {
        if (rawPropertyVal == Int.MIN_VALUE || rawPropertyVal == 0) return null

        var ma = if (abs(rawPropertyVal) > 10_000) {
            rawPropertyVal / 1000
        } else {
            rawPropertyVal
        }

        if (abs(ma) > 20_000) return null

        if (isCharging && ma < 0) {
            ma = -ma
        } else if (!isCharging && ma > 0) {
            ma = -ma
        }

        return ma
    }
}

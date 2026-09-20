package com.example.model

enum class BatteryStatus {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN
}

enum class PlugType {
    UNPLUGGED,
    AC,
    USB,
    WIRELESS,
    DOCK
}

enum class BatteryHealth {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD,
    UNKNOWN
}

enum class MetricReliability {
    MEASURED,
    ESTIMATED,
    UNAVAILABLE
}

data class BatteryTelemetry(
    val percentage: Int = 0,
    val status: BatteryStatus = BatteryStatus.UNKNOWN,
    val plugType: PlugType = PlugType.UNPLUGGED,
    val health: BatteryHealth = BatteryHealth.UNKNOWN,
    val technology: String = "Unknown",
    val voltageMv: Int? = null,
    val temperatureC: Float? = null,
    val currentNowMa: Int? = null,
    val currentAverageMa: Int? = null,
    val powerWatts: Double? = null,
    val chargeCounterUah: Long? = null,
    val energyCounterNwh: Long? = null,
    val cycleCount: Int? = null,
    val computedChargeTimeRemainingMs: Long? = null,
    val thermalStatus: String? = null,
    val isScreenOn: Boolean = true,
    val drainRatePerHour: Double? = null,
    val estimatedTimeRemainingSeconds: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isCharging: Boolean
        get() = status == BatteryStatus.CHARGING || status == BatteryStatus.FULL || plugType != PlugType.UNPLUGGED

    val chargeCounterMah: Long?
        get() = chargeCounterUah?.let { it / 1000 }

    val energyCounterMwh: Long?
        get() = energyCounterNwh?.let { it / 1000000 }
}

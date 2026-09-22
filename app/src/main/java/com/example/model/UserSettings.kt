package com.example.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val amoledMode: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val configuredCapacityMah: Int = 4500,
    val dataRetentionDays: Int = 7, // 1, 7, 30, -1 for unlimited
    val samplingIntervalSeconds: Int = 30,
    val lowBatteryAlertEnabled: Boolean = false,
    val lowBatteryThreshold: Int = 20,
    val fullChargeAlertEnabled: Boolean = false,
    val fullChargeThreshold: Int = 80,
    val highTempAlertEnabled: Boolean = false,
    val highTempThresholdC: Float = 42.0f,
    val slowChargingAlertEnabled: Boolean = false,
    val unusualDrainAlertEnabled: Boolean = false
)

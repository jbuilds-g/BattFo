package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.TemperatureUnit
import com.example.model.ThemeMode
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("battfo_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM)

        val tempUnitStr = prefs.getString("temp_unit", TemperatureUnit.CELSIUS.name) ?: TemperatureUnit.CELSIUS.name
        val tempUnit = runCatching { TemperatureUnit.valueOf(tempUnitStr) }.getOrDefault(TemperatureUnit.CELSIUS)

        return UserSettings(
            themeMode = themeMode,
            useDynamicColor = prefs.getBoolean("dynamic_color", true),
            temperatureUnit = tempUnit,
            configuredCapacityMah = prefs.getInt("configured_capacity_mah", 4500),
            dataRetentionDays = prefs.getInt("retention_days", 7),
            samplingIntervalSeconds = prefs.getInt("sampling_interval_sec", 30),
            lowBatteryAlertEnabled = prefs.getBoolean("low_battery_alert", false),
            lowBatteryThreshold = prefs.getInt("low_battery_thresh", 20),
            fullChargeAlertEnabled = prefs.getBoolean("full_charge_alert", false),
            fullChargeThreshold = prefs.getInt("full_charge_thresh", 80),
            highTempAlertEnabled = prefs.getBoolean("high_temp_alert", false),
            highTempThresholdC = prefs.getFloat("high_temp_thresh", 42.0f),
            slowChargingAlertEnabled = prefs.getBoolean("slow_charging_alert", false),
            unusualDrainAlertEnabled = prefs.getBoolean("unusual_drain_alert", false)
        )
    }

    fun updateSettings(newSettings: UserSettings) {
        prefs.edit()
            .putString("theme_mode", newSettings.themeMode.name)
            .putBoolean("dynamic_color", newSettings.useDynamicColor)
            .putString("temp_unit", newSettings.temperatureUnit.name)
            .putInt("configured_capacity_mah", newSettings.configuredCapacityMah)
            .putInt("retention_days", newSettings.dataRetentionDays)
            .putInt("sampling_interval_sec", newSettings.samplingIntervalSeconds)
            .putBoolean("low_battery_alert", newSettings.lowBatteryAlertEnabled)
            .putInt("low_battery_thresh", newSettings.lowBatteryThreshold)
            .putBoolean("full_charge_alert", newSettings.fullChargeAlertEnabled)
            .putInt("full_charge_thresh", newSettings.fullChargeThreshold)
            .putBoolean("high_temp_alert", newSettings.highTempAlertEnabled)
            .putFloat("high_temp_thresh", newSettings.highTempThresholdC)
            .putBoolean("slow_charging_alert", newSettings.slowChargingAlertEnabled)
            .putBoolean("unusual_drain_alert", newSettings.unusualDrainAlertEnabled)
            .apply()

        _settings.value = newSettings
    }
}

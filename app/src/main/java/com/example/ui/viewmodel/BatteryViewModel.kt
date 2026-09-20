package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BatterySnapshotEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.repository.BatteryRepository
import com.example.model.BatteryTelemetry
import com.example.model.TemperatureUnit
import com.example.model.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryRange(val label: String, val durationMs: Long) {
    ONE_HOUR("1h", 1L * 60 * 60 * 1000),
    SIX_HOURS("6h", 6L * 60 * 60 * 1000),
    TWELVE_HOURS("12h", 12L * 60 * 60 * 1000),
    TWENTY_FOUR_HOURS("24h", 24L * 60 * 60 * 1000),
    SEVEN_DAYS("7d", 7L * 24 * 60 * 60 * 1000)
}

enum class HistoryMetric(val label: String) {
    PERCENTAGE("Battery %"),
    TEMPERATURE("Temperature"),
    VOLTAGE("Voltage"),
    POWER("Power")
}

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    val repository = BatteryRepository(application)

    val currentTelemetry: StateFlow<BatteryTelemetry> = repository.currentTelemetry

    val settings: StateFlow<UserSettings> = repository.preferences.settings

    private val _selectedRange = MutableStateFlow(HistoryRange.TWELVE_HOURS)
    val selectedRange: StateFlow<HistoryRange> = _selectedRange.asStateFlow()

    private val _selectedMetric = MutableStateFlow(HistoryMetric.PERCENTAGE)
    val selectedMetric: StateFlow<HistoryMetric> = _selectedMetric.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historySnapshots: StateFlow<List<BatterySnapshotEntity>> = _selectedRange
        .flatMapLatest { range ->
            val cutoff = System.currentTimeMillis() - range.durationMs
            repository.getSnapshotsSince(cutoff)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chargingSessions: StateFlow<List<ChargingSessionEntity>> = repository.getAllChargingSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setHistoryRange(range: HistoryRange) {
        _selectedRange.value = range
    }

    fun setHistoryMetric(metric: HistoryMetric) {
        _selectedMetric.value = metric
    }

    fun updateSettings(newSettings: UserSettings) {
        repository.preferences.updateSettings(newSettings)
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    suspend fun exportDataJson(): String {
        return repository.exportDataJson()
    }

    suspend fun importDataJson(jsonString: String): Boolean {
        return repository.importDataJson(jsonString)
    }

    fun formatTemperature(tempC: Float?): String {
        if (tempC == null) return "Unavailable"
        return when (settings.value.temperatureUnit) {
            TemperatureUnit.CELSIUS -> String.format("%.1f °C", tempC)
            TemperatureUnit.FAHRENHEIT -> {
                val f = (tempC * 9f / 5f) + 32f
                String.format("%.1f °F", f)
            }
        }
    }
}

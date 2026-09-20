package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PlugType
import com.example.ui.components.TelemetryMetricCard
import com.example.ui.viewmodel.BatteryViewModel
import java.util.Locale

@Composable
fun DetailsScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.currentTelemetry.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("details_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "State & Power Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        TelemetryMetricCard(
            label = "Battery Percentage",
            value = "${telemetry.percentage}%",
            leadingIcon = Icons.Default.BatteryChargingFull,
            subtext = "State of charge (SoC)"
        )

        val formattedStatus = when (telemetry.status) {
            com.example.model.BatteryStatus.CHARGING -> "Charging"
            com.example.model.BatteryStatus.DISCHARGING -> "Discharging"
            com.example.model.BatteryStatus.FULL -> "Full (charged)"
            com.example.model.BatteryStatus.NOT_CHARGING -> "Not charging"
            com.example.model.BatteryStatus.UNKNOWN -> "Unknown"
        }
        TelemetryMetricCard(
            label = "Battery Status",
            value = formattedStatus,
            leadingIcon = Icons.Default.Power,
            subtext = "System battery state reported by OS"
        )

        if (telemetry.plugType != PlugType.UNPLUGGED) {
            val formattedPlug = telemetry.plugType.name.lowercase().replaceFirstChar { it.uppercase() }
            TelemetryMetricCard(
                label = "Charge Power Source",
                value = formattedPlug,
                leadingIcon = Icons.Default.Bolt,
                subtext = "Active connection type"
            )
        }

        if (telemetry.currentNowMa != null) {
            TelemetryMetricCard(
                label = "Instantaneous Current",
                value = "${telemetry.currentNowMa} mA",
                leadingIcon = Icons.Default.Bolt,
                subtext = "Live current flow from battery sensor"
            )
        }

        if (telemetry.currentAverageMa != null) {
            TelemetryMetricCard(
                label = "Average Current",
                value = "${telemetry.currentAverageMa} mA",
                leadingIcon = Icons.Default.Speed,
                subtext = "Rolling average current over recent window"
            )
        }

        if (telemetry.voltageMv != null) {
            TelemetryMetricCard(
                label = "Terminal Voltage",
                value = "${telemetry.voltageMv} mV (${String.format(Locale.US, "%.3f V", telemetry.voltageMv!! / 1000f)})",
                leadingIcon = Icons.Default.ElectricMeter,
                subtext = "Voltage across battery terminals"
            )
        }

        if (telemetry.temperatureC != null) {
            TelemetryMetricCard(
                label = "Battery Temperature",
                value = viewModel.formatTemperature(telemetry.temperatureC),
                leadingIcon = Icons.Default.Thermostat,
                subtext = "Internal thermistor sensor telemetry"
            )
        }

        if (telemetry.powerWatts != null) {
            TelemetryMetricCard(
                label = "Power Draw",
                value = String.format(Locale.US, "%.3f W", telemetry.powerWatts),
                leadingIcon = Icons.Default.FlashOn,
                subtext = "Calculated: Voltage (V) × Current (A)",
                isCalculated = true
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Hardware & Chemistry",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        TelemetryMetricCard(
            label = "Hardware Manufacturer",
            value = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            leadingIcon = Icons.Outlined.Smartphone,
            subtext = "Device manufacturer"
        )

        TelemetryMetricCard(
            label = "Device Model",
            value = "${Build.MODEL} (${Build.DEVICE})",
            leadingIcon = Icons.Default.Memory,
            subtext = "Hardware codename and model"
        )

        if (telemetry.technology.isNotBlank() && telemetry.technology != "Unknown") {
            TelemetryMetricCard(
                label = "Battery Chemistry",
                value = telemetry.technology,
                leadingIcon = Icons.Default.Memory,
                subtext = "Reported cell technology"
            )
        }

        val formattedHealth = telemetry.health.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
        TelemetryMetricCard(
            label = "Hardware Health Report",
            value = formattedHealth,
            leadingIcon = Icons.Default.HealthAndSafety,
            subtext = "Hardware self-diagnostics condition"
        )

        if (telemetry.chargeCounterMah != null) {
            TelemetryMetricCard(
                label = "Remaining Charge Counter",
                value = "${telemetry.chargeCounterMah} mAh (${telemetry.chargeCounterUah} μAh)",
                leadingIcon = Icons.Default.ElectricMeter,
                subtext = "Hardware battery fuel-gauge counter"
            )
        }

        TelemetryMetricCard(
            label = "Configured Design Capacity",
            value = "${settings.configuredCapacityMah} mAh",
            leadingIcon = Icons.Default.ElectricMeter,
            subtext = "User-configured benchmark capacity",
            isCalculated = true
        )

        if (telemetry.energyCounterMwh != null) {
            TelemetryMetricCard(
                label = "Remaining Energy",
                value = "${telemetry.energyCounterMwh} mWh",
                leadingIcon = Icons.Default.Timeline,
                subtext = "Hardware remaining energy counter"
            )
        }

        if (telemetry.cycleCount != null) {
            TelemetryMetricCard(
                label = "Battery Charge Cycle Count",
                value = "${telemetry.cycleCount} cycles",
                leadingIcon = Icons.Default.Refresh,
                subtext = "Total full charge-discharge cycles"
            )
        }

        telemetry.thermalStatus?.let { status ->
            TelemetryMetricCard(
                label = "Thermal Throttling State",
                value = status,
                leadingIcon = Icons.Default.Thermostat,
                subtext = "Operating system thermal status"
            )
        }
    }
}

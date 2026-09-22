package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BatteryStatus
import com.example.model.PlugType
import com.example.ui.components.TelemetryMetricCard
import com.example.ui.viewmodel.BatteryViewModel
import java.util.Locale

@Composable
private fun DetailsSection(
    title: String,
    summary: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring()),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun DetailsScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.currentTelemetry.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var stateExpanded by remember { mutableStateOf(true) }
    var hardwareExpanded by remember { mutableStateOf(false) }
    var thermalExpanded by remember { mutableStateOf(false) }

    val status = when (telemetry.status) {
        BatteryStatus.CHARGING -> "Charging"
        BatteryStatus.DISCHARGING -> "Discharging"
        BatteryStatus.FULL -> "Full"
        BatteryStatus.NOT_CHARGING -> "Not charging"
        BatteryStatus.UNKNOWN -> "Unknown"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("details_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailsSection(
            title = "State & Power",
            summary = "Current battery state, current, voltage and power",
            icon = Icons.Default.BatteryChargingFull,
            expanded = stateExpanded,
            onToggle = { stateExpanded = !stateExpanded }
        ) {
            TelemetryMetricCard(
                label = "Battery Percentage",
                value = "${telemetry.percentage}%",
                leadingIcon = Icons.Default.BatteryChargingFull,
                subtext = "State of charge (SoC)"
            )
            TelemetryMetricCard(
                label = "Battery Status",
                value = status,
                leadingIcon = Icons.Default.Power,
                subtext = "System battery state reported by OS"
            )

            if (telemetry.plugType != PlugType.UNPLUGGED) {
                TelemetryMetricCard(
                    label = "Charge Power Source",
                    value = telemetry.plugType.name.lowercase().replaceFirstChar { it.uppercase() },
                    leadingIcon = Icons.Default.Bolt,
                    subtext = "Active connection type"
                )
            }

            telemetry.currentNowMa?.let {
                TelemetryMetricCard(
                    label = "Instantaneous Current",
                    value = "${it} mA",
                    leadingIcon = Icons.Default.Bolt,
                    subtext = "Live current flow from battery sensor"
                )
            }

            telemetry.currentAverageMa?.let {
                TelemetryMetricCard(
                    label = "Average Current",
                    value = "${it} mA",
                    leadingIcon = Icons.Default.Speed,
                    subtext = "Rolling average current over recent window"
                )
            }

            telemetry.voltageMv?.let {
                TelemetryMetricCard(
                    label = "Terminal Voltage",
                    value = "${it} mV (${String.format(Locale.US, "%.3f V", it / 1000f)})",
                    leadingIcon = Icons.Default.ElectricMeter,
                    subtext = "Voltage across battery terminals"
                )
            }

            telemetry.powerWatts?.let {
                TelemetryMetricCard(
                    label = "Power Draw",
                    value = String.format(Locale.US, "%.3f W", it),
                    leadingIcon = Icons.Default.FlashOn,
                    subtext = "Calculated from voltage and current",
                    isCalculated = true
                )
            }
        }

        DetailsSection(
            title = "Battery & Hardware",
            summary = "Capacity, chemistry, health and device information",
            icon = Icons.Default.ElectricMeter,
            expanded = hardwareExpanded,
            onToggle = { hardwareExpanded = !hardwareExpanded }
        ) {
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

            TelemetryMetricCard(
                label = "Hardware Health Report",
                value = telemetry.health.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
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
        }

        DetailsSection(
            title = "Thermal & Environment",
            summary = "Battery temperature and thermal state",
            icon = Icons.Default.Thermostat,
            expanded = thermalExpanded,
            onToggle = { thermalExpanded = !thermalExpanded }
        ) {
            telemetry.temperatureC?.let {
                TelemetryMetricCard(
                    label = "Battery Temperature",
                    value = viewModel.formatTemperature(it),
                    leadingIcon = Icons.Default.Thermostat,
                    subtext = "Internal thermistor sensor telemetry"
                )
            }

            telemetry.thermalStatus?.let {
                TelemetryMetricCard(
                    label = "Thermal Throttling State",
                    value = it,
                    leadingIcon = Icons.Default.Thermostat,
                    subtext = "Operating system thermal status"
                )
            }
        }
    }
}

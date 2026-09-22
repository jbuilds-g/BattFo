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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
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
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring()),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
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
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
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
            TelemetryMetricCard("Battery Percentage", "${telemetry.percentage}%", Icons.Default.BatteryChargingFull, "State of charge (SoC)")
            TelemetryMetricCard("Battery Status", status, Icons.Default.Power, "System battery state reported by OS")

            if (telemetry.plugType != PlugType.UNPLUGGED) {
                TelemetryMetricCard("Charge Power Source", telemetry.plugType.name.lowercase().replaceFirstChar { it.uppercase() }, Icons.Default.Bolt, "Active connection type")
            }
            telemetry.currentNowMa?.let {
                TelemetryMetricCard("Instantaneous Current", "${it} mA", Icons.Default.Bolt, "Live current flow from battery sensor")
            }
            telemetry.currentAverageMa?.let {
                TelemetryMetricCard("Average Current", "${it} mA", Icons.Default.Speed, "Rolling average current over recent window")
            }
            telemetry.voltageMv?.let {
                TelemetryMetricCard("Terminal Voltage", "${it} mV (${String.format(Locale.US, "%.3f V", it / 1000f)})", Icons.Default.ElectricMeter, "Voltage across battery terminals")
            }
            telemetry.powerWatts?.let {
                TelemetryMetricCard("Power Draw", String.format(Locale.US, "%.3f W", it), Icons.Default.FlashOn, "Calculated from voltage and current", isCalculated = true)
            }
        }

        DetailsSection(
            title = "Battery & Hardware",
            summary = "Capacity, chemistry, health and device information",
            icon = Icons.Default.ElectricMeter,
            expanded = hardwareExpanded,
            onToggle = { hardwareExpanded = !hardwareExpanded }
        ) {
            TelemetryMetricCard("Hardware Manufacturer", Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, Icons.Outlined.Smartphone, "Device manufacturer")
            TelemetryMetricCard("Device Model", "${Build.MODEL} (${Build.DEVICE})", Icons.Default.Memory, "Hardware codename and model")

            if (telemetry.technology.isNotBlank() && telemetry.technology != "Unknown") {
                TelemetryMetricCard("Battery Chemistry", telemetry.technology, Icons.Default.Memory, "Reported cell technology")
            }
            TelemetryMetricCard("Hardware Health Report", telemetry.health.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, Icons.Default.HealthAndSafety, "Hardware self-diagnostics condition")

            telemetry.chargeCounterMah?.let {
                TelemetryMetricCard("Remaining Charge Counter", "${telemetry.chargeCounterMah} mAh (${telemetry.chargeCounterUah} μAh)", Icons.Default.ElectricMeter, "Hardware battery fuel-gauge counter")
            }
            TelemetryMetricCard("Configured Design Capacity", "${settings.configuredCapacityMah} mAh", Icons.Default.ElectricMeter, "User-configured benchmark capacity", isCalculated = true)

            telemetry.energyCounterMwh?.let {
                TelemetryMetricCard("Remaining Energy", "${it} mWh", Icons.Default.Timeline, "Hardware remaining energy counter")
            }
            telemetry.cycleCount?.let {
                TelemetryMetricCard("Battery Charge Cycle Count", "${it} cycles", Icons.Default.Refresh, "Total full charge-discharge cycles")
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
                TelemetryMetricCard("Battery Temperature", viewModel.formatTemperature(it), Icons.Default.Thermostat, "Internal thermistor sensor telemetry")
            }
            telemetry.thermalStatus?.let {
                TelemetryMetricCard("Thermal Throttling State", it, Icons.Default.Thermostat, "Operating system thermal status")
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.telemetry.BatteryCalculator
import com.example.model.BatteryStatus
import com.example.model.PlugType
import com.example.ui.components.BatteryVisualIndicator
import com.example.ui.components.CollapsibleSection
import com.example.ui.components.TelemetryMetricCard
import com.example.ui.viewmodel.BatteryViewModel
import java.util.Locale

private data class QuickTileData(
    val label: String,
    val value: String,
    val icon: ImageVector
)

@Composable
fun DashboardScreen(
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
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Immediate, scannable battery telemetry
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_battery_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row: Status pill + Power source badge + Battery gauge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (statusColor, statusText) = when (telemetry.status) {
                        BatteryStatus.CHARGING -> Pair(MaterialTheme.colorScheme.primary, "Charging")
                        BatteryStatus.DISCHARGING -> Pair(MaterialTheme.colorScheme.secondary, "Discharging")
                        BatteryStatus.FULL -> Pair(MaterialTheme.colorScheme.primary, "Full")
                        BatteryStatus.NOT_CHARGING -> Pair(MaterialTheme.colorScheme.tertiary, "Not charging")
                        BatteryStatus.UNKNOWN -> Pair(MaterialTheme.colorScheme.outline, "Unknown")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (telemetry.plugType != PlugType.UNPLUGGED) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = telemetry.plugType.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    BatteryVisualIndicator(
                        percentage = telemetry.percentage,
                        isCharging = telemetry.isCharging
                    )
                }

                // Main Battery Percentage Hero Display
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${telemetry.percentage}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 64.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    // Rate badge (%/hr)
                    telemetry.drainRatePerHour?.let { rate ->
                        val isPositive = rate > 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPositive) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.Default.Bolt else Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.1f", rate)}%/hr",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }
                    }
                }

                // Estimated time remaining callout
                val timeRemainingStr = BatteryCalculator.formatEstimatedDuration(telemetry.estimatedTimeRemainingSeconds)
                val estimateLabel = if (telemetry.isCharging) {
                    "Until full: $timeRemainingStr"
                } else {
                    "Remaining runtime: $timeRemainingStr"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = estimateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Dynamic Quick Telemetry Grid (Only shows supported metrics)
                val quickTiles = remember(telemetry, settings.temperatureUnit) {
                    buildList {
                        telemetry.powerWatts?.let {
                            add(QuickTileData("Power Draw", String.format(Locale.US, "%.1f W", it), Icons.Default.FlashOn))
                        }
                        telemetry.temperatureC?.let {
                            add(QuickTileData("Temperature", viewModel.formatTemperature(it), Icons.Default.Thermostat))
                        }
                        telemetry.voltageMv?.let {
                            add(QuickTileData("Voltage", "${it} mV", Icons.Default.ElectricMeter))
                        }
                        val currentVal = telemetry.currentNowMa?.let { "${it} mA" }
                            ?: telemetry.currentAverageMa?.let { "${it} mA (avg)" }
                        if (currentVal != null) {
                            add(QuickTileData("Current", currentVal, Icons.Default.Bolt))
                        }
                    }
                }

                quickTiles.chunked(2).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTiles.forEach { tile ->
                            HeroQuickTile(
                                label = tile.label,
                                value = tile.value,
                                icon = tile.icon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowTiles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Collapsible Section 1: Power & Current Flow (Default Collapsed)
        CollapsibleSection(
            title = "Power & Current Flow",
            leadingIcon = Icons.Default.FlashOn,
            summaryText = "Instantaneous and average current flow",
            initiallyExpanded = false,
            testTag = "collapsible_power_flow"
        ) {
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
            if (telemetry.powerWatts != null) {
                TelemetryMetricCard(
                    label = "Power Consumption",
                    value = String.format(Locale.US, "%.2f", telemetry.powerWatts),
                    leadingIcon = Icons.Default.FlashOn,
                    unit = "W",
                    subtext = "Voltage (V) × Current (A)",
                    isCalculated = true
                )
            }
        }

        // Collapsible Section 2: Drain & Runtime Telemetry (Default Collapsed)
        CollapsibleSection(
            title = "Drain & Runtime Telemetry",
            leadingIcon = Icons.Default.HourglassEmpty,
            summaryText = "Discharge estimates and screen state",
            initiallyExpanded = false,
            testTag = "collapsible_drain_telemetry"
        ) {
            if (telemetry.drainRatePerHour != null) {
                TelemetryMetricCard(
                    label = "Current Drain Rate",
                    value = String.format(Locale.US, "%.2f", telemetry.drainRatePerHour),
                    leadingIcon = Icons.Default.Speed,
                    unit = "%/hr",
                    subtext = "Rate calculated from local snapshots",
                    isCalculated = true
                )
            }
            if (telemetry.estimatedTimeRemainingSeconds != null) {
                TelemetryMetricCard(
                    label = "Estimated Remaining Runtime",
                    value = BatteryCalculator.formatEstimatedDuration(telemetry.estimatedTimeRemainingSeconds),
                    leadingIcon = Icons.Default.Timer,
                    subtext = "Based on current load and capacity (${settings.configuredCapacityMah} mAh)",
                    isCalculated = true
                )
            }
            TelemetryMetricCard(
                label = "Screen Display State",
                value = if (telemetry.isScreenOn) "Screen Interactive (On)" else "Screen Standby (Off)",
                leadingIcon = Icons.Outlined.Smartphone,
                subtext = "Interactive power state reported by PowerManager"
            )
        }

        // Collapsible Section 3: Cell Health & Environment (Default Collapsed)
        CollapsibleSection(
            title = "Cell Health & Environment",
            leadingIcon = Icons.Default.HealthAndSafety,
            summaryText = "Battery health, chemistry, and thermal status",
            initiallyExpanded = false,
            testTag = "collapsible_health_env"
        ) {
            TelemetryMetricCard(
                label = "Battery Health Status",
                value = telemetry.health.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                leadingIcon = Icons.Default.Favorite,
                subtext = "Self-diagnostic report from battery hardware"
            )
            if (telemetry.technology.isNotBlank() && telemetry.technology != "Unknown") {
                TelemetryMetricCard(
                    label = "Battery Technology",
                    value = telemetry.technology,
                    leadingIcon = Icons.Default.Memory,
                    subtext = "Cell chemical composition reported by OS"
                )
            }
            telemetry.thermalStatus?.let { status ->
                TelemetryMetricCard(
                    label = "System Thermal Status",
                    value = status,
                    leadingIcon = Icons.Default.Thermostat,
                    subtext = "SoC / Battery thermal throttling state"
                )
            }
        }
    }
}

@Composable
private fun HeroQuickTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

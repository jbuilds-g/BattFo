package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.telemetry.BatteryCalculator
import com.example.model.PlugType
import com.example.ui.components.BatteryVisualIndicator
import com.example.ui.components.TelemetryMetricCard
import com.example.ui.viewmodel.BatteryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ChargingScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.currentTelemetry.collectAsStateWithLifecycle()
    val sessions by viewModel.chargingSessions.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("charging_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Active Charging Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_charging_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (telemetry.isCharging) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                border = if (telemetry.isCharging) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                } else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (telemetry.isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (telemetry.isCharging) "Charging" else "Disconnected",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (telemetry.isCharging && telemetry.plugType != PlugType.UNPLUGGED) {
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

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${telemetry.percentage}%",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 48.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        if (telemetry.isCharging && telemetry.powerWatts != null) {
                            Text(
                                text = String.format(Locale.US, "%.1f W", telemetry.powerWatts),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    if (telemetry.isCharging) {
                        val timeStr = BatteryCalculator.formatEstimatedDuration(telemetry.estimatedTimeRemainingSeconds)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
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
                                    text = "Until full: $timeStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Connect a charger to monitor live charging wattage, speed, and session telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live Charging Telemetry Tiles (shown when charging and supported)
        if (telemetry.isCharging) {
            item {
                Text(
                    text = "Live Charging Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (telemetry.currentNowMa != null) {
                item {
                    TelemetryMetricCard(
                        label = "Charging Current",
                        value = "${abs(telemetry.currentNowMa!!)} mA",
                        leadingIcon = Icons.Default.Bolt,
                        subtext = "Current delivered into cell"
                    )
                }
            }

            if (telemetry.voltageMv != null) {
                item {
                    TelemetryMetricCard(
                        label = "Charging Voltage",
                        value = "${telemetry.voltageMv} mV (${String.format(Locale.US, "%.2f V", telemetry.voltageMv!! / 1000f)})",
                        leadingIcon = Icons.Default.ElectricMeter,
                        subtext = "Cell terminal voltage"
                    )
                }
            }

            if (telemetry.powerWatts != null) {
                item {
                    TelemetryMetricCard(
                        label = "Calculated Wattage",
                        value = String.format(Locale.US, "%.2f W", telemetry.powerWatts),
                        leadingIcon = Icons.Default.FlashOn,
                        subtext = "Power into battery (excludes phone system load)",
                        isCalculated = true
                    )
                }
            }

            if (telemetry.temperatureC != null) {
                item {
                    TelemetryMetricCard(
                        label = "Battery Temperature",
                        value = viewModel.formatTemperature(telemetry.temperatureC),
                        leadingIcon = Icons.Default.Thermostat,
                        subtext = "Live thermistor temperature during charge"
                    )
                }
            }

            if (telemetry.drainRatePerHour != null) {
                item {
                    TelemetryMetricCard(
                        label = "Charging Speed",
                        value = String.format(Locale.US, "+%.1f %%/hr", abs(telemetry.drainRatePerHour!!)),
                        leadingIcon = Icons.Default.Speed,
                        subtext = "Charge rate per hour",
                        isCalculated = true
                    )
                }
            }
        }

        // Previous Charging Sessions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Charging Session History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "No charging sessions recorded yet. Plug in your charger to start recording charging telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                ChargingSessionCard(session = session, formatTemp = { viewModel.formatTemperature(it) })
            }
        }
    }
}

@Composable
private fun ChargingSessionCard(
    session: ChargingSessionEntity,
    formatTemp: (Float) -> String
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val durationMin = remember(session.startTime, session.endTime) {
        val end = session.endTime ?: System.currentTimeMillis()
        maxOf(1L, (end - session.startTime) / 60_000L)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormatter.format(Date(session.startTime)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (session.isCompleted) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    }
                ) {
                    Text(
                        text = if (session.isCompleted) "Completed (${durationMin}m)" else "Active Session",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (session.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Progress row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Level: ${session.startPercentage}% → ${session.endPercentage}% (+${maxOf(0, session.endPercentage - session.startPercentage)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Source: ${session.plugType.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (session.peakWattage > 0.0) {
                    Text(
                        text = "Peak: ${String.format(Locale.US, "%.1f W", session.peakWattage)} (${session.peakCurrentMa} mA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (session.maxTemperatureC > 0f) {
                    Text(
                        text = "Max: ${formatTemp(session.maxTemperatureC)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

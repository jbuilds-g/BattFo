package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TemperatureUnit
import com.example.ui.components.ChartDataPoint
import com.example.ui.components.HistoryCanvasChart
import com.example.ui.viewmodel.BatteryViewModel
import com.example.ui.viewmodel.HistoryMetric
import com.example.ui.viewmodel.HistoryRange
import java.util.Locale
import kotlin.math.abs

@Composable
fun HistoryScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val snapshots by viewModel.historySnapshots.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val selectedMetric by viewModel.selectedMetric.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // Map snapshots to chart data points based on selected metric
    val chartDataPoints = remember(snapshots, selectedMetric, settings.temperatureUnit) {
        snapshots.mapNotNull { snap ->
            val value = when (selectedMetric) {
                HistoryMetric.PERCENTAGE -> snap.percentage.toFloat()
                HistoryMetric.TEMPERATURE -> {
                    snap.temperatureC?.let { c ->
                        if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                            (c * 9f / 5f) + 32f
                        } else {
                            c
                        }
                    }
                }
                HistoryMetric.VOLTAGE -> snap.voltageMv?.toFloat()
                HistoryMetric.POWER -> snap.powerWatts?.toFloat()
            }
            if (value != null) ChartDataPoint(timestamp = snap.timestamp, value = value) else null
        }
    }

    val unit = when (selectedMetric) {
        HistoryMetric.PERCENTAGE -> "%"
        HistoryMetric.TEMPERATURE -> if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) "°F" else "°C"
        HistoryMetric.VOLTAGE -> "mV"
        HistoryMetric.POWER -> "W"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("history_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Range Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryRange.entries.forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { viewModel.setHistoryRange(range) },
                    label = { Text(range.label) },
                    leadingIcon = if (selectedRange == range) {
                        {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }

        // Metric Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryMetric.entries.forEach { metric ->
                FilterChip(
                    selected = selectedMetric == metric,
                    onClick = { viewModel.setHistoryMetric(metric) },
                    label = { Text(metric.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_chart_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                HistoryCanvasChart(
                    dataPoints = chartDataPoints,
                    metricLabel = selectedMetric.label,
                    unit = unit,
                    lineColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Range Summary Statistics
        if (chartDataPoints.isNotEmpty()) {
            val values = chartDataPoints.map { it.value }
            val minVal = values.minOrNull() ?: 0f
            val maxVal = values.maxOrNull() ?: 0f
            val avgVal = values.average().toFloat()
            val netDelta = values.last() - values.first()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Summary Statistics (${selectedRange.label})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Samples",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${chartDataPoints.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column {
                            Text(
                                text = "Minimum",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", minVal)} $unit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column {
                            Text(
                                text = "Average",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", avgVal)} $unit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column {
                            Text(
                                text = "Maximum",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", maxVal)} $unit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (selectedMetric == HistoryMetric.PERCENTAGE) {
                        val netColor = if (netDelta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        Text(
                            text = "Net change: ${if (netDelta >= 0) "+" else ""}${String.format(Locale.US, "%.1f", netDelta)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = netColor
                        )
                    }
                }
            }
        }
    }
}

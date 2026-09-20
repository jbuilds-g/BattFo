package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class ChartDataPoint(
    val timestamp: Long,
    val value: Float
)

@Composable
fun HistoryCanvasChart(
    dataPoints: List<ChartDataPoint>,
    metricLabel: String,
    unit: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (dataPoints.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No history recorded yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Data will appear as telemetry snapshots are stored.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    var inspectedPoint by remember { mutableStateOf<ChartDataPoint?>(null) }
    var cursorX by remember { mutableStateOf<Float?>(null) }

    val minTimestamp = dataPoints.first().timestamp
    val maxTimestamp = dataPoints.last().timestamp
    val timeSpan = max(1L, maxTimestamp - minTimestamp)

    val values = dataPoints.map { it.value }
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 100f
    val valueRange = if (maxValue == minValue) 1f else (maxValue - minValue)

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textStyle = MaterialTheme.typography.labelSmall
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fullTimeFormatter = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Inspection tooltip header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inspectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${fullTimeFormatter.format(Date(inspectedPoint!!.timestamp))}: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", inspectedPoint!!.value)} $unit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Touch/drag chart to inspect specific points",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Text(
                text = "$metricLabel ($unit)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Chart Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(dataPoints) {
                    detectTapGestures(
                        onPress = { offset ->
                            cursorX = offset.x
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetTime = minTimestamp + (timeSpan * fraction).toLong()
                            inspectedPoint = dataPoints.minByOrNull { kotlin.math.abs(it.timestamp - targetTime) }
                        }
                    )
                }
                .pointerInput(dataPoints) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            cursorX = offset.x
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetTime = minTimestamp + (timeSpan * fraction).toLong()
                            inspectedPoint = dataPoints.minByOrNull { kotlin.math.abs(it.timestamp - targetTime) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            cursorX = change.position.x
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val targetTime = minTimestamp + (timeSpan * fraction).toLong()
                            inspectedPoint = dataPoints.minByOrNull { kotlin.math.abs(it.timestamp - targetTime) }
                        },
                        onDragEnd = {
                            cursorX = null
                            inspectedPoint = null
                        },
                        onDragCancel = {
                            cursorX = null
                            inspectedPoint = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height - 20.dp.toPx()
                val bottomOffset = 18.dp.toPx()

                // Draw 3 horizontal gridlines
                for (i in 0..2) {
                    val y = chartHeight * (i / 2f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // If only 1 point, draw a simple dot
                if (dataPoints.size == 1) {
                    drawCircle(
                        color = lineColor,
                        radius = 6.dp.toPx(),
                        center = Offset(chartWidth / 2f, chartHeight / 2f)
                    )
                    return@Canvas
                }

                // Generate path
                val path = Path()
                val fillPath = Path()

                dataPoints.forEachIndexed { index, pt ->
                    val x = if (timeSpan > 0) {
                        ((pt.timestamp - minTimestamp).toFloat() / timeSpan.toFloat()) * chartWidth
                    } else {
                        (index.toFloat() / (dataPoints.size - 1)) * chartWidth
                    }
                    val y = chartHeight - (((pt.value - minValue) / valueRange) * chartHeight)

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                // Fill area below curve
                val lastX = chartWidth
                fillPath.lineTo(lastX, chartHeight)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    color = lineColor.copy(alpha = 0.12f)
                )

                // Draw line stroke
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw vertical inspection cursor if scrubbing
                cursorX?.let { cx ->
                    val clampedX = cx.coerceIn(0f, chartWidth)
                    drawLine(
                        color = lineColor.copy(alpha = 0.8f),
                        start = Offset(clampedX, 0f),
                        end = Offset(clampedX, chartHeight),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Draw dot at inspected value
                    inspectedPoint?.let { pt ->
                        val py = chartHeight - (((pt.value - minValue) / valueRange) * chartHeight)
                        drawCircle(
                            color = surfaceColor,
                            radius = 5.dp.toPx(),
                            center = Offset(clampedX, py)
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 3.dp.toPx(),
                            center = Offset(clampedX, py)
                        )
                    }
                }
            }
        }

        // Time axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeFormatter.format(Date(minTimestamp)),
                style = textStyle,
                color = textColor
            )
            Text(
                text = timeFormatter.format(Date((minTimestamp + maxTimestamp) / 2)),
                style = textStyle,
                color = textColor
            )
            Text(
                text = timeFormatter.format(Date(maxTimestamp)),
                style = textStyle,
                color = textColor
            )
        }
    }
}

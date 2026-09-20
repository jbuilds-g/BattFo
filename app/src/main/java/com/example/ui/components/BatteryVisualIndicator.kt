package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BatteryVisualIndicator(
    percentage: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = (percentage.coerceIn(0, 100)) / 100f,
        label = "battery_fill_level"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outline
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val fillColor = when {
        isCharging -> primaryColor
        percentage <= 15 -> errorColor
        percentage <= 25 -> tertiaryColor
        else -> primaryColor
    }

    Box(
        modifier = modifier
            .width(72.dp)
            .height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            val terminalWidth = 4.dp.toPx()
            val terminalHeight = 14.dp.toPx()

            val bodyWidth = size.width - terminalWidth - 3.dp.toPx()
            val bodyHeight = size.height

            // Draw outer battery shell
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(0f, 0f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeWidth)
            )

            // Draw positive terminal nub on right
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(bodyWidth + 1.dp.toPx(), (bodyHeight - terminalHeight) / 2f),
                size = Size(terminalWidth, terminalHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Draw inner fill
            val innerPadding = strokeWidth + 2.dp.toPx()
            val maxFillWidth = bodyWidth - (innerPadding * 2)
            val fillWidth = maxFillWidth * animatedPercent
            val fillHeight = bodyHeight - (innerPadding * 2)

            if (fillWidth > 0) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(innerPadding, innerPadding),
                    size = Size(fillWidth, fillHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        if (isCharging) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Charging",
                tint = onPrimaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

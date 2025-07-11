package com.example.touchkeyboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

@Composable
fun WeeklyUsageChart(
    weeklyStats: Map<Int, Long>,
    modifier: Modifier = Modifier
) {
    // Calculate the maximum hours for scaling
    val maxHours = weeklyStats.values.maxOfOrNull { it.toFloat() / (1000 * 60 * 60) } ?: 1f

    // Round up to the nearest appropriate increment to create a clean y-axis
    val roundedMax = when {
        maxHours < 1f -> 1 // Minimum of 1 hour for very small usage
        maxHours <= 5f -> ceil(maxHours).toInt()
        maxHours <= 10f -> ((ceil(maxHours) / 2).toInt() * 2) // Round to nearest even number
        else -> ((ceil(maxHours) / 5).toInt() * 5) // Round to nearest multiple of 5
    }

    // Determine the step size for y-axis labels based on the max value
    val yAxisStep = when {
        roundedMax > 15 -> 5
        roundedMax > 8 -> 2
        else -> 1
    }

    // Create the list of y-axis labels (0h, 1h, 2h, etc.)
    val yLabels = (0..roundedMax step yAxisStep).toList()

    // Define day labels
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Get theme colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    // Chart dimensions
    val chartHeight = 180.dp
    val yAxisWidth = 40.dp
    val bottomLabelHeight = 24.dp
    val topPadding = 16.dp
    val bottomPadding = 8.dp

    Column(modifier = modifier.fillMaxWidth()) {
        // Main chart area with Y-axis and bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight + topPadding + bottomPadding)
        ) {
            // Y-Axis labels
            Column(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
                    .padding(top = topPadding, bottom = bottomPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Labels from top to bottom (max hours to 0h)
                yLabels.reversed().forEach { hour ->
                    Text(
                        text = "${hour}h",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    )
                }
            }

            // Chart area with grid lines and bars
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding, bottom = bottomPadding)
                ) {
                    val chartAreaHeight = size.height
                    val chartAreaWidth = size.width
                    val barSpacing = chartAreaWidth / daysOfWeek.size
                    val barWidth = barSpacing * 0.6f

                    // Draw horizontal grid lines
                    yLabels.forEach { hour ->
                        // Calculate Y position: 0h at bottom, max at top
                        val normalizedValue = hour.toFloat() / roundedMax
                        val yPosition = chartAreaHeight - (normalizedValue * chartAreaHeight)

                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, yPosition),
                            end = Offset(chartAreaWidth, yPosition),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw bars
                    weeklyStats.forEach { (dayIndex, usage) ->
                        val usageHours = usage.toFloat() / (1000 * 60 * 60)
                        val normalizedHeight = (usageHours / roundedMax).coerceIn(0f, 1f)
                        val barHeight = chartAreaHeight * normalizedHeight

                        val xCenter = dayIndex * barSpacing + (barSpacing / 2)
                        val yBottom = chartAreaHeight // 0h line (bottom)
                        val yTop = chartAreaHeight - barHeight // Bar top

                        drawLine(
                            color = primaryColor,
                            start = Offset(xCenter, yBottom),
                            end = Offset(xCenter, yTop),
                            strokeWidth = barWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // X-axis day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomLabelHeight)
                .padding(
                    start = yAxisWidth,
                    top = 10.dp
                ),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun UsageProgressBar(
    currentUsage: Long,
    dailyLimit: Long,
    modifier: Modifier = Modifier
) {
    val progress = (currentUsage.toFloat() / dailyLimit).coerceIn(0f, 1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentUsage),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "/ ${formatDuration(dailyLimit)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background track
                drawLine(
                    color = surfaceVariantColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round
                )

                // Progress indicator
                if (progress > 0f) {
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width * progress, size.height / 2),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return "${hours}h ${minutes}m"
}

@Preview(showBackground = true)
@Composable
fun WeeklyUsageChartPreview() {
    // Sample data for preview - simulates a week of usage
    val sampleData = mapOf(
        0 to TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(30), // Sunday - 3.5h
        1 to TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(15), // Monday - 1.25h
        2 to TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(45), // Tuesday - 2.75h
        3 to TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(10), // Wednesday - 4.17h
        4 to TimeUnit.HOURS.toMillis(0) + TimeUnit.MINUTES.toMillis(45), // Thursday - 0.75h
        5 to TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(20), // Friday - 3.33h
        6 to TimeUnit.HOURS.toMillis(5) + TimeUnit.MINUTES.toMillis(0)   // Saturday - 5h
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Weekly App Usage",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            WeeklyUsageChart(weeklyStats = sampleData)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Daily Progress",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            UsageProgressBar(
                currentUsage = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30),
                dailyLimit = TimeUnit.HOURS.toMillis(4)
            )
        }
    }
}
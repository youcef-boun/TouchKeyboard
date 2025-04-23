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
import androidx.compose.ui.unit.times
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

    // Chart constants
    val chartHeight = 180.dp
    val yAxisWidth = 40.dp
    val bottomLabelHeight = 24.dp
    val chartTopPadding = 16.dp
    val chartBottomPadding = 8.dp

    Row(modifier = modifier.fillMaxWidth()) {
        // Y-Axis labels column
        Box(
            modifier = Modifier
                .width(yAxisWidth)
                .height(chartHeight + bottomLabelHeight + chartTopPadding + chartBottomPadding)
        ) {
            // Place each y-axis label at its exact position
            yLabels.forEach { hour ->
                // Position from top, aligned with grid lines
                val position = chartTopPadding + (1 - hour.toFloat() / roundedMax) * chartHeight

                Text(
                    text = "${hour}h",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = position)
                        .padding(end = 8.dp)
                        .width(32.dp)
                )
            }
        }

        // Chart area (bars, grid lines, and day labels)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(chartHeight + bottomLabelHeight + chartTopPadding + chartBottomPadding)
        ) {
            // Grid lines and bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .align(Alignment.TopCenter)
                    .padding(top = chartTopPadding)
            ) {
                val barSpacing = size.width / daysOfWeek.size
                val barWidth = barSpacing * 0.6f // 60% of available space per day

                // Draw horizontal grid lines with exact alignment
                yLabels.forEach { hour ->
                    val yPosition = size.height * (1 - hour.toFloat() / roundedMax)

                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, yPosition),
                        end = Offset(size.width, yPosition),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw bars starting from the baseline (0h)
                weeklyStats.forEach { (day, usage) ->
                    val usageHours = usage.toFloat() / (1000 * 60 * 60)

                    // Calculate height as percentage of chart height (for proper scaling)
                    val heightPercentage = (usageHours / roundedMax).coerceIn(0f, 1f)
                    val barHeight = size.height * heightPercentage

                    // Position bar in the center of its allocated space
                    val xCenter = day * barSpacing + (barSpacing / 2)

                    // Draw from baseline (bottom) upward
                    val yStart = size.height // Start at the bottom (0h line)
                    val yEnd = size.height - barHeight // Go up by the calculated height

                    drawLine(
                        color = primaryColor,
                        start = Offset(x = xCenter, y = yStart),
                        end = Offset(x = xCenter, y = yEnd),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // X-axis day labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomLabelHeight)
                    .align(Alignment.BottomStart)
                    .padding(top = chartBottomPadding),
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background track
                drawLine(
                    color = surfaceVariantColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height
                )

                // Progress indicator
                drawLine(
                    color = primaryColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width * progress, size.height / 2),
                    strokeWidth = size.height
                )
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
        0 to TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(30), // Sunday
        1 to TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(15), // Monday
        2 to TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(45), // Tuesday
        3 to TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(10), // Wednesday
        4 to TimeUnit.HOURS.toMillis(0) + TimeUnit.MINUTES.toMillis(45), // Thursday
        5 to TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(20), // Friday
        6 to TimeUnit.HOURS.toMillis(5) + TimeUnit.MINUTES.toMillis(0)   // Saturday
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
        }
    }
}
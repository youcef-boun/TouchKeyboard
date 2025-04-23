package com.example.touchkeyboard.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val category: String = "Uncategorized"
)

data class HourlyUsage(
    val hour: Int,
    val usageTimeInMillis: Long
)

class UsageStatsCollector(private val context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    // Only exclude essential system UI elements
    private val excludedPackages = setOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3"
    )

    private fun shouldExcludeApp(packageName: String): Boolean {
        return try {
            // Only exclude system UI and launchers
            packageName in excludedPackages
        } catch (e: Exception) {
            false // If we can't determine, include it
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName // Fallback to package name if app name can't be found
        }
    }

    fun getDailyUsageStats(dayOffset: Int = 0): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = calendar.timeInMillis

        return calculateAppUsage(startTime, endTime)
    }

    private fun calculateAppUsage(startTime: Long, endTime: Long): List<AppUsageInfo> {
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val appUsageMap = mutableMapOf<String, Long>()
        val lastUsedMap = mutableMapOf<String, Long>()
        val appNameCache = mutableMapOf<String, String>()

        var currentEvent: UsageEvents.Event? = null
        val eventMap = mutableMapOf<String, UsageEvents.Event?>()

        while (events.hasNextEvent()) {
            currentEvent = UsageEvents.Event().also { events.getNextEvent(it) }

            // Skip excluded system UI apps
            if (shouldExcludeApp(currentEvent.packageName)) continue

            // Cache app name
            if (!appNameCache.containsKey(currentEvent.packageName)) {
                appNameCache[currentEvent.packageName] = getAppName(currentEvent.packageName)
            }

            when (currentEvent.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    eventMap[currentEvent.packageName] = currentEvent
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val foregroundEvent = eventMap[currentEvent.packageName]
                    if (foregroundEvent != null) {
                        val usageTime = currentEvent.timeStamp - foregroundEvent.timeStamp
                        if (usageTime > 0) {  // Only count positive durations
                            appUsageMap[currentEvent.packageName] =
                                appUsageMap.getOrDefault(currentEvent.packageName, 0L) + usageTime
                            lastUsedMap[currentEvent.packageName] = currentEvent.timeStamp
                        }
                        eventMap.remove(currentEvent.packageName)
                    }
                }
            }
        }

        // Handle any apps still in foreground at query time
        val queryEndTime = System.currentTimeMillis().coerceAtMost(endTime)
        eventMap.forEach { (packageName, foregroundEvent) ->
            if (foregroundEvent != null && !shouldExcludeApp(packageName)) {
                val usageTime = queryEndTime - foregroundEvent.timeStamp
                if (usageTime > 0) {  // Only count positive durations
                    appUsageMap[packageName] = appUsageMap.getOrDefault(packageName, 0L) + usageTime
                    lastUsedMap[packageName] = queryEndTime
                    if (!appNameCache.containsKey(packageName)) {
                        appNameCache[packageName] = getAppName(packageName)
                    }
                }
            }
        }

        return appUsageMap.map { (packageName, timeInForeground) ->
            AppUsageInfo(
                packageName = packageName,
                appName = appNameCache[packageName] ?: packageName,
                totalTimeInForeground = timeInForeground,
                lastTimeUsed = lastUsedMap[packageName] ?: 0L
            )
        }.sortedByDescending { it.totalTimeInForeground }
    }

    fun getHourlyBreakdown(dayOffset: Int = 0): List<HourlyUsage> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val hourlyUsage = MutableList(24) { HourlyUsage(it, 0L) }
        val eventMap = mutableMapOf<String, UsageEvents.Event?>()

        var currentEvent: UsageEvents.Event? = null

        while (events.hasNextEvent()) {
            currentEvent = UsageEvents.Event().also { events.getNextEvent(it) }

            // Skip excluded system UI apps
            if (shouldExcludeApp(currentEvent.packageName)) continue

            when (currentEvent.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    eventMap[currentEvent.packageName] = currentEvent
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val foregroundEvent = eventMap[currentEvent.packageName]
                    if (foregroundEvent != null) {
                        val usageTime = currentEvent.timeStamp - foregroundEvent.timeStamp
                        if (usageTime > 0) {
                            processTimeRange(
                                foregroundEvent.timeStamp,
                                currentEvent.timeStamp,
                                hourlyUsage
                            )
                        }
                        eventMap.remove(currentEvent.packageName)
                    }
                }
            }
        }

        // Handle apps still in foreground
        val queryEndTime = System.currentTimeMillis().coerceAtMost(endTime)
        eventMap.forEach { (packageName, foregroundEvent) ->
            if (foregroundEvent != null && !shouldExcludeApp(packageName)) {
                val usageTime = queryEndTime - foregroundEvent.timeStamp
                if (usageTime > 0) {
                    processTimeRange(
                        foregroundEvent.timeStamp,
                        queryEndTime,
                        hourlyUsage
                    )
                }
            }
        }

        return hourlyUsage
    }

    private fun processTimeRange(startTime: Long, endTime: Long, hourlyUsage: MutableList<HourlyUsage>) {
        val calendar = Calendar.getInstance()
        var currentTime = startTime

        while (currentTime < endTime) {
            calendar.timeInMillis = currentTime
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            // Calculate next hour boundary
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            val nextHourStart = calendar.timeInMillis

            val timeInThisHour = minOf(endTime, nextHourStart) - currentTime
            hourlyUsage[currentHour] = hourlyUsage[currentHour].copy(
                usageTimeInMillis = hourlyUsage[currentHour].usageTimeInMillis + timeInThisHour
            )

            currentTime = nextHourStart
        }
    }

    fun getWeeklyStats(): Map<Int, Long> {
        val calendar = Calendar.getInstance()
        val stats = mutableMapOf<Int, Long>()

        // Start from the beginning of current week (Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val weekStart = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val weekEnd = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(weekStart, weekEnd)
        val dailyUsage = mutableMapOf<Int, Long>()
        val eventMap = mutableMapOf<String, UsageEvents.Event?>()

        var currentEvent: UsageEvents.Event? = null

        while (events.hasNextEvent()) {
            currentEvent = UsageEvents.Event().also { events.getNextEvent(it) }

            // Skip excluded system UI apps
            if (shouldExcludeApp(currentEvent.packageName)) continue

            when (currentEvent.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    eventMap[currentEvent.packageName] = currentEvent
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val foregroundEvent = eventMap[currentEvent.packageName]
                    if (foregroundEvent != null) {
                        calendar.timeInMillis = foregroundEvent.timeStamp
                        val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek + 7) % 7
                        val usageTime = currentEvent.timeStamp - foregroundEvent.timeStamp
                        dailyUsage[dayOfWeek] = dailyUsage.getOrDefault(dayOfWeek, 0L) + usageTime
                        eventMap.remove(currentEvent.packageName)
                    }
                }
            }
        }

        // Handle apps still in foreground
        val queryEndTime = System.currentTimeMillis().coerceAtMost(weekEnd)
        eventMap.forEach { (packageName, foregroundEvent) ->
            if (foregroundEvent != null && !shouldExcludeApp(packageName)) {
                calendar.timeInMillis = foregroundEvent.timeStamp
                val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek + 7) % 7
                val usageTime = queryEndTime - foregroundEvent.timeStamp
                dailyUsage[dayOfWeek] = dailyUsage.getOrDefault(dayOfWeek, 0L) + usageTime
            }
        }

        return dailyUsage
    }

    fun getTotalScreenTime(startTime: Long, endTime: Long): Long {
        val events = usageStatsManager.queryEvents(startTime, endTime)
        var totalTime = 0L
        val eventMap = mutableMapOf<String, UsageEvents.Event?>()

        var currentEvent: UsageEvents.Event? = null

        while (events.hasNextEvent()) {
            currentEvent = UsageEvents.Event().also { events.getNextEvent(it) }

            // Skip excluded system UI apps
            if (shouldExcludeApp(currentEvent.packageName)) continue

            when (currentEvent.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    eventMap[currentEvent.packageName] = currentEvent
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val foregroundEvent = eventMap[currentEvent.packageName]
                    if (foregroundEvent != null) {
                        totalTime += currentEvent.timeStamp - foregroundEvent.timeStamp
                        eventMap.remove(currentEvent.packageName)
                    }
                }
            }
        }

        // Handle apps still in foreground
        val queryEndTime = System.currentTimeMillis().coerceAtMost(endTime)
        eventMap.forEach { (packageName, foregroundEvent) ->
            if (foregroundEvent != null && !shouldExcludeApp(packageName)) {
                totalTime += queryEndTime - foregroundEvent.timeStamp
            }
        }

        return totalTime
    }
}
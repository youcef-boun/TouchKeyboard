package com.example.touchkeyboard.services

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.touchkeyboard.MainActivity
import com.example.touchkeyboard.R
import com.touchkeyboard.domain.repositories.IScreenTimeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.Duration
import javax.inject.Inject

@AndroidEntryPoint
class AppUsageTrackingService : Service() {
    @Inject
    lateinit var screenTimeRepository: IScreenTimeRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var trackingJob: Job? = null
    private val appUsageMap = mutableMapOf<String, UsageEventInfo>()
    private var lastEventTime: Long = System.currentTimeMillis() - 1000 * 60 // Start from 1 minute ago
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "screen_time_tracking"
    private var isForeground = false

    private data class UsageEventInfo(
        val packageName: String,
        val appName: String,
        val eventType: Int,
        val timeStamp: Long
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_FOREGROUND" -> startForegroundService()
            "STOP_FOREGROUND" -> stopForegroundService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        if (!isForeground) {
            startForeground(NOTIFICATION_ID, createNotification())
            isForeground = true
        }
    }

    private fun stopForegroundService() {
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Screen Time Tracking"
            val descriptionText = "Tracks app usage time"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time Tracking")
            .setContentText("Monitoring app usage")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startTracking() {
        trackingJob = serviceScope.launch {
            try {
                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val packageManager = packageManager

                while (isActive) {
                    try {
                        val now = System.currentTimeMillis()
                        val events = usageStatsManager.queryEvents(lastEventTime, now)
                        processEvents(events, packageManager)
                        lastEventTime = now
                        delay(10000) // Check every 10 seconds instead of 1 minute
                    } catch (e: Exception) {
                        Log.e("AppUsageTracking", "Error tracking app usage", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppUsageTracking", "Fatal error in tracking service", e)
            }
        }
    }

    private suspend fun processEvents(events: UsageEvents, packageManager: PackageManager) {
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            try {
                events.getNextEvent(event)
                val packageName = event.packageName

                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            // Store the RESUME event
                            appUsageMap[packageName] = UsageEventInfo(
                                packageName = packageName,
                                appName = appName,
                                eventType = event.eventType,
                                timeStamp = event.timeStamp
                            )
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED -> {
                            // When we get a PAUSE event, look for the matching RESUME event
                            appUsageMap[packageName]?.let { resumeEvent ->
                                if (resumeEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                                    val duration = Duration.ofMillis(event.timeStamp - resumeEvent.timeStamp)
                                    if (duration.seconds > 0) {
                                        Log.d("AppUsageTracking", "Recording usage for $appName: ${duration.seconds} seconds")
                                        screenTimeRepository.recordAppUsage(packageName, appName, duration)
                                    }
                                }
                            }
                            // Update the last event to PAUSED
                            appUsageMap[packageName] = UsageEventInfo(
                                packageName = packageName,
                                appName = appName,
                                eventType = event.eventType,
                                timeStamp = event.timeStamp
                            )
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e("AppUsageTracking", "Package not found: $packageName")
                }
            } catch (e: Exception) {
                Log.e("AppUsageTracking", "Error processing event", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()
        trackingJob?.cancel()
        serviceScope.cancel()
    }
}

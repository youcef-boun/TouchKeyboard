package com.example.touchkeyboard.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.touchkeyboard.R
import com.touchkeyboard.data.models.BlockedApp
import com.example.touchkeyboard.utils.OverlayPermissionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import com.touchkeyboard.data.local.AppDatabase
import com.touchkeyboard.data.local.dao.BlockedAppsDao
import kotlinx.coroutines.flow.firstOrNull

private const val PACKAGE_SERVICE = "package"

class AppBlockingService : Service() {
    private val TAG = "AppBlockingService"
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val checkInterval = 1000L // Check every second
    private var isRunning = false
    private var blockedApps: List<BlockedApp> = emptyList()
    private val activityManager by lazy { getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    private lateinit var blockedAppsDao: BlockedAppsDao

    inner class LocalBinder : Binder() {
        fun getService(): AppBlockingService = this@AppBlockingService
    }

    companion object {
        private const val PACKAGE_SERVICE = "package"

        private const val ACTION_START = "com.example.touchkeyboard.action.START"
        private const val ACTION_STOP = "com.example.touchkeyboard.action.STOP"
        private const val ACTION_RELOAD = "com.example.touchkeyboard.action.RELOAD"
        private const val NOTIFICATION_ID = 1

        fun startService(context: Context) {
            val intent = Intent(context, AppBlockingService::class.java).apply {
                action = ACTION_START
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AppBlockingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun reloadService(context: Context) {
            val intent = Intent(context, AppBlockingService::class.java).apply {
                action = ACTION_RELOAD
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        blockedAppsDao = AppDatabase.getInstance(applicationContext).blockedAppsDao()
        startForegroundService()
        loadBlockedAppsFromDb()
    }

    private fun startForegroundService() {
        Log.d(TAG, "Starting foreground service")
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val channelId = "app_blocking_channel"
        val channelName = "App Blocking Service"
        val channelDescription = "Service that blocks distracting apps"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = channelDescription
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("App Blocking Service")
            .setContentText("Monitoring blocked apps")
            .setSmallIcon(R.drawable.ic_block)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun loadBlockedAppsFromDb() {
        Log.d(TAG, "loadBlockedAppsFromDb called")
        scope.launch {
            blockedApps = blockedAppsDao.getAllBlockedApps().firstOrNull() ?: emptyList()
            Log.d(TAG, "Blocked apps loaded from DB: $blockedApps")
            startMonitoring()
        }
    }

    fun reloadBlockedApps() {
        scope.launch {
            loadBlockedAppsFromDb()
        }
    }

    private fun startMonitoring() {
        if (isRunning) return

        if (!OverlayPermissionManager.hasOverlayPermission(this)) {
            Log.e(TAG, "Missing overlay permission, requesting...")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .setData(Uri.parse("package:${packageName}"))
            startActivity(intent)
            return
        }

        isRunning = true
        scope.launch {
            while (isActive) {
                checkBlockedApps()
                delay(checkInterval)
            }
        }
    }

    private suspend fun checkBlockedApps() {
        Log.d(TAG, "checkBlockedApps called, blockedApps: $blockedApps")
        // Always refresh from DB before checking
        blockedApps = blockedAppsDao.getAllBlockedApps().firstOrNull() ?: emptyList()
        val runningTasks = activityManager.getRunningTasks(1)
        if (runningTasks.isEmpty()) return

        val topActivity = runningTasks[0].topActivity
        val topPackageName = topActivity?.packageName

        if (topPackageName != null) {
            val blockedApp = blockedApps.find { it.packageName == topPackageName }
            if (blockedApp != null) {
                val now = System.currentTimeMillis()
                val shouldBlock = when {
                    // App is marked as currently blocked
                    blockedApp.isCurrentlyBlocked -> true
                    // App is temporarily unblocked but the period has expired
                    !blockedApp.isCurrentlyBlocked && blockedApp.blockEndTime > 0 && now >= blockedApp.blockEndTime -> true
                    // App is blocked indefinitely
                    blockedApp.isBlockedIndefinitely && blockedApp.blockEndTime == 0L -> true
                    else -> false
                }

                if (shouldBlock) {
                    Log.d(TAG, "Found blocked app in foreground: ${blockedApp.packageName}")
                    blockApp(topPackageName)
                }
            }
        }
    }

    private fun blockApp(packageName: String) {
        scope.launch {
            try {
                // First try to kill the app
                activityManager.killBackgroundProcesses(packageName)

                // Try to force stop the app
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        val method = activityManager.javaClass.getMethod("forceStopPackage", String::class.java)
                        method.invoke(activityManager, packageName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error using forceStopPackage", e)
                    }
                }

                // Try to remove from recents
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        val method = activityManager.javaClass.getMethod("removeTask", Int::class.java)
                        method.invoke(activityManager, 0)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing task", e)
                    }
                }

                // Update the app's block status in DB
                val app = blockedAppsDao.getAllBlockedApps().firstOrNull()?.find { it.packageName == packageName }
                if (app != null) {
                    val now = System.currentTimeMillis()
                    val shouldBeBlocked = when {
                        app.isCurrentlyBlocked -> true
                        !app.isCurrentlyBlocked && app.blockEndTime > 0 && now >= app.blockEndTime -> true
                        app.isBlockedIndefinitely && app.blockEndTime == 0L -> true
                        else -> false
                    }
                    if (shouldBeBlocked) {
                        val updatedApp = app.copy(
                            isCurrentlyBlocked = true,
                            blockStartTime = now,
                            blockEndTime = 0L,
                            isBlockedIndefinitely = true
                        )
                        blockedAppsDao.insertBlockedApp(updatedApp)
                    }
                }

                Log.d(TAG, "Successfully blocked app: $packageName")
                redirectToHome()
            } catch (e: Exception) {
                Log.e(TAG, "Error blocking app: $packageName", e)
                // Retry after a short delay
                delay(1000)
                blockApp(packageName)
            }
        }
    }

    private fun redirectToHome() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "ACTION_START received")
                startForegroundService()
                loadBlockedAppsFromDb()
            }
            ACTION_STOP -> {
                Log.d(TAG, "ACTION_STOP received")
                stopSelf()
            }
            ACTION_RELOAD -> {
                Log.d(TAG, "ACTION_RELOAD received, reloading blocked apps")
                loadBlockedAppsFromDb()
            }
            else -> {
                Log.d(TAG, "Unknown action received")
            }
        }
        return START_STICKY
    }

    private fun stopMonitoring() {
        isRunning = false
        scope.cancel()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        scope.cancel()
    }


}

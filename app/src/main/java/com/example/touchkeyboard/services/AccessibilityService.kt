package com.example.touchkeyboard.services


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo

import android.view.accessibility.AccessibilityEvent
import com.example.touchkeyboard.domain.repositories.IBlockListRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.touchkeyboard.utils.PermissionManager
import android.util.Log
import com.example.touchkeyboard.MainActivity
import android.content.Context
import android.provider.Settings



@AndroidEntryPoint
class TouchKeyboardAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var blockListRepository: IBlockListRepository

    @Inject
    lateinit var permissionManager: PermissionManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val TAG = "TouchKeyboardService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        Log.d(TAG, "Repo initialized: ${::blockListRepository.isInitialized}")
        Log.d(TAG, "Repo instance: $blockListRepository")

        // Debug: Log all blocked apps on service start
        serviceScope.launch {
            try {
                val blockedApps = blockListRepository.getBlockedApps()
                Log.d(TAG, "Blocked apps at startup: $blockedApps")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting blocked apps", e)
            }
        }
        createNotificationChannel()
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        this.serviceInfo = info
        Log.d(TAG, "Service configured with flags: ${info.flags}")

        // Check if accessibility service is enabled
        val isEnabled = isAccessibilityServiceEnabled(this, TouchKeyboardAccessibilityService::class.java)
        Log.d(TAG, "Accessibility service enabled: $isEnabled")

        if (!isEnabled) {
            Log.e(TAG, "Accessibility service is not enabled")
            // You might want to show a notification or dialog to the user
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${serviceClass.name}"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServicesSetting.contains(expectedComponentName)
    }

    private suspend fun debugBlockListCheck(pkg: String): Boolean {
        val isBlocked = blockListRepository.isAppBlocked(pkg)
        Log.d(TAG, """
            Package: $pkg
            Blocked: $isBlocked
            All blocked apps: ${blockListRepository.getBlockedApps()}
            Block count: ${blockListRepository.getBlockedApps().size}
            Is Instagram blocked: ${blockListRepository.isAppBlocked("com.instagram.lite")}
        """.trimIndent())
        return isBlocked
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.packageName?.toString()?.let { packageName ->
                    // Skip system UI and launcher
                    if (packageName == "com.android.systemui" ||
                        packageName == "com.google.android.apps.nexuslauncher" ||
                        packageName == applicationContext.packageName) {
                        return
                    }

                    Log.d(TAG, "Checking block status for: $packageName")
                    serviceScope.launch {
                        val isBlocked = debugBlockListCheck(packageName)
                        if (isBlocked) {
                            Log.d(TAG, "App $packageName is blocked - redirecting to verification screen")

                            // Create intent to launch MainActivity in verification mode
                            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                                putExtra("verification_mode", true)
                                putExtra("blocked_package", packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            }

                            try {
                                // Use applicationContext to start the activity
                                applicationContext.startActivity(intent)
                                Log.d(TAG, "Successfully started MainActivity in verification mode")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to start MainActivity", e)
                            }
                        }
                    }
                }
            }
            else -> Log.d(TAG, "Received event type: ${event.eventType}")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "block_service_channel",
                "App Block Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}
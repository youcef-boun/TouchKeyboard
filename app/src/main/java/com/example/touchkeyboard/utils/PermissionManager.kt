package com.example.touchkeyboard.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton
import android.accessibilityservice.AccessibilityServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityManager


@Singleton
class PermissionManager @Inject constructor(
    private val context: Context
) {
    private val TAG = "PermissionManager"

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasKillBackgroundProcessesPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.KILL_BACKGROUND_PROCESSES
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPackageQueryPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.QUERY_ALL_PACKAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // For Android versions below Tiramisu, this permission is not required
        }
    }

    fun hasAccessibilityPermission(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        // The canonical service class name we're looking for
        val targetServiceName = "com.example.touchkeyboard.services.TouchKeyboardAccessibilityService"

        enabledServices.forEach { service ->
            Log.d(TAG, "Found enabled service: ${service.id}")
        }

        val isEnabled = enabledServices.any { service ->
            service.id?.let { id ->
                // Check all possible formats
                id == "com.example.touchkeyboard/$targetServiceName" ||  // Full format
                        id == "com.example.touchkeyboard/.services.TouchKeyboardAccessibilityService" ||  // Short format
                        id.endsWith("TouchKeyboardAccessibilityService")  // Just the class name
            } ?: false
        }

        Log.d(TAG, "Accessibility service enabled: $isEnabled")
        return isEnabled
    }

    fun hasSystemAlertWindowPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasAllBlockingPermissions(): Boolean {
        return hasKillBackgroundProcessesPermission() &&
                hasAccessibilityPermission() &&
                hasSystemAlertWindowPermission() &&
                hasPackageQueryPermission()
    }

    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()

        if (!hasKillBackgroundProcessesPermission()) {
            missing.add("Kill Background Processes")
        }
        if (!hasAccessibilityPermission()) {
            missing.add("Accessibility Service")
        }
        if (!hasSystemAlertWindowPermission()) {
            missing.add("Draw Over Other Apps")
        }
        if (!hasPackageQueryPermission()) {
            missing.add("Query All Packages")
        }

        return missing
    }

    fun getPermissionErrorMessages(): List<String> {
        val messages = mutableListOf<String>()

        if (!hasKillBackgroundProcessesPermission()) {
            messages.add("Need permission to kill background processes")
        }
        if (!hasAccessibilityPermission()) {
            messages.add("Accessibility service needs to be enabled")
        }
        if (!hasSystemAlertWindowPermission()) {
            messages.add("Need permission to draw over other apps")
        }
        if (!hasPackageQueryPermission()) {
            messages.add("Need permission to query installed apps")
        }

        return messages
    }

    fun requestBlockingPermissions() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }

    fun getUsageStatsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    fun getRequiredPermissions(): List<String> {
        return listOf(
            android.Manifest.permission.KILL_BACKGROUND_PROCESSES,
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.QUERY_ALL_PACKAGES
        )
    }
}

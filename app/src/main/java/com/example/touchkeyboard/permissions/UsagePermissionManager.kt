package com.example.touchkeyboard.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
class UsagePermissionManager(private val context: Context) {

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsagePermission(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        launcher.launch(intent)
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 123
    }
}
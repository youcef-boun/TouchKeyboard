package com.example.touchkeyboard.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi


class OverlayPermissionManager {
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1234

        fun hasOverlayPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun requestOverlayPermission(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    .setData(Uri.parse("package:${activity.packageName}"))
                activity.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun handlePermissionResult(
            activity: Activity,
            requestCode: Int,
            resultCode: Int
        ): Boolean {
            return if (requestCode == REQUEST_OVERLAY_PERMISSION) {
                Settings.canDrawOverlays(activity)
            } else {
                false
            }
        }
    }
}
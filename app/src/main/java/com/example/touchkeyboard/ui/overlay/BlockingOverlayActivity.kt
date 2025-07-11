package com.example.touchkeyboard.ui.overlay


import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.touchkeyboard.R
import com.example.touchkeyboard.utils.OverlayPermissionManager
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat.getSystemService
import com.example.touchkeyboard.MainActivity








class BlockingOverlayActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 1000L // Check every second
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAndKillApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    // Guard to prevent multiple redirects
    private var hasRedirected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TK_BlockingOverlay", "onCreate: intent=$intent flags=${intent?.flags}")
        super.onCreate(savedInstanceState)
        Log.d("BlockingOverlay", "onCreate called")

        // Get the blocked app package name
        val appPackageName = intent.getStringExtra("blocked_app_package") ?: ""
        Log.d("TK_BlockingOverlay", "blocked_app_package=$appPackageName")
        if (appPackageName.isEmpty()) {
            Log.e("TK_BlockingOverlay", "No package name provided")
            redirectToHomeAndFinish()
            return
        }

        // Check if we have overlay permission
        if (!OverlayPermissionManager.hasOverlayPermission(this)) {
            Log.e("TK_BlockingOverlay", "Missing overlay permission")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .setData(Uri.parse("package:${packageName}"))
            startActivity(intent)
            finish()
            return
        }

        // Set window flags for overlay
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        Log.d("TK_BlockingOverlay", "Window flags set")

        // Start the blocking process
        startBlocking(appPackageName)
    }

    private fun startBlocking(appPackageName: String) {
        // Start the blocking process
        handler.post(checkRunnable)
        Log.d("BlockingOverlay", "Blocking started for $appPackageName")
    }

    private fun checkAndKillApp() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)

        if (runningTasks.isNotEmpty()) {
            val topActivity = runningTasks[0].topActivity
            if (topActivity?.packageName == intent.getStringExtra("blocked_app_package")) {
                Log.d("BlockingOverlay", "App ${intent.getStringExtra("blocked_app_package")} is in foreground, killing it")
                killApp()
            }
        }
    }

    private fun killApp() {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(intent.getStringExtra("blocked_app_package"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val method = activityManager.javaClass.getMethod("forceStopPackage", String::class.java)
                    method.invoke(activityManager, intent.getStringExtra("blocked_app_package"))
                } catch (e: Exception) {
                    Log.e("BlockingOverlay", "Error using forceStopPackage", e)
                }
            }

            Log.d("BlockingOverlay", "Successfully killed app: ${intent.getStringExtra("blocked_app_package")}")
            redirectToHomeAndFinish()
        } catch (e: Exception) {
            Log.e("BlockingOverlay", "Error killing app: ${intent.getStringExtra("blocked_app_package")}", e)
        }
    }

    private fun redirectToHomeAndFinish() {
        if (hasRedirected) {
            Log.w("TK_BlockingOverlay", "redirectToHomeAndFinish: already redirected, skipping")
            return
        }
        hasRedirected = true
        Log.d("TK_BlockingOverlay", "redirectToHomeAndFinish: launching MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        startActivity(intent)
        Log.d("TK_BlockingOverlay", "MainActivity started, finishing overlay")
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        Log.d("BlockingOverlay", "onResume called")
        killApp()
    }

    override fun onPause() {
        super.onPause()
        Log.d("BlockingOverlay", "onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BlockingOverlay", "onDestroy called")
        handler.removeCallbacks(checkRunnable)
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    override fun onBackPressed() {
        // Disable back button but call super to maintain proper lifecycle
        super.onBackPressed()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

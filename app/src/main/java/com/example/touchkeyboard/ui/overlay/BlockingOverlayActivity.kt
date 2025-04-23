package com.example.touchkeyboard.ui.overlay


import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.touchkeyboard.R
import com.example.touchkeyboard.utils.OverlayPermissionManager
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.os.Handler
import android.os.Looper


class BlockingOverlayActivity : AppCompatActivity() {
    private lateinit var messageTextView: TextView
    private lateinit var appPackageName: String
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 1000L // Check every second
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAndKillApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BlockingOverlay", "onCreate called")

        // Check if we have overlay permission
        if (!OverlayPermissionManager.hasOverlayPermission(this)) {
            Log.e("BlockingOverlay", "Missing overlay permission")
            OverlayPermissionManager.requestOverlayPermission(this)
            finish()
            return
        }

        // Get the blocked app package name
        appPackageName = intent.getStringExtra("blocked_app_package") ?: ""
        if (appPackageName.isEmpty()) {
            Log.e("BlockingOverlay", "No package name provided")
            finish()
            return
        }

        // Set up window flags
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_blocking_overlay)
        messageTextView = findViewById(R.id.block_message)

        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(appPackageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            messageTextView.text = "You're blocked from $appName. Time to focus!"

            // Initial app kill
            killApp()

            // Start periodic checking
            handler.post(checkRunnable)

        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("BlockingOverlay", "App not found: $appPackageName", e)
            finish()
        }
    }

    private fun checkAndKillApp() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)

        if (runningTasks.isNotEmpty()) {
            val topActivity = runningTasks[0].topActivity
            if (topActivity?.packageName == appPackageName) {
                Log.d("BlockingOverlay", "App $appPackageName is in foreground, killing it")
                killApp()
            }
        }
    }

    private fun killApp() {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(appPackageName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val method = activityManager.javaClass.getMethod("forceStopPackage", String::class.java)
                    method.invoke(activityManager, appPackageName)
                } catch (e: Exception) {
                    Log.e("BlockingOverlay", "Error using forceStopPackage", e)
                }
            }

            Log.d("BlockingOverlay", "Successfully killed app: $appPackageName")
        } catch (e: Exception) {
            Log.e("BlockingOverlay", "Error killing app: $appPackageName", e)
        }
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
}

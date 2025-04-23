package com.example.touchkeyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.touchkeyboard.ui.theme.TouchKeyboardTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import dagger.hilt.android.AndroidEntryPoint
import com.example.touchkeyboard.ui.navigation.MainScreen
import com.example.touchkeyboard.utils.PermissionManager
import javax.inject.Inject
import android.content.Intent
import android.content.pm.ActivityInfo
import com.example.touchkeyboard.services.AppUsageTrackingService




@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        setContent {
            TouchKeyboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(permissionManager = permissionManager)
                }
            }
        }

        if (permissionManager.hasUsageStatsPermission()) {
            startService(Intent(this, AppUsageTrackingService::class.java).apply {
                action = "START_FOREGROUND"
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startService(Intent(this, AppUsageTrackingService::class.java).apply {
            action = "STOP_FOREGROUND"
        })
    }
}

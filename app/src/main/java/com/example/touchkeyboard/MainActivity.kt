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
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.touchkeyboard.services.AppUsageTrackingService
import com.example.touchkeyboard.ui.screens.onboarding.OnboardingNavigator
import com.example.touchkeyboard.data.OnboardingDataStore
import androidx.compose.runtime.collectAsState



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var permissionManager: PermissionManager
    @Inject
    lateinit var onboardingDataStore: OnboardingDataStore

    // Used to detect if coming from block overlay
    private var launchedFromBlockOverlay = false

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TK_MainActivity", "onCreate: intent=$intent flags=${intent?.flags}")
        super.onCreate(savedInstanceState)

        // Detect if launched from BlockingOverlayActivity
        if (intent?.flags?.and(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0) {
            launchedFromBlockOverlay = true
            Log.d("TK_MainActivity", "Launched from block overlay (flags matched)")
        } else {
            Log.d("TK_MainActivity", "Normal launch (flags not matched)")
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            TouchKeyboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isOnboardingComplete = onboardingDataStore.onboardingComplete.collectAsState(initial = false).value
                    if (isOnboardingComplete) {
                        MainScreen()
                    } else {
                        OnboardingNavigator(
                            permissionManager = permissionManager,
                            onboardingDataStore = onboardingDataStore,
                            onOnboardingComplete = {
                                Log.d("TK_MainActivity", "Onboarding completed")
                            }
                        )
                    }
                }
            }
        }

        if (permissionManager.hasUsageStatsPermission()) {
            Log.d("TK_MainActivity", "Starting AppUsageTrackingService")
            startService(Intent(this, AppUsageTrackingService::class.java).apply {
                action = "START_FOREGROUND"
            })
        }
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d("TK_MainActivity", "onNewIntent: intent=$intent flags=${intent?.flags}")
        super.onNewIntent(intent)
        // Detect if coming from block overlay
        if (intent?.flags?.and(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0) {
            launchedFromBlockOverlay = true
            Log.d("TK_MainActivity", "onNewIntent: Launched from block overlay (flags matched)")
            suppressEntryAnimation()
        } else {
            Log.d("TK_MainActivity", "onNewIntent: Normal launch (flags not matched)")
        }
    }

    override fun onResume() {
        Log.d("TK_MainActivity", "onResume: launchedFromBlockOverlay=$launchedFromBlockOverlay")
        super.onResume()
        if (launchedFromBlockOverlay) {
            suppressEntryAnimation()
            launchedFromBlockOverlay = false
            Log.d("TK_MainActivity", "onResume: Suppressed animation and showed notification")
        }
    }

    override fun onDestroy() {
        Log.d("TK_MainActivity", "onDestroy")
        super.onDestroy()
        startService(Intent(this, AppUsageTrackingService::class.java).apply {
            action = "STOP_FOREGROUND"
        })
    }

    private fun suppressEntryAnimation() {
        overridePendingTransition(0, 0)
    }
}




package com.example.touchkeyboard.ui.screens.onboarding

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.example.touchkeyboard.utils.PermissionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.touchkeyboard.data.OnboardingDataStore
import com.example.touchkeyboard.utils.OverlayPermissionManager
import kotlinx.coroutines.launch

@Composable
fun OnboardingNavigator(
    permissionManager: PermissionManager,
    onboardingDataStore: OnboardingDataStore,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(0) }
    var onboardingDone by remember { mutableStateOf(false) }
    val onboardingCompleteFlow = onboardingDataStore.onboardingComplete.collectAsState(initial = false)

    // Handle onboarding state changes
    LaunchedEffect(onboardingCompleteFlow.value) {
        if (onboardingCompleteFlow.value || onboardingDone) {
            onOnboardingComplete()
            return@LaunchedEffect
        }
    }

    // If onboarding is already complete, skip
    if (onboardingCompleteFlow.value || onboardingDone) {
        return
    }

    // Launchers for permission requests
    val usageStatsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (permissionManager.hasUsageStatsPermission()) {
            step++
        }
    }
    val accessibilityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (permissionManager.hasAccessibilityPermission()) {
            step++
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        step++
    }

    when (step) {
        0 -> {
            if (!onboardingCompleteFlow.value) {
                WelcomingScreen(onContinue = { step++ })
            }
        }
        1 -> {
            if (!permissionManager.hasUsageStatsPermission()) {
                PermissionStepScreen(
                    title = "Usage Access",
                    description = "To track your app usage and help you manage screen time, we need Usage Access.",
                    onGrant = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        usageStatsLauncher.launch(intent)
                    },
                    onIgnore = { step++ }
                )
            } else step++
        }
        2 -> {
            if (!permissionManager.hasAccessibilityPermission()) {
                PermissionStepScreen(
                    title = "Accessibility Service",
                    description = "Enable the TouchKeyboard Accessibility Service for full functionality.",
                    onGrant = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        accessibilityLauncher.launch(intent)
                    },
                    onIgnore = { step++ }
                )
            } else step++
        }
        3 -> {
            if (!OverlayPermissionManager.hasOverlayPermission(context)) {
                PermissionStepScreen(
                    title = "Draw Over Other Apps",
                    description = "To block distracting apps, we need permission to draw over other apps.",
                    onGrant = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            .setData(Uri.parse("package:${context.packageName}"))
                        usageStatsLauncher.launch(intent)
                    },
                    onIgnore = { step++ }
                )
            } else step++
        }
        4 -> {
            if (!permissionManager.hasCameraPermission()) {
                PermissionStepScreen(
                    title = "Camera Access",
                    description = "Camera access is needed for keyboard verification.",
                    onGrant = {
                        cameraLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    onIgnore = { step++ }
                )
            } else step++
        }
        else -> {
            // Mark onboarding as complete in DataStore
            LaunchedEffect(Unit) {
                scope.launch {
                    onboardingDataStore.setOnboardingComplete(true)
                    onboardingDone = true
                }
            }
        }
    }
}

@Composable
fun PermissionStepScreen(
    title: String,
    description: String,
    onGrant: () -> Unit,
    onIgnore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onGrant) { Text("Grant") }
            OutlinedButton(onClick = onIgnore) { Text("Ignore") }
        }
    }
}

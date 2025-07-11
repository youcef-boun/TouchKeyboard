package com.example.touchkeyboard.ui.screens.permission


import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import android.os.Build
import android.content.ComponentName
import com.example.touchkeyboard.services.TouchKeyboardAccessibilityService

private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS = "android.settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS"
private const val EXTRA_ACCESSIBILITY_SERVICE_COMPONENT_NAME = "android.provider.extra.ACCESSIBILITY_SERVICE_COMPONENT_NAME"

// --- Permission Cards ---

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = onClick,
                enabled = !granted,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (granted) "Granted" else buttonText)
            }
        }
    }
}

// --- Main PermissionScreen Composable ---

@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onPermissionsResult(permissions)
    }

    val usageStatsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (viewModel.checkUsageStatsPermission()) {
            viewModel.onUsageStatsGranted()
        }
    }

    val packageAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (viewModel.checkPackageQueryPermission()) {
            viewModel.onPackageQueryGranted()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentViewModel by rememberUpdatedState(viewModel)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentViewModel.notifyScreenResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.allPermissionsGranted && viewModel.isAccessibilityGranted()) {
        if (state.allPermissionsGranted && viewModel.isAccessibilityGranted()) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to TouchKeyboard",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "We need a few permissions to help you manage your screen time effectively:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Usage Stats Permission
        PermissionCard(
            title = "Usage Access",
            description = "To track your app usage and help you manage screen time",
            granted = state.usageStatsGranted,
            buttonText = "Grant Access",
            onClick = { usageStatsLauncher.launch(viewModel.getUsageStatsIntent()) }
        )

        // Accessibility Permission
        PermissionCard(
            title = "Accessibility Service",
            description = "Enable the TouchKeyboard Accessibility Service for full functionality.",
            granted = viewModel.isAccessibilityGranted(),
            buttonText = "Enable Service",
            onClick = {
                val detailsIntent = Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
                    putExtra(
                        EXTRA_ACCESSIBILITY_SERVICE_COMPONENT_NAME,
                        ComponentName(
                            context,
                            TouchKeyboardAccessibilityService::class.java
                        ).flattenToString()
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val canHandle = detailsIntent.resolveActivity(context.packageManager) != null
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canHandle) {
                    detailsIntent
                } else {
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
            }
        )

        // Package Query Permission
        PermissionCard(
            title = "App List Access",
            description = "To view and manage your list of installed apps",
            granted = state.packageQueryGranted,
            buttonText = "Grant Access",
            onClick = { packageAccessLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }) }
        )

        // Other Permissions
        PermissionCard(
            title = "Additional Permissions",
            description = "Camera access for keyboard verification and notifications for app blocking",
            granted = state.otherPermissionsGranted,
            buttonText = "Grant Access",
            onClick = {
                permissionLauncher.launch(viewModel.getRequiredPermissions().toTypedArray())
            }
        )
    }
}

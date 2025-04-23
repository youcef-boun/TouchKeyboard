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
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import android.os.Build
import android.content.ComponentName
import com.example.touchkeyboard.services.TouchKeyboardAccessibilityService

private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS = "android.settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS"
private const val EXTRA_ACCESSIBILITY_SERVICE_COMPONENT_NAME = "android.provider.extra.ACCESSIBILITY_SERVICE_COMPONENT_NAME"

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
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Usage Access",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "To track your app usage and help you manage screen time",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = {
                        usageStatsLauncher.launch(viewModel.getUsageStatsIntent())
                    },
                    enabled = !state.usageStatsGranted,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (state.usageStatsGranted) "Granted" else "Grant Access")
                }
            }
        }

        // Accessibility Permission
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Accessibility Service",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Enable the TouchKeyboard Accessibility Service for full functionality.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
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
                    },
                    enabled = !viewModel.isAccessibilityGranted(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (viewModel.isAccessibilityGranted()) "Granted" else "Enable Service")
                }
            }
        }

        // Package Query Permission
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "App List Access",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "To view and manage your list of installed apps",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = {
                        packageAccessLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                    enabled = !state.packageQueryGranted,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (state.packageQueryGranted) "Granted" else "Grant Access")
                }
            }
        }

        // Other Permissions
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Additional Permissions",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Camera access for keyboard verification and notifications for app blocking",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(viewModel.getRequiredPermissions().toTypedArray())
                    },
                    enabled = !state.otherPermissionsGranted || state.showRetryButton,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (state.otherPermissionsGranted) "Granted" else "Grant Access")
                }
            }
        }
    }
}

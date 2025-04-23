package com.example.touchkeyboard.ui.screens.connection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.touchkeyboard.R
import com.example.touchkeyboard.permissions.UsagePermissionManager

@Composable
fun ConnectionScreen(
    permissionManager: UsagePermissionManager,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPermissionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if permission was granted after returning from settings
        if (permissionManager.hasUsagePermission()) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_digital_wellbeing3),
            contentDescription = "Digital Wellbeing Icon",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Connect to Digital Wellbeing",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We use this to access your screen time data and help you maintain a healthy digital balance",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!permissionManager.hasUsagePermission()) {
                    showPermissionDialog = true
                } else {
                    onPermissionGranted()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = {
                Text(
                    "To track your screen time, we need access to usage data. " +
                            "Please enable 'Usage Access' in the next screen."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        permissionManager.requestUsagePermission(permissionLauncher)
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
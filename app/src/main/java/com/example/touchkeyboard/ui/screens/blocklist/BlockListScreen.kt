package com.touchkeyboard.ui.screens.blocklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.touchkeyboard.domain.models.BlockedApp
import com.example.touchkeyboard.ui.theme.BackgroundDark
import com.touchkeyboard.ui.viewmodels.BlockListViewModel
import com.touchkeyboard.ui.viewmodels.InstalledApp



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockListScreen(
    viewModel: BlockListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddAppDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showAddAppDialog) {
        if (showAddAppDialog) {
            viewModel.refreshInstalledApps()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.missingPermissions.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddAppDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add App"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add App")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                if (uiState.error != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.missingPermissions.isNotEmpty()) {
                            Text(
                                text = "Missing permissions:",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            uiState.missingPermissions.forEach { permission ->
                                Text(
                                    text = permission,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Blocked Apps",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.blockedApps) { app ->
                                BlockedAppItem(
                                    app = app,
                                    onRemove = { viewModel.removeAppFromBlockList(app.packageName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Available Apps",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.installedApps.filter { app ->
                                !uiState.blockedApps.any { it.packageName == app.packageName }
                            }) { app ->
                                InstalledAppItem(
                                    app = app,
                                    onSelect = { viewModel.addAppToBlockList(app.packageName, app.appName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add app dialog
    if (showAddAppDialog) {
        AddAppDialog(
            installedApps = uiState.installedApps.filter { app ->
                !uiState.blockedApps.any { it.packageName == app.packageName }
            },
            onAddApp = { packageName, appName ->
                viewModel.addAppToBlockList(packageName, appName)
                showAddAppDialog = false
            },
            onDismiss = { showAddAppDialog = false },
            error = uiState.error
        )
    }
}

@Composable
fun BlockedAppItem(
    app: BlockedApp,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = app.appName,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun InstalledAppItem(
    app: InstalledApp,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = app.appName,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AddAppDialog(
    installedApps: List<InstalledApp>,
    onAddApp: (String, String) -> Unit,
    onDismiss: () -> Unit,
    error: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App") },
        text = {
            LazyColumn {
                items(installedApps) { app ->
                    InstalledAppItem(
                        app = app,
                        onSelect = {
                            onAddApp(app.packageName, app.appName)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}
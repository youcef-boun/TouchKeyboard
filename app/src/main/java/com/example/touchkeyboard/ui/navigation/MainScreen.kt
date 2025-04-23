package com.example.touchkeyboard.ui.navigation


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.touchkeyboard.ui.components.BottomNavBar
import com.example.touchkeyboard.ui.screens.permission.PermissionScreen
import com.example.touchkeyboard.utils.PermissionManager


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MainScreen(
    permissionManager: PermissionManager
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember { mutableStateOf(checkAllPermissions(permissionManager)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = checkAllPermissions(permissionManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!permissionState) {
        PermissionScreen(
            onPermissionsGranted = {
                permissionState = checkAllPermissions(permissionManager)
            }
        )
    } else {
        val navController = rememberNavController()
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            ?: AppDestinations.PROFILE

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppNavigation(navController = navController)
                }
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

private fun checkAllPermissions(permissionManager: PermissionManager): Boolean {
    return permissionManager.hasUsageStatsPermission() &&
            permissionManager.hasCameraPermission() &&
            permissionManager.hasNotificationPermission() &&
            permissionManager.hasAccessibilityPermission()
}

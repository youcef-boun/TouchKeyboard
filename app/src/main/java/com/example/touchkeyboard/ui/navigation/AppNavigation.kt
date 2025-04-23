package com.example.touchkeyboard.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.touchkeyboard.permissions.UsagePermissionManager
import com.example.touchkeyboard.ui.screens.connection.ConnectionScreen
import com.touchkeyboard.ui.screens.blocklist.BlockListScreen
import com.example.touchkeyboard.ui.screens.home.HomeScreen
import com.touchkeyboard.ui.screens.profile.ProfileScreen
import com.touchkeyboard.ui.screens.verification.VerificationScreen

/**
 * Main navigation component for the TouchKeyboard app
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val permissionManager = remember { UsagePermissionManager(context) }
    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME
    ) {
        composable(AppDestinations.HOME) {

            HomeScreen(
                onPermissionRequired = {
                    navController.navigate(AppDestinations.CONNECTION)
                }
            )
        }

        composable(AppDestinations.CONNECTION) {

            ConnectionScreen(
                onPermissionGranted = {
                    navController.popBackStack()
                },
                permissionManager = permissionManager,
                modifier = Modifier,
            )
        }

        composable(AppDestinations.PROFILE) {
            ProfileScreen()
        }

        composable(AppDestinations.VERIFICATION) {
            VerificationScreen(
                onVerificationComplete = {
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppDestinations.BLOCK_LIST) {
            BlockListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Object containing all navigation destinations in the app
 */
object AppDestinations {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val VERIFICATION = "verification"
    const val BLOCK_LIST = "block_list"
    const val CONNECTION = "connection"
}
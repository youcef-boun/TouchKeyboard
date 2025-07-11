package com.example.touchkeyboard.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.touchkeyboard.data.OnboardingDataStore
import com.example.touchkeyboard.permissions.UsagePermissionManager
import com.example.touchkeyboard.ui.screens.connection.ConnectionScreen
import com.example.touchkeyboard.ui.screens.home.HomeScreen
import com.example.touchkeyboard.ui.screens.onboarding.OnboardingNavigator
import com.example.touchkeyboard.utils.PermissionManager
import com.touchkeyboard.ui.screens.blocklist.BlockListScreen
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
    val permissionManager = remember { PermissionManager(context) }
    val onboardingDataStore = remember { OnboardingDataStore(context) }
    var onboardingCompleteLocal by rememberSaveable { mutableStateOf(false) }
    val onboardingCompleteFlow = onboardingDataStore.onboardingComplete.collectAsState(initial = false)

    val onboardingDone = onboardingCompleteFlow.value || onboardingCompleteLocal
    if (!onboardingDone) {
        OnboardingNavigator(
            permissionManager = permissionManager,
            onboardingDataStore = onboardingDataStore,
            onOnboardingComplete = { onboardingCompleteLocal = true }
        )
        return
    }

    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME
    ) {
        composable(AppDestinations.HOME) {
            val homeViewModel: com.example.touchkeyboard.ui.viewmodels.HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onPermissionRequired = {
                    android.util.Log.d("AppNavigation", "Navigating to ConnectionScreen due to missing permission")
                    navController.navigate(AppDestinations.CONNECTION)
                }
            )
        }

        composable(AppDestinations.CONNECTION) {
            val homeViewModel: com.example.touchkeyboard.ui.viewmodels.HomeViewModel = hiltViewModel()
            ConnectionScreen(
                onPermissionGranted = {
                    android.util.Log.d("AppNavigation", "Permission granted, refreshing HomeViewModel data")
                    homeViewModel.refreshData()
                    // Log permission state after refresh
                    android.util.Log.d("AppNavigation", "hasUsagePermission after refresh: ${homeViewModel.hasUsagePermission.value}")
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
    const val BLOCK_LIST = "block_list"
    const val CONNECTION = "connection"
    const val VERIFICATION = "verification"

}
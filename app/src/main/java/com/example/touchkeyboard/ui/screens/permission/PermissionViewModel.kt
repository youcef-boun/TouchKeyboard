package com.example.touchkeyboard.ui.screens.permission

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.touchkeyboard.utils.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PermissionState(
    val usageStatsGranted: Boolean = false,
    val packageQueryGranted: Boolean = false,
    val otherPermissionsGranted: Boolean = false,
    val showRetryButton: Boolean = false,
    val allPermissionsGranted: Boolean = false
)

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionState())
    val state: StateFlow<PermissionState> = _state

    val isAccessibilityGranted: () -> Boolean = {
        permissionManager.hasAccessibilityPermission()
    }

    init {
        checkInitialPermissions()
    }

    private fun checkInitialPermissions() {
        val usageStats = permissionManager.hasUsageStatsPermission()
        val packageQuery = permissionManager.hasPackageQueryPermission()
        val others = checkOtherPermissions()
        updateState(usageStats, packageQuery, others)
    }

    fun checkUsageStatsPermission(): Boolean {
        val granted = permissionManager.hasUsageStatsPermission()
        _state.update { it.copy(usageStatsGranted = granted) }
        return granted
    }

    fun checkPackageQueryPermission(): Boolean {
        val granted = permissionManager.hasPackageQueryPermission()
        _state.update { it.copy(packageQueryGranted = granted) }
        return granted
    }

    fun onUsageStatsGranted() {
        _state.update { it.copy(usageStatsGranted = true) }
        checkAllPermissions()
    }

    fun onPackageQueryGranted() {
        _state.update { it.copy(packageQueryGranted = true) }
        checkAllPermissions()
    }

    fun onPermissionsResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.all { it.value }
        _state.update {
            it.copy(
                otherPermissionsGranted = allGranted,
                showRetryButton = !allGranted
            )
        }
        checkAllPermissions()
    }

    private fun checkOtherPermissions(): Boolean {
        return permissionManager.hasCameraPermission() &&
                permissionManager.hasNotificationPermission()
    }

    private fun checkAllPermissions() {
        val usageStats = _state.value.usageStatsGranted
        val packageQuery = _state.value.packageQueryGranted
        val others = _state.value.otherPermissionsGranted
        updateState(usageStats, packageQuery, others)
    }

    private fun updateState(usageStats: Boolean, packageQuery: Boolean, others: Boolean) {
        _state.update {
            it.copy(
                usageStatsGranted = usageStats,
                packageQueryGranted = packageQuery,
                otherPermissionsGranted = others,
                showRetryButton = !usageStats || !packageQuery || !others,
                allPermissionsGranted = usageStats && packageQuery && others
            )
        }
    }

    fun getUsageStatsIntent(): Intent {
        return permissionManager.getUsageStatsIntent()
    }

    fun getRequiredPermissions(): List<String> {
        return permissionManager.getRequiredPermissions()
    }

    fun notifyScreenResumed() {
        // Re-check all permissions including accessibility
        val usageStats = permissionManager.hasUsageStatsPermission()
        val packageQuery = permissionManager.hasPackageQueryPermission()
        val others = checkOtherPermissions()
        _state.update {
            it.copy(
                usageStatsGranted = usageStats,
                packageQueryGranted = packageQuery,
                otherPermissionsGranted = others
            )
        }
    }
}

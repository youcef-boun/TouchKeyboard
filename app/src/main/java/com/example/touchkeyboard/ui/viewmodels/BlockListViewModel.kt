package com.touchkeyboard.ui.viewmodels


import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchkeyboard.domain.models.BlockedApp
import com.touchkeyboard.domain.usecases.blockedapps.GetBlockedAppsUseCase
import com.touchkeyboard.domain.usecases.blockedapps.ManageBlockedAppsUseCase
import com.example.touchkeyboard.utils.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.touchkeyboard.domain.repositories.IBlockListRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.firstOrNull


/**
 * Domain model for installed applications
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false
)

data class BlockListUiState(
    val blockedApps: List<BlockedApp> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val missingPermissions: List<String> = emptyList(),
    val isBlockListEnabled: Boolean = true
)

@HiltViewModel
class BlockListViewModel @Inject constructor(
    private val getBlockedAppsUseCase: GetBlockedAppsUseCase,
    private val manageBlockedAppsUseCase: ManageBlockedAppsUseCase,
    private val packageManager: PackageManager,
    private val permissionManager: PermissionManager,
    @ApplicationContext private val context: Context,
    private val blockListRepository: IBlockListRepository
) : ViewModel() {
    private val TAG = "BlockListViewModel"

    private val _uiState = MutableStateFlow(BlockListUiState())
    val uiState: StateFlow<BlockListUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Load blocked apps
                getBlockedAppsUseCase.getAllBlockedApps()
                    .catch { e ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to load blocked apps: ${e.message}"
                        ) }
                    }
                    .collect { blockedApps ->
                        // Filter out temporarily unblocked apps
                        val currentlyBlockedApps = blockedApps.filter { it.isCurrentlyBlocked }
                        _uiState.update { it.copy(
                            isLoading = false,
                            blockedApps = currentlyBlockedApps
                        ) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load data: ${e.message}"
                ) }
            }
        }
    }

    private fun isAppBlocked(packageName: String): Boolean {
        return runBlocking {
            try {
                val blockedApps = getBlockedAppsUseCase.getAllBlockedApps()
                    .firstOrNull()
                    ?: emptyList()
                Log.d(TAG, "Checking block status for $packageName")
                Log.d(TAG, "Blocked apps: ${blockedApps.map { it.packageName }}")
                val isBlocked = blockedApps.any { it.packageName == packageName && it.isCurrentlyBlocked }
                Log.d(TAG, "isAppBlocked($packageName): $isBlocked")
                isBlocked
            } catch (e: Exception) {
                Log.e(TAG, "Error checking block status", e)
                false
            }
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            val installedApps = loadInstalledApps()
            _uiState.update { it.copy(installedApps = installedApps) }
        }
    }

    private fun loadInstalledApps(): List<InstalledApp> {
        Log.d(TAG, "Loading installed apps")
        if (!permissionManager.hasPackageQueryPermission()) {
            Log.e(TAG, "Package query permission not granted")
            _uiState.update {
                it.copy(
                    error = "Permission required to access app list. Please grant the required permissions in Settings.",
                    isLoading = false
                )
            }
            return emptyList()
        }

        try {
            val pm = packageManager
            // Only get apps that have a launcher intent (user-visible)
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
            launcherIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val resolvedActivities = pm.queryIntentActivities(launcherIntent, 0)
            val userVisiblePackages = resolvedActivities.map { it.activityInfo.packageName }.toSet()

            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            Log.d(TAG, "Found ${packages.size} installed apps")

            return packages
                .filter { !it.packageName.equals(context.packageName) }  // Don't show our own app
                .filter { userVisiblePackages.contains(it.packageName) } // Only apps with launcher icon
                .mapNotNull { appInfo ->
                    try {
                        val label = pm.getApplicationLabel(appInfo).toString()
                        Log.d(TAG, "Processing app: $label (${appInfo.packageName})")
                        InstalledApp(
                            packageName = appInfo.packageName,
                            appName = label,
                            isBlocked = isAppBlocked(appInfo.packageName)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing app ${appInfo.packageName}", e)
                        null
                    }
                }
                .sortedBy { it.appName }
                .also { apps ->
                    Log.d(TAG, "Filtered down to ${apps.size} user-visible apps")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading installed apps", e)
            _uiState.update { it.copy(error = "Failed to load installed apps") }
            return emptyList()
        }
    }

    fun addAppToBlockList(packageName: String, appName: String) {
        viewModelScope.launch {
            try {
                manageBlockedAppsUseCase.addAppToBlockList(packageName, appName)
                loadData() // Reload data after adding
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Failed to add app: ${e.message}"
                ) }
            }
        }
    }

    fun removeAppFromBlockList(packageName: String) {
        viewModelScope.launch {
            try {
                manageBlockedAppsUseCase.removeAppFromBlockList(packageName)
                loadData() // Reload data after removing
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Failed to remove app: ${e.message}"
                ) }
            }
        }
    }

    fun toggleBlockList(enabled: Boolean) {
        viewModelScope.launch {
            try {
                blockListRepository.setBlockListEnabled(enabled)
                _uiState.update { it.copy(isBlockListEnabled = enabled) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Failed to toggle block list: ${e.message}"
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
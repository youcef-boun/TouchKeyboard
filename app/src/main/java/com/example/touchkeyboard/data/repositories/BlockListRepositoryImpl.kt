package com.example.touchkeyboard.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

import androidx.datastore.preferences.preferencesDataStore
import com.example.touchkeyboard.domain.repositories.IBlockListRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.touchkeyboard.domain.models.BlockedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

import android.content.SharedPreferences

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "block_list")

@Singleton
class BlockListRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IBlockListRepository {

    private val TAG = "BlockListRepository"
    private val prefs: SharedPreferences = context.getSharedPreferences("block_list_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val blockedAppsFlow = MutableStateFlow<List<BlockedApp>>(emptyList())
    private val isBlockListEnabledFlow = MutableStateFlow(true)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Load initial data
        loadBlockedApps()
        loadBlockListEnabled()

        // Enable block list by default if not set
        if (!prefs.contains(KEY_BLOCK_LIST_ENABLED)) {
            scope.launch {
                setBlockListEnabled(true)
            }
        }
    }

    private fun loadBlockedApps() {
        try {
            val json = prefs.getString(KEY_BLOCKED_APPS, "[]")
            val type = object : TypeToken<List<BlockedApp>>() {}.type
            val apps = gson.fromJson<List<BlockedApp>>(json, type) ?: emptyList()
            blockedAppsFlow.value = apps
            Log.d(TAG, "Loaded blocked apps: $apps")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading blocked apps", e)
            blockedAppsFlow.value = emptyList()
        }
    }

    private fun loadBlockListEnabled() {
        try {
            val isEnabled = prefs.getBoolean(KEY_BLOCK_LIST_ENABLED, true)
            isBlockListEnabledFlow.value = isEnabled
            Log.d(TAG, "Block list enabled: $isEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading block list enabled state", e)
            isBlockListEnabledFlow.value = true
        }
    }

    override suspend fun isAppBlocked(packageName: String): Boolean {
        val isEnabled = isBlockListEnabledFlow.value
        val blockedApps = blockedAppsFlow.value

        Log.d(TAG, """
            Checking if app is blocked:
            Package: $packageName
            Block list enabled: $isEnabled
            Blocked apps: $blockedApps
        """.trimIndent())

        if (!isEnabled) {
            Log.d(TAG, "Block list is disabled, app is not blocked")
            return false
        }

        val isBlocked = blockedApps.any { it.packageName == packageName && it.isCurrentlyBlocked }
        Log.d(TAG, "App $packageName is blocked: $isBlocked")
        return isBlocked
    }

    override suspend fun getBlockedApps(): List<BlockedApp> {
        return blockedAppsFlow.value
    }

    override suspend fun addAppToBlockList(packageName: String, appName: String) {
        val currentApps = blockedAppsFlow.value.toMutableList()
        if (!currentApps.any { it.packageName == packageName }) {
            val app = BlockedApp(
                packageName = packageName,
                appName = appName,
                isCurrentlyBlocked = true,
                blockStartTime = 0,
                blockEndTime = 0,
                isBlockedIndefinitely = true,
                remainingBlockTime = 0
            )
            currentApps.add(app)
            saveBlockedApps(currentApps)
            Log.d(TAG, "Added app to block list: $packageName ($appName)")
        }
    }

    override suspend fun removeAppFromBlockList(packageName: String) {
        val currentApps = blockedAppsFlow.value.toMutableList()
        currentApps.removeAll { it.packageName == packageName }
        saveBlockedApps(currentApps)
        Log.d(TAG, "Removed app from block list: $packageName")
    }

    private fun saveBlockedApps(apps: List<BlockedApp>) {
        try {
            val json = gson.toJson(apps)
            prefs.edit().putString(KEY_BLOCKED_APPS, json).apply()
            blockedAppsFlow.value = apps
            Log.d(TAG, "Saved blocked apps: $apps")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving blocked apps", e)
        }
    }

    override suspend fun isBlockListEnabled(): Boolean {
        return isBlockListEnabledFlow.value
    }

    override suspend fun setBlockListEnabled(enabled: Boolean) {
        try {
            prefs.edit().putBoolean(KEY_BLOCK_LIST_ENABLED, enabled).apply()
            isBlockListEnabledFlow.value = enabled
            Log.d(TAG, "Block list enabled set to: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting block list enabled state", e)
        }
    }

    override fun getAllBlockedApps(): Flow<List<BlockedApp>> {
        return blockedAppsFlow.asStateFlow()
            .catch { e ->
                Log.e(TAG, "Error in getAllBlockedApps flow", e)
                emit(emptyList())
            }
    }

    override suspend fun unblockAppsTemporarily(packageNames: List<String>, durationMinutes: Int) {
        val currentApps = blockedAppsFlow.value.toMutableList()
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60 * 1000)

        val updatedApps = currentApps.map { app ->
            if (packageNames.contains(app.packageName)) {
                app.copy(
                    blockStartTime = now,
                    blockEndTime = endTime,
                    isCurrentlyBlocked = false
                )
            } else {
                app
            }
        }

        saveBlockedApps(updatedApps)
        Log.d(TAG, "Temporarily unblocked apps $packageNames for $durationMinutes minutes")
    }

    override suspend fun isAppTemporarilyUnblocked(packageName: String): Boolean {
        val app = blockedAppsFlow.value.find { it.packageName == packageName }
        if (app == null) return false

        val now = System.currentTimeMillis()
        return !app.isCurrentlyBlocked && now < app.blockEndTime
    }

    companion object {
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_BLOCK_LIST_ENABLED = "block_list_enabled"
    }
}
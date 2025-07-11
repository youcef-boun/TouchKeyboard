package com.example.touchkeyboard.domain.repositories




import com.touchkeyboard.domain.models.BlockedApp
import kotlinx.coroutines.flow.Flow

interface IBlockListRepository {
    fun getAllBlockedApps(): Flow<List<BlockedApp>>
    suspend fun getBlockedApps(): List<BlockedApp>
    suspend fun isAppBlocked(packageName: String): Boolean
    suspend fun addAppToBlockList(packageName: String, appName: String)
    suspend fun removeAppFromBlockList(packageName: String)
    suspend fun isBlockListEnabled(): Boolean
    suspend fun setBlockListEnabled(enabled: Boolean)
    suspend fun unblockAppsTemporarily(packageNames: List<String>, durationMinutes: Int)
    suspend fun unblockAppsTemporarilyMs(packageNames: List<String>, durationMs: Long)
    suspend fun isAppTemporarilyUnblocked(packageName: String): Boolean
}
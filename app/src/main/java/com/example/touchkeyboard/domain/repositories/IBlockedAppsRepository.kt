package com.touchkeyboard.domain.repositories

import com.touchkeyboard.domain.models.BlockedApp
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for blocked apps operations
 */
interface IBlockedAppsRepository {
    fun getAllBlockedApps(): Flow<List<BlockedApp>>
    fun getCurrentlyBlockedApps(): Flow<List<BlockedApp>>
    suspend fun addBlockedApp(packageName: String, appName: String)
    suspend fun removeBlockedApp(packageName: String)
    suspend fun updateBlockStatus(packageName: String, isBlocked: Boolean)
    suspend fun unblockAppsTemporarily(packageNames: List<String>, durationMinutes: Int)
    suspend fun checkAndUpdateBlockStatus()
} 
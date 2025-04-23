package com.touchkeyboard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.touchkeyboard.data.models.BlockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppsDao {
    
    @Query("SELECT * FROM blocked_apps")
    fun getAllBlockedApps(): Flow<List<BlockedApp>>
    
    @Query("SELECT * FROM blocked_apps WHERE isCurrentlyBlocked = 1")
    fun getCurrentlyBlockedApps(): Flow<List<BlockedApp>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(blockedApp: BlockedApp)
    
    @Update
    suspend fun updateBlockedApp(blockedApp: BlockedApp)
    
    @Delete
    suspend fun deleteBlockedApp(blockedApp: BlockedApp)
    
    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedAppByPackageName(packageName: String)
    
    @Query("UPDATE blocked_apps SET isCurrentlyBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateBlockStatus(packageName: String, isBlocked: Boolean)
    
    @Query("UPDATE blocked_apps SET blockEndTime = :endTime WHERE packageName = :packageName")
    suspend fun updateBlockEndTime(packageName: String, endTime: Long)
    
    @Query("UPDATE blocked_apps SET isCurrentlyBlocked = 0 WHERE blockEndTime > 0 AND blockEndTime <= :currentTime")
    suspend fun unblockExpiredApps(currentTime: Long)
} 
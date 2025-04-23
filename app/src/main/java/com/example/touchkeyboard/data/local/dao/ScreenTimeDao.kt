package com.touchkeyboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.touchkeyboard.data.models.ScreenTimeSession
import kotlinx.coroutines.flow.Flow
import com.example.touchkeyboard.data.models.AppUsage


@Dao
interface ScreenTimeDao {
    @Query("SELECT * FROM screen_time_sessions ORDER BY startTime DESC")
    fun getAllScreenTimeSessions(): Flow<List<ScreenTimeSession>>

    @Query("SELECT * FROM screen_time_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getScreenTimeSessionsByDate(date: String): Flow<List<ScreenTimeSession>>

    @Query("SELECT SUM(duration) FROM screen_time_sessions WHERE date = :date")
    fun getTotalScreenTimeForDate(date: String): Flow<Long>

    @Query("SELECT SUM(duration) FROM screen_time_sessions WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalScreenTimeForDateRange(startDate: String, endDate: String): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenTimeSession(screenTimeSession: ScreenTimeSession)

    @Query("DELETE FROM screen_time_sessions WHERE date < :date")
    suspend fun deleteOldScreenTimeSessions(date: String)

    @Query("SELECT * FROM app_usage WHERE date = :date")
    fun getAppUsageForDate(date: String): Flow<List<AppUsage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(appUsage: AppUsage)
}
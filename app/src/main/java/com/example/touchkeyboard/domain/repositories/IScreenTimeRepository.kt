package com.touchkeyboard.domain.repositories


import com.example.touchkeyboard.domain.models.AppScreenTime
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.LocalDate

/**
 * Repository interface for screen time operations
 */
interface IScreenTimeRepository {
    /**
     * Get total screen time for a specific date
     */
    fun getScreenTimeForDate(date: LocalDate): Flow<Duration>

    /**
     * Get per-app usage statistics for a specific date
     */
    fun getAppUsageForDate(date: LocalDate): Flow<List<AppScreenTime>>

    /**
     * Record screen time for an app
     */
    suspend fun recordAppUsage(packageName: String, appName: String, duration: Duration)
}
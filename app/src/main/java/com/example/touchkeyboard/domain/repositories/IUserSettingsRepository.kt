package com.touchkeyboard.domain.repositories

import com.example.touchkeyboard.ui.screens.onboarding.UserGoal
import com.touchkeyboard.domain.models.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user settings operations
 */
interface IUserSettingsRepository {
    fun getUserSettings(): Flow<UserSettings>
    suspend fun saveUserSettings(userSettings: UserSettings)
    suspend fun updateUserGoal(goal: UserGoal)
    suspend fun incrementKeyboardTouchCount()
    suspend fun decrementRemainingSkips()
    fun getKeyboardTouchCount(): Flow<Int>
} 
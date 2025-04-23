package com.touchkeyboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.touchkeyboard.data.models.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(userSettings: UserSettings)
    
    @Update
    suspend fun updateUserSettings(userSettings: UserSettings)
    
    @Query("UPDATE user_settings SET goal = :goal WHERE id = 1")
    suspend fun updateUserGoal(goal: String)
    
    @Query("UPDATE user_settings SET keyboardTouchCount = keyboardTouchCount + 1 WHERE id = 1")
    suspend fun incrementKeyboardTouchCount()
    
    @Query("UPDATE user_settings SET remainingSkips = remainingSkips - 1 WHERE id = 1 AND remainingSkips > 0")
    suspend fun decrementRemainingSkips()
    
    @Query("SELECT keyboardTouchCount FROM user_settings WHERE id = 1")
    fun getKeyboardTouchCount(): Flow<Int>
} 
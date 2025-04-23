package com.touchkeyboard.data.local.converters

import androidx.room.TypeConverter
import com.example.touchkeyboard.ui.screens.onboarding.UserGoal


/**
 * Type converters for Room database to handle complex types
 */
class Converters {
    
    @TypeConverter
    fun fromUserGoal(value: UserGoal): String {
        return value.name
    }
    
    @TypeConverter
    fun toUserGoal(value: String): UserGoal {
        return try {
            UserGoal.valueOf(value)
        } catch (e: IllegalArgumentException) {
            UserGoal.BE_PRESENT
        }
    }
} 
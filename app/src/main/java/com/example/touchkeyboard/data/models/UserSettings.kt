package com.touchkeyboard.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.touchkeyboard.ui.screens.onboarding.UserGoal


@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,  // Single instance
    val goal: UserGoal = UserGoal.BE_PRESENT,
    val ageRange: String = "25-34",
    val dailyScreenTimeTarget: Int = 240, // In minutes
    val keyboardTouchCount: Int = 0,
    val isSubscriptionActive: Boolean = false,
    val remainingSkips: Int = 3
) 
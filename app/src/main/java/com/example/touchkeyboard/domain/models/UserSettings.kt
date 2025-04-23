package com.touchkeyboard.domain.models

import com.example.touchkeyboard.ui.screens.onboarding.UserGoal


/**
 * Domain model for user settings
 */
data class UserSettings(
    val goal: UserGoal = UserGoal.BE_PRESENT,
    val ageRange: String = "25-34",
    val dailyScreenTimeTarget: Int = 240, // In minutes
    val keyboardTouchCount: Int = 0,
    val isSubscriptionActive: Boolean = false,
    val remainingSkips: Int = 3
) 
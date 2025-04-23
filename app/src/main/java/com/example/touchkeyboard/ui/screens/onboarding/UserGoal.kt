package com.example.touchkeyboard.ui.screens.onboarding

/**
 * Represents the different goals a user can select
 */
enum class UserGoal(val title: String, val description: String) {
    BE_PRESENT("Be present", "Limit distractions and stay in the moment"),
    CONNECT_PEOPLE("Connect with people", "Reduce screen time and connect with loved ones"),
    FOCUS_WORK("Focus on work", "Increase productivity by reducing digital distractions"),
    CUSTOM("Custom goal", "Set your own personalized goal")
}
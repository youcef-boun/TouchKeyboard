package com.touchkeyboard.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain model for screen time tracking
 */
data class ScreenTimeSession(
    val id: Long = 0,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val duration: Long, // Duration in milliseconds
    val date: LocalDate
)

/**
 * Stats model for screen time visualization
 */
data class ScreenTimeStats(
    val date: LocalDate,
    val totalScreenTime: Long, // in milliseconds
    val targetAchieved: Boolean
) 
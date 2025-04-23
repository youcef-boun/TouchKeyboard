package com.touchkeyboard.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain model for keyboard verification tracking
 */
data class KeyboardVerification(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val isSuccessful: Boolean,
    val unlockDuration: Int = 0, // Duration in minutes for which apps are unlocked
    val date: LocalDate
) 
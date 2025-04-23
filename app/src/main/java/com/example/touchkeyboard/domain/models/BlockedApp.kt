package com.touchkeyboard.domain.models

/**
 * Domain model for blocked applications
 */
data class BlockedApp(
    val packageName: String,
    val appName: String,
    val isCurrentlyBlocked: Boolean = true,
    val blockStartTime: Long = 0,
    val blockEndTime: Long = 0,
    val isBlockedIndefinitely: Boolean = true,
    val remainingBlockTime: Long = 0 // Calculated field for UI
) 
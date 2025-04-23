package com.touchkeyboard.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isCurrentlyBlocked: Boolean = true,
    val blockStartTime: Long = 0,  // Start time in milliseconds
    val blockEndTime: Long = 0,    // End time in milliseconds
    val isBlockedIndefinitely: Boolean = true
) 
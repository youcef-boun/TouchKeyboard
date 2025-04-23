package com.touchkeyboard.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time_sessions")
data class ScreenTimeSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,  // Duration in milliseconds
    val date: String    // Date in YYYY-MM-DD format
) 
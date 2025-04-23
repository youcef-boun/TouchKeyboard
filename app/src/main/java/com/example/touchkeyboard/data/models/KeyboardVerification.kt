package com.touchkeyboard.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keyboard_verifications")
data class KeyboardVerification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val isSuccessful: Boolean,
    val unlockDuration: Int = 0, // Duration in minutes for which apps are unlocked
    val date: String // Date in YYYY-MM-DD format
) 
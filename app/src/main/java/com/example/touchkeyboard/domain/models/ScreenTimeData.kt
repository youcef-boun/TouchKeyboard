package com.example.touchkeyboard.domain.models

import java.time.Duration

data class AppScreenTime(
    val appName: String,
    val packageName: String,
    val duration: Duration
)

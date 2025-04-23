package com.touchkeyboard.domain.repositories

import com.touchkeyboard.domain.models.KeyboardVerification
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for keyboard verification operations
 */
interface IKeyboardVerificationRepository {
    fun getAllVerifications(): Flow<List<KeyboardVerification>>
    fun getVerificationsByDate(date: LocalDate): Flow<List<KeyboardVerification>>
    fun getVerificationCountForDate(date: LocalDate): Flow<Int>
    fun getVerificationCountForWeek(): Flow<Int>
    suspend fun recordVerification(isSuccessful: Boolean, unlockDuration: Int = 0)
    suspend fun cleanUpOldData()
} 
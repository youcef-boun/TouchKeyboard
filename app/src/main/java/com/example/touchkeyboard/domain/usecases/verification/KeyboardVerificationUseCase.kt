package com.touchkeyboard.domain.usecases.verification

import com.touchkeyboard.domain.models.KeyboardVerification
import com.touchkeyboard.domain.repositories.IBlockedAppsRepository
import com.touchkeyboard.domain.repositories.IKeyboardVerificationRepository
import com.touchkeyboard.domain.repositories.IUserSettingsRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for handling keyboard verification
 */
class KeyboardVerificationUseCase @Inject constructor(
    private val keyboardVerificationRepository: IKeyboardVerificationRepository,
    private val blockedAppsRepository: IBlockedAppsRepository,
    private val userSettingsRepository: IUserSettingsRepository
) {
    /**
     * Get all verification records
     */
    fun getAllVerifications(): Flow<List<KeyboardVerification>> {
        return keyboardVerificationRepository.getAllVerifications()
    }
    
    /**
     * Get verification records for a specific date
     */
    fun getVerificationsByDate(date: LocalDate): Flow<List<KeyboardVerification>> {
        return keyboardVerificationRepository.getVerificationsByDate(date)
    }
    
    /**
     * Get verification count for a specific date
     */
    fun getVerificationCountForDate(date: LocalDate): Flow<Int> {
        return keyboardVerificationRepository.getVerificationCountForDate(date)
    }
    
    /**
     * Get verification count for the current week
     */
    fun getVerificationCountForWeek(): Flow<Int> {
        return keyboardVerificationRepository.getVerificationCountForWeek()
    }
    
    /**
     * Record a new verification and handle related actions
     */
    suspend fun performVerification(isSuccessful: Boolean, unlockDuration: Int = 30): VerificationResult {
        android.util.Log.d("KeyboardVerificationUseCase", "performVerification called with isSuccessful=$isSuccessful, unlockDuration=$unlockDuration")
        // Record the verification
        keyboardVerificationRepository.recordVerification(isSuccessful, unlockDuration)
        
        if (isSuccessful) {
            // Increment keyboard touch count
            userSettingsRepository.incrementKeyboardTouchCount()
            
            android.util.Log.d("KeyboardVerificationUseCase", "Returning Success($unlockDuration)")
            return VerificationResult.Success(unlockDuration)
        }
        
        android.util.Log.d("KeyboardVerificationUseCase", "Returning Failure")
        return VerificationResult.Failure("Verification failed")
    }
    
    /**
     * Skip verification by using a remaining skip
     */
    suspend fun skipVerification(unlockDuration: Int = 30): SkipResult {
        // Get current user settings to check remaining skips
        val userSettings = userSettingsRepository.getUserSettings()
        
        // Try to decrement remaining skips
        userSettingsRepository.decrementRemainingSkips()
        
        return SkipResult.Success(unlockDuration)
    }
}

/**
 * Sealed class for verification result
 */
sealed class VerificationResult {
    data class Success(val unlockDuration: Int) : VerificationResult()
    data class Failure(val errorMessage: String) : VerificationResult()
}

/**
 * Sealed class for skip result
 */
sealed class SkipResult {
    data class Success(val unlockDuration: Int) : SkipResult()
    data class NoSkipsRemaining(val errorMessage: String) : SkipResult()
} 
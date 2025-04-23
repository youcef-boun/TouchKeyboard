package com.touchkeyboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.touchkeyboard.data.models.KeyboardVerification
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyboardVerificationDao {
    
    @Query("SELECT * FROM keyboard_verifications ORDER BY timestamp DESC")
    fun getAllVerifications(): Flow<List<KeyboardVerification>>
    
    @Query("SELECT * FROM keyboard_verifications WHERE date = :date ORDER BY timestamp DESC")
    fun getVerificationsByDate(date: String): Flow<List<KeyboardVerification>>
    
    @Query("SELECT COUNT(*) FROM keyboard_verifications WHERE date = :date AND isSuccessful = 1")
    fun getSuccessfulVerificationCountForDate(date: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM keyboard_verifications WHERE date BETWEEN :startDate AND :endDate AND isSuccessful = 1")
    fun getSuccessfulVerificationCountForDateRange(startDate: String, endDate: String): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(keyboardVerification: KeyboardVerification)
    
    @Query("DELETE FROM keyboard_verifications WHERE date < :date")
    suspend fun deleteOldVerifications(date: String)
} 
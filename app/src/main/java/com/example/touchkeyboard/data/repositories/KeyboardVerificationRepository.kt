package com.touchkeyboard.data.repositories

import com.touchkeyboard.data.local.dao.KeyboardVerificationDao
import com.touchkeyboard.data.models.KeyboardVerification as DataKeyboardVerification
import com.touchkeyboard.domain.models.KeyboardVerification as DomainKeyboardVerification
import com.touchkeyboard.domain.repositories.IKeyboardVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyboardVerificationRepository @Inject constructor(
    private val keyboardVerificationDao: KeyboardVerificationDao
) : IKeyboardVerificationRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun getAllVerifications(): Flow<List<DomainKeyboardVerification>> {
        return keyboardVerificationDao.getAllVerifications().map { list ->
            list.map { mapToDomainModel(it) }
        }
    }

    override fun getVerificationsByDate(date: LocalDate): Flow<List<DomainKeyboardVerification>> {
        val dateString = date.format(dateFormatter)
        return keyboardVerificationDao.getVerificationsByDate(dateString).map { list ->
            list.map { mapToDomainModel(it) }
        }
    }

    override fun getVerificationCountForDate(date: LocalDate): Flow<Int> {
        val dateString = date.format(dateFormatter)
        return keyboardVerificationDao.getSuccessfulVerificationCountForDate(dateString)
    }

    override fun getVerificationCountForWeek(): Flow<Int> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val startDateString = startOfWeek.format(dateFormatter)
        val endDateString = today.format(dateFormatter)
        
        return keyboardVerificationDao.getSuccessfulVerificationCountForDateRange(startDateString, endDateString)
    }

    override suspend fun recordVerification(isSuccessful: Boolean, unlockDuration: Int) {
        val now = System.currentTimeMillis()
        val today = LocalDate.now().format(dateFormatter)
        
        val verification = DataKeyboardVerification(
            timestamp = now,
            isSuccessful = isSuccessful,
            unlockDuration = unlockDuration,
            date = today
        )
        
        keyboardVerificationDao.insertVerification(verification)
    }

    override suspend fun cleanUpOldData() {
        // Keep data for the last 30 days
        val cutoffDate = LocalDate.now().minusDays(30).format(dateFormatter)
        keyboardVerificationDao.deleteOldVerifications(cutoffDate)
    }

    private fun mapToDomainModel(dataModel: DataKeyboardVerification): DomainKeyboardVerification {
        val instant = Instant.ofEpochMilli(dataModel.timestamp)
        val timestamp = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val date = LocalDate.parse(dataModel.date, dateFormatter)
        
        return DomainKeyboardVerification(
            id = dataModel.id,
            timestamp = timestamp,
            isSuccessful = dataModel.isSuccessful,
            unlockDuration = dataModel.unlockDuration,
            date = date
        )
    }
} 
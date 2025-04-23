package com.touchkeyboard.data.repositories


import com.example.touchkeyboard.data.models.AppUsage
import com.touchkeyboard.data.local.dao.ScreenTimeDao
import com.touchkeyboard.data.models.ScreenTimeSession as DataScreenTimeSession
import com.example.touchkeyboard.domain.models.AppScreenTime
import com.touchkeyboard.domain.models.ScreenTimeSession as DomainScreenTimeSession
import com.touchkeyboard.domain.models.ScreenTimeStats
import com.touchkeyboard.domain.repositories.IScreenTimeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton





@Singleton
class ScreenTimeRepository @Inject constructor(
    private val screenTimeDao: ScreenTimeDao
) : IScreenTimeRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getAllScreenTimeSessions(): Flow<List<DomainScreenTimeSession>> {
        return screenTimeDao.getAllScreenTimeSessions().map { list ->
            list.map { mapToDomainModel(it) }
        }
    }

   fun getScreenTimeSessionsByDate(date: LocalDate): Flow<List<DomainScreenTimeSession>> {
        val dateString = date.format(dateFormatter)
        return screenTimeDao.getScreenTimeSessionsByDate(dateString).map { list ->
            list.map { mapToDomainModel(it) }
        }
    }

    fun getTotalScreenTimeForDate(date: LocalDate): Flow<Long> {
        val dateString = date.format(dateFormatter)
        return screenTimeDao.getTotalScreenTimeForDate(dateString)
    }

     fun getTotalScreenTimeForWeek(): Flow<Long> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val startDateString = startOfWeek.format(dateFormatter)
        val endDateString = today.format(dateFormatter)

        return screenTimeDao.getTotalScreenTimeForDateRange(startDateString, endDateString)
    }

    override fun getScreenTimeForDate(date: LocalDate): Flow<Duration> {
        val dateString = date.format(dateFormatter)
        return screenTimeDao.getTotalScreenTimeForDate(dateString)
            .map { milliseconds -> Duration.ofMillis(milliseconds) }
    }

     fun getDailyScreenTimeStats(days: Int): Flow<List<ScreenTimeStats>> {
        val today = LocalDate.now()

        // Create a flow that will calculate and emit the stats
        return flow {
            val stats = mutableListOf<ScreenTimeStats>()

            for (i in 0 until days) {
                val date = today.minusDays(i.toLong())
                val dateString = date.format(dateFormatter)

                // Get the screen time value by collecting the first value from the flow
                val screenTime = screenTimeDao.getTotalScreenTimeForDate(dateString).first()

                // Target is 4 hours (240 minutes)
                val targetAchieved = screenTime <= 4 * 60 * 60 * 1000

                stats.add(
                    ScreenTimeStats(
                        date = date,
                        totalScreenTime = screenTime,
                        targetAchieved = targetAchieved
                    )
                )
            }

            emit(stats.reversed())
        }
    }

     suspend fun recordScreenTimeSession(startTime: Long, endTime: Long) {
        val duration = endTime - startTime
        val date = LocalDate.now().format(dateFormatter)

        val session = DataScreenTimeSession(
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            date = date
        )

        screenTimeDao.insertScreenTimeSession(session)
    }

     suspend fun cleanUpOldData() {
        // Keep data for the last 30 days
        val cutoffDate = LocalDate.now().minusDays(30).format(dateFormatter)
        screenTimeDao.deleteOldScreenTimeSessions(cutoffDate)
    }

    override fun getAppUsageForDate(date: LocalDate): Flow<List<AppScreenTime>> {
        val dateString = date.format(dateFormatter)
        return screenTimeDao.getAppUsageForDate(dateString)
            .map { appUsages ->
                appUsages.map { appUsage ->
                    AppScreenTime(
                        packageName = appUsage.packageName,
                        appName = appUsage.appName,
                        duration = Duration.ofMillis(appUsage.durationMillis)
                    )
                }
            }
    }

    override suspend fun recordAppUsage(packageName: String, appName: String, duration: Duration) {
        val dateString = LocalDate.now().format(dateFormatter)
        val appUsage = AppUsage(
            packageName = packageName,
            appName = appName,
            durationMillis = duration.toMillis(),
            date = dateString
        )
        screenTimeDao.insertAppUsage(appUsage)
    }

    private fun mapToDomainModel(dataModel: DataScreenTimeSession): DomainScreenTimeSession {
        val startInstant = Instant.ofEpochMilli(dataModel.startTime)
        val endInstant = Instant.ofEpochMilli(dataModel.endTime)

        val startDateTime = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault())
        val endDateTime = LocalDateTime.ofInstant(endInstant, ZoneId.systemDefault())
        val date = LocalDate.parse(dataModel.date, dateFormatter)

        return DomainScreenTimeSession(
            id = dataModel.id,
            startTime = startDateTime,
            endTime = endDateTime,
            duration = dataModel.duration,
            date = date
        )
    }
}
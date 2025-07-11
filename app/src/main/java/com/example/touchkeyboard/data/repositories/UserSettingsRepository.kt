package com.touchkeyboard.data.repositories

import com.example.touchkeyboard.ui.screens.onboarding.UserGoal
import com.touchkeyboard.data.local.dao.UserSettingsDao
import com.touchkeyboard.data.models.UserSettings as DataUserSettings
import com.touchkeyboard.domain.models.UserSettings as DomainUserSettings
import com.touchkeyboard.domain.repositories.IUserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepository @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) : IUserSettingsRepository {

    override fun getUserSettings(): Flow<DomainUserSettings> {
        return userSettingsDao.getUserSettings().map { dataModel ->
            mapToDomainModel(dataModel)
        }
    }

    override suspend fun saveUserSettings(userSettings: DomainUserSettings) {
        userSettingsDao.ensureAndUpdateUserSettings(mapToDataModel(userSettings))
    }

    override suspend fun updateUserGoal(goal: UserGoal) {
        if (!userSettingsDao.doesSettingsRowExist()) {
            userSettingsDao.insertUserSettings(
                DataUserSettings(
                    id = 1,
                    goal = goal,
                    ageRange = "25-34",
                    dailyScreenTimeTarget = 240,
                    keyboardTouchCount = 0,
                    isSubscriptionActive = false,
                    remainingSkips = 3
                )
            )
        } else {
            userSettingsDao.updateUserGoal(goal.name)
        }
    }

    override suspend fun updateAgeRange(ageRange: String) {
        if (!userSettingsDao.doesSettingsRowExist()) {
            userSettingsDao.insertUserSettings(
                DataUserSettings(
                    id = 1,
                    goal = UserGoal.BE_PRESENT,
                    ageRange = ageRange,
                    dailyScreenTimeTarget = 240,
                    keyboardTouchCount = 0,
                    isSubscriptionActive = false,
                    remainingSkips = 3
                )
            )
        } else {
            userSettingsDao.updateAgeRange(ageRange)
        }
    }

    override suspend fun incrementKeyboardTouchCount() {
        userSettingsDao.incrementKeyboardTouchCount()
    }

    override suspend fun decrementRemainingSkips() {
        userSettingsDao.decrementRemainingSkips()
    }

    override fun getKeyboardTouchCount(): Flow<Int> {
        return userSettingsDao.getKeyboardTouchCount()
    }

    private fun mapToDomainModel(dataModel: DataUserSettings?): DomainUserSettings {
        return if (dataModel != null) {
            DomainUserSettings(
                goal = dataModel.goal,
                ageRange = dataModel.ageRange,
                dailyScreenTimeTarget = dataModel.dailyScreenTimeTarget,
                keyboardTouchCount = dataModel.keyboardTouchCount,
                isSubscriptionActive = dataModel.isSubscriptionActive,
                remainingSkips = dataModel.remainingSkips
            )
        } else {
            DomainUserSettings(
                goal = UserGoal.BE_PRESENT,
                ageRange = "25-34", // Aligned with UserSettings
                dailyScreenTimeTarget = 240,
                keyboardTouchCount = 0,
                isSubscriptionActive = false,
                remainingSkips = 3
            )
        }
    }

    private fun mapToDataModel(domainModel: DomainUserSettings): DataUserSettings {
        return DataUserSettings(
            id = 1,
            goal = domainModel.goal,
            ageRange = domainModel.ageRange,
            dailyScreenTimeTarget = domainModel.dailyScreenTimeTarget,
            keyboardTouchCount = domainModel.keyboardTouchCount,
            isSubscriptionActive = domainModel.isSubscriptionActive,
            remainingSkips = domainModel.remainingSkips
        )
    }
}
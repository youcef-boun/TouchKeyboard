package com.touchkeyboard.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touchkeyboard.ui.screens.onboarding.UserGoal
import com.touchkeyboard.domain.usecases.verification.KeyboardVerificationUseCase
import com.touchkeyboard.domain.repositories.IScreenTimeRepository
import com.touchkeyboard.domain.repositories.IUserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Duration
import javax.inject.Inject
import com.touchkeyboard.domain.models.UserSettings
import kotlinx.coroutines.flow.asStateFlow


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userSettingsRepository: IUserSettingsRepository,
    private val screenTimeRepository: IScreenTimeRepository,
    private val keyboardVerificationUseCase: KeyboardVerificationUseCase


) : ViewModel() {

    private val _profileState = MutableStateFlow(ProfileUiState())
    val profileState: StateFlow<ProfileUiState> = _profileState


    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            combine(
                userSettingsRepository.getUserSettings(),
                screenTimeRepository.getScreenTimeForDate(LocalDate.now()),
                keyboardVerificationUseCase.getVerificationCountForWeek()
            ) { settings: UserSettings, screenTimeToday: Duration, keyboardTouchesWeek: Int ->
                val formattedScreenTime = formatScreenTime(screenTimeToday)

                ProfileUiState(
                    userGoal = settings.goal,
                    ageRange = settings.ageRange,
                    screenTimeAverage = formattedScreenTime,
                    keyboardTouchCount = "${keyboardTouchesWeek} times this week",
                    subscriptionTier = if (settings.isSubscriptionActive) "Premium" else "Free tier",
                    remainingSkips = "${settings.remainingSkips} remaining today",
                    isLoading = false
                )
            }.collect { state ->
                _profileState.value = state
            }
        }
    }

    fun updateUserGoal(goal: UserGoal) {
        viewModelScope.launch {
            userSettingsRepository.updateUserGoal(goal)
        }
    }

    private fun formatScreenTime(duration: Duration): String {
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "$hours h $minutes min"
            else -> "$minutes min"
        }
    }
}

data class ProfileUiState(
    val userGoal: UserGoal = UserGoal.BE_PRESENT,
    val ageRange: String = "",
    val screenTimeAverage: String = "",
    val keyboardTouchCount: String = "",
    val subscriptionTier: String = "",
    val remainingSkips: String = "",
    val isLoading: Boolean = true
)
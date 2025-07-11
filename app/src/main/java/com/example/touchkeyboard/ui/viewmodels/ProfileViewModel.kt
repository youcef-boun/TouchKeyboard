package com.touchkeyboard.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touchkeyboard.ui.screens.onboarding.UserGoal
import com.example.touchkeyboard.ui.viewmodels.HomeViewModel
import com.touchkeyboard.domain.usecases.verification.KeyboardVerificationUseCase
import com.touchkeyboard.domain.repositories.IUserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.touchkeyboard.domain.models.UserSettings
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userSettingsRepository: IUserSettingsRepository,
    private val keyboardVerificationUseCase: KeyboardVerificationUseCase
) : ViewModel() {

    private val _profileState = MutableStateFlow(ProfileUiState())
    val profileState: StateFlow<ProfileUiState> = _profileState



    init {
        ensureUserSettingsRowExists()
        loadProfileData()
    }

    private fun ensureUserSettingsRowExists() {
        viewModelScope.launch {
            val settings = userSettingsRepository.getUserSettings().firstOrNull()
            if (settings == null) {
                userSettingsRepository.saveUserSettings(
                    UserSettings(
                        goal = UserGoal.BE_PRESENT,
                        ageRange = "25-34",
                        dailyScreenTimeTarget = 240,
                        keyboardTouchCount = 0,
                        isSubscriptionActive = false,
                        remainingSkips = 3
                    )
                )
            }
        }
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            userSettingsRepository.getUserSettings().collect { settings ->
                _profileState.value = _profileState.value.copy(
                    userGoal = settings.goal,
                    ageRange = settings.ageRange,
                    subscriptionTier = if (settings.isSubscriptionActive) "Premium" else "Free tier",
                    remainingSkips = "${settings.remainingSkips} remaining today",
                    isLoading = false
                )
            }
        }
    }

    fun updateUserGoal(goal: UserGoal) {
        viewModelScope.launch {
            userSettingsRepository.updateUserGoal(goal)
        }
    }

    fun updateAgeRange(ageRange: String) {
        viewModelScope.launch {
            userSettingsRepository.updateAgeRange(ageRange)
        }
    }

    fun updateScreenTimeFromHome(homeViewModel: HomeViewModel) {
        val millis = homeViewModel.screenTimeState.value
        _profileState.value = _profileState.value.copy(
            screenTimeAverage = formatScreenTime(millis)
        )
    }

    private fun formatScreenTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        return if (hours > 0) "$hours h $minutes min" else "$minutes min"
    }
}

data class ProfileUiState(
    val userGoal: UserGoal = UserGoal.BE_PRESENT,
    val ageRange: String = "25-34", // Aligned with UserSettings
    val screenTimeAverage: String = "",
    val keyboardTouchCount: String = "",
    val subscriptionTier: String = "",
    val remainingSkips: String = "",
    val isLoading: Boolean = true
)


private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return when {
        hours > 0 -> "$hours h $minutes min"
        else -> "$minutes min"
    }
}
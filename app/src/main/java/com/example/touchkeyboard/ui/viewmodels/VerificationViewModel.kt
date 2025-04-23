package com.touchkeyboard.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touchkeyboard.domain.repositories.IBlockListRepository


import com.touchkeyboard.domain.usecases.blockedapps.GetBlockedAppsUseCase
import com.touchkeyboard.domain.usecases.blockedapps.ManageBlockedAppsUseCase
import com.touchkeyboard.domain.usecases.verification.KeyboardVerificationUseCase
import com.touchkeyboard.domain.usecases.verification.SkipResult
import com.touchkeyboard.domain.usecases.verification.VerificationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val keyboardVerificationUseCase: KeyboardVerificationUseCase,
    private val getBlockedAppsUseCase: GetBlockedAppsUseCase,
    private val manageBlockedAppsUseCase: ManageBlockedAppsUseCase
) : ViewModel() {

    private val _verificationState = MutableStateFlow(VerificationUiState())
    val verificationState: StateFlow<VerificationUiState> = _verificationState

    fun startVerification() {
        viewModelScope.launch {
            _verificationState.value = _verificationState.value.copy(isVerifying = true)

            // In a real app, this would use camera and ML to verify keyboard touch
            // For now, we'll simulate the verification process with a delay
            delay(2000)

            // Perform the verification
            when (val result = keyboardVerificationUseCase.performVerification(true, DEFAULT_UNLOCK_DURATION)) {
                is VerificationResult.Success -> {
                    // Get blocked apps and unblock them
                    val blockedApps = getBlockedAppsUseCase.getAllBlockedApps().first()
                    val packageNames = blockedApps.map { it.packageName }
                    manageBlockedAppsUseCase.unblockAppsTemporarily(packageNames, result.unlockDuration)

                    _verificationState.value = _verificationState.value.copy(
                        isVerifying = false,
                        isVerified = true,
                        errorMessage = null
                    )
                }
                is VerificationResult.Failure -> {
                    _verificationState.value = _verificationState.value.copy(
                        isVerifying = false,
                        isVerified = false,
                        errorMessage = result.errorMessage
                    )
                }

                else -> {}
            }
        }
    }

    fun resetState() {
        _verificationState.value = VerificationUiState()
    }

    fun skipVerification() {
        viewModelScope.launch {
            when (val result = keyboardVerificationUseCase.skipVerification(DEFAULT_UNLOCK_DURATION)) {
                is SkipResult.Success -> {
                    // Get blocked apps and unblock them
                    val blockedApps = getBlockedAppsUseCase.getAllBlockedApps().first()
                    val packageNames = blockedApps.map { it.packageName }
                    manageBlockedAppsUseCase.unblockAppsTemporarily(packageNames, result.unlockDuration)

                    _verificationState.value = _verificationState.value.copy(
                        isVerified = true,
                        wasSkipped = true
                    )
                }
                is SkipResult.NoSkipsRemaining -> {
                    _verificationState.value = _verificationState.value.copy(
                        errorMessage = "No skips remaining for today."
                    )
                }

                else -> {}
            }
        }
    }

    companion object {
        private const val DEFAULT_UNLOCK_DURATION = 30 // minutes
    }
}

data class VerificationUiState(
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
    val wasSkipped: Boolean = false,
    val errorMessage: String? = null
) 
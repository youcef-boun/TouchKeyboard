package com.touchkeyboard.ui.viewmodels

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import android.util.Log
import android.app.Application
import java.util.Calendar

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val keyboardVerificationUseCase: KeyboardVerificationUseCase,
    private val getBlockedAppsUseCase: GetBlockedAppsUseCase,
    private val manageBlockedAppsUseCase: ManageBlockedAppsUseCase,
    private val application: Application
) : AndroidViewModel(application) {

    private val TAG = "VerificationViewModel"

    private val _verificationState = MutableStateFlow(VerificationUiState())
    val verificationState: StateFlow<VerificationUiState> = _verificationState


    fun startVerification(durationMs: Long) {
        Log.d(TAG, "startVerification called with durationMs=$durationMs")
        viewModelScope.launch {
            _verificationState.value = _verificationState.value.copy(isVerifying = true)

            // In a real app, this would use camera and ML to verify keyboard touch
            // For now, we'll simulate the verification process with a delay
            delay(2000)

            // Convert ms to minutes for legacy use case, but pass ms to downstream
            val durationMinutes = (durationMs / 60000).toInt().coerceAtLeast(1)
            Log.d(TAG, "Calling performVerification with durationMinutes=$durationMinutes")
            val result = keyboardVerificationUseCase.performVerification(true, durationMinutes)
            Log.d(TAG, "performVerification returned: $result")
            when (result) {
                is VerificationResult.Success -> {
                    val packageNames = getBlockedAppsUseCase.getAllBlockedApps().first()
                        .map { it.packageName }
                    Log.d(TAG, "Calling unblockAppsTemporarilyMs for $packageNames with durationMs=$durationMs")
                    manageBlockedAppsUseCase.unblockAppsTemporarilyMs(packageNames, durationMs)
                    Log.d(TAG, "Unblocked apps: $packageNames for ${durationMs} ms.")

                    _verificationState.value = _verificationState.value.copy(
                        isVerifying = false,
                        isVerified = true,
                        errorMessage = null
                    )
                }
                is VerificationResult.Failure -> {
                    Log.d(TAG, "VerificationResult.Failure: ${result.errorMessage}")
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

    fun setVerifying(isVerifying: Boolean) {
        _verificationState.value = _verificationState.value.copy(isVerifying = isVerifying)
    }

    fun resetState() {
        _verificationState.value = VerificationUiState()
    }

    fun skipVerification() {
        viewModelScope.launch {
            when (val result = keyboardVerificationUseCase.skipVerification(DEFAULT_UNLOCK_DURATION)) {
                is SkipResult.Success -> {
                    val packageNames = getBlockedAppsUseCase.getAllBlockedApps().first()
                        .map { it.packageName }
                    manageBlockedAppsUseCase.unblockAppsTemporarily(packageNames, result.unlockDuration)
                    Log.d(TAG, "Unblocked apps (skip): $packageNames for ${result.unlockDuration} min.")

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



    // Call this after re-block time to force UI to reload from storage
    fun reloadBlockedAppsFromStorage() {
        val repo = com.example.touchkeyboard.data.repositories.BlockListRepositoryProvider.get(application)
        repo.reloadBlockedApps()
    }

    fun forceReloadRoomDb() {
        com.touchkeyboard.data.local.AppDatabase.closeInstance()
        // Next access to getInstance() will reopen and reload from disk
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
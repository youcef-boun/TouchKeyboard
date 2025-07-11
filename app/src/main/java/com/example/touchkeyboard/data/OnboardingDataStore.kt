package com.example.touchkeyboard.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

object OnboardingPrefsKeys {
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
}

class OnboardingDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val onboardingComplete: Flow<Boolean> = context.onboardingDataStore.data
        .map { prefs -> prefs[OnboardingPrefsKeys.ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[OnboardingPrefsKeys.ONBOARDING_COMPLETE] = value
        }
    }
}

package com.touchkeyboard.di

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.example.touchkeyboard.utils.PermissionManager
import com.touchkeyboard.data.local.AppDatabase
import com.touchkeyboard.data.local.dao.BlockedAppsDao
import com.touchkeyboard.data.local.dao.KeyboardVerificationDao
import com.touchkeyboard.data.local.dao.ScreenTimeDao
import com.touchkeyboard.data.local.dao.UserSettingsDao
import com.touchkeyboard.data.repositories.BlockedAppsRepository
import com.touchkeyboard.data.repositories.KeyboardVerificationRepository
import com.touchkeyboard.data.repositories.ScreenTimeRepository
import com.touchkeyboard.data.repositories.UserSettingsRepository
import com.touchkeyboard.domain.repositories.IBlockedAppsRepository
import com.touchkeyboard.domain.repositories.IKeyboardVerificationRepository
import com.touchkeyboard.domain.repositories.IScreenTimeRepository
import com.touchkeyboard.domain.repositories.IUserSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "block_list")
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Context

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    // System Services

    @Provides
    @Singleton
    fun providePackageManager(@ApplicationContext context: Context): PackageManager {
        return context.packageManager
    }

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    // DataStore

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.settingsDataStore

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    // Database and DAOs

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "blocked_apps.db"
    ).build()

    @Provides
    fun provideUserSettingsDao(database: AppDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }

    @Provides
    @Singleton
    fun provideBlockedAppsDao(database: AppDatabase): BlockedAppsDao = database.blockedAppsDao()

    @Provides
    fun provideScreenTimeDao(database: AppDatabase): ScreenTimeDao {
        return database.screenTimeDao()
    }

    @Provides
    fun provideKeyboardVerificationDao(database: AppDatabase): KeyboardVerificationDao {
        return database.keyboardVerificationDao()
    }

    // Repositories

    @Provides
    @Singleton
    fun provideUserSettingsRepository(userSettingsDao: UserSettingsDao): IUserSettingsRepository {
        return UserSettingsRepository(userSettingsDao)
    }

    @Provides
    @Singleton
    fun provideBlockedAppsRepository(blockedAppsDao: BlockedAppsDao): IBlockedAppsRepository {
        return BlockedAppsRepository(blockedAppsDao)
    }

    @Provides
    @Singleton
    fun provideScreenTimeRepository(screenTimeDao: ScreenTimeDao): IScreenTimeRepository {
        return ScreenTimeRepository(screenTimeDao)
    }

    @Provides
    @Singleton
    fun provideKeyboardVerificationRepository(keyboardVerificationDao: KeyboardVerificationDao): IKeyboardVerificationRepository {
        return KeyboardVerificationRepository(keyboardVerificationDao)
    }
}
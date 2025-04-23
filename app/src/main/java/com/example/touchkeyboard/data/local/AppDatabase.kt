package com.touchkeyboard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.touchkeyboard.data.models.AppUsage
import com.touchkeyboard.data.models.BlockedApp
import com.touchkeyboard.data.models.KeyboardVerification
import com.touchkeyboard.data.models.ScreenTimeSession
import com.touchkeyboard.data.models.UserSettings
import com.touchkeyboard.data.local.dao.BlockedAppsDao
import com.touchkeyboard.data.local.dao.KeyboardVerificationDao
import com.touchkeyboard.data.local.dao.ScreenTimeDao
import com.touchkeyboard.data.local.dao.UserSettingsDao
import com.touchkeyboard.data.local.converters.Converters

@Database(
    entities = [
        UserSettings::class,
        BlockedApp::class,
        ScreenTimeSession::class,
        KeyboardVerification::class,
        AppUsage::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun blockedAppsDao(): BlockedAppsDao
    abstract fun screenTimeDao(): ScreenTimeDao
    abstract fun keyboardVerificationDao(): KeyboardVerificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new app_usage table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_usage (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        package_name TEXT NOT NULL,
                        app_name TEXT NOT NULL,
                        usage_time INTEGER NOT NULL,
                        date TEXT NOT NULL
                    )
                """)
            }
        }
    }
}

package com.example.touchkeyboard.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.example.touchkeyboard.data.repositories.BlockListRepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.touchkeyboard.data.local.AppDatabase
import com.touchkeyboard.data.local.dao.BlockedAppsDao
import kotlinx.coroutines.flow.firstOrNull


class ReblockAppsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        android.util.Log.d("ReblockAppsWorker", "Worker started")
        val packageNames = inputData.getStringArray(KEY_PACKAGE_NAMES)?.toList() ?: emptyList()
        android.util.Log.d("ReblockAppsWorker", "Re-blocking apps: $packageNames")
        try {
            val blockedAppsDao = AppDatabase.getInstance(applicationContext).blockedAppsDao()
            val allApps = blockedAppsDao.getAllBlockedApps().firstOrNull() ?: emptyList()
            for (pkg in packageNames) {
                val app = allApps.find { it.packageName == pkg }
                if (app != null) {
                    val updatedApp = app.copy(isCurrentlyBlocked = true, blockEndTime = 0)
                    blockedAppsDao.insertBlockedApp(updatedApp)
                    android.util.Log.d("ReblockAppsWorker", "Re-blocked $pkg in DB")
                }
            }
            // Ensure the AppBlockingService is running and reload its state
            try {
                AppBlockingService.startService(applicationContext)
                android.util.Log.d("ReblockAppsWorker", "AppBlockingService.startService called")
            } catch (e: Exception) {
                android.util.Log.e("ReblockAppsWorker", "Error calling AppBlockingService.startService", e)
            }
            try {
                AppBlockingService.reloadService(applicationContext)
                android.util.Log.d("ReblockAppsWorker", "AppBlockingService.reloadService called")
            } catch (e: Exception) {
                android.util.Log.e("ReblockAppsWorker", "Error calling AppBlockingService.reloadService", e)
            }
            // Hard reset: stop and start the service
            try {
                AppBlockingService.stopService(applicationContext)
                android.util.Log.d("ReblockAppsWorker", "AppBlockingService.stopService called")
                Thread.sleep(500)
                AppBlockingService.startService(applicationContext)
                android.util.Log.d("ReblockAppsWorker", "AppBlockingService.startService (hard reset) called")
            } catch (e: Exception) {
                android.util.Log.e("ReblockAppsWorker", "Error hard resetting AppBlockingService", e)
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ReblockAppsWorker", "Exception in doWork", e)
            Result.failure()
        }
    }

    companion object {
        const val KEY_PACKAGE_NAMES = "package_names"
    }
}
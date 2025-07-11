package com.example.touchkeyboard.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.example.touchkeyboard.data.repositories.BlockListRepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log





class ReblockAppsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        android.util.Log.d("ReblockAppsWorker", "Worker started")
        val packageNames = inputData.getStringArray(KEY_PACKAGE_NAMES)?.toList() ?: emptyList()
        android.util.Log.d("ReblockAppsWorker", "Re-blocking apps: $packageNames")
        try {
            val repo = BlockListRepositoryProvider.get(applicationContext)
            val currentApps = repo.getBlockedApps()
            val updatedApps = currentApps.map { app ->
                if (packageNames.contains(app.packageName)) {
                    app.copy(isCurrentlyBlocked = true, blockEndTime = 0)
                } else app
            }

            // Call the method directly instead of using reflection
            repo.saveBlockedApps(updatedApps)

            android.util.Log.d("ReblockAppsWorker", "Blocked state updated for: $packageNames")
            // Notify the AppBlockingService to reload its blocked apps list
            AppBlockingService.reloadService(applicationContext)
            android.util.Log.d("ReblockAppsWorker", "AppBlockingService.reloadService called")
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
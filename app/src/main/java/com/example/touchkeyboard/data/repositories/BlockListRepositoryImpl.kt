package com.example.touchkeyboard.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.touchkeyboard.domain.repositories.IBlockListRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import com.touchkeyboard.data.local.AppDatabase
import com.touchkeyboard.data.local.dao.BlockedAppsDao
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.example.touchkeyboard.services.AppBlockingService
import com.touchkeyboard.data.models.BlockedApp as DataBlockedApp
import com.touchkeyboard.domain.models.BlockedApp as DomainBlockedApp
import kotlinx.coroutines.flow.map


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "block_list")

// Mapping functions
private fun dataToDomain(app: DataBlockedApp): DomainBlockedApp =
    DomainBlockedApp(
        packageName = app.packageName,
        appName = app.appName,
        isCurrentlyBlocked = app.isCurrentlyBlocked,
        blockStartTime = app.blockStartTime,
        blockEndTime = app.blockEndTime,
        isBlockedIndefinitely = app.isBlockedIndefinitely,
        remainingBlockTime = 0 // You can calculate this if needed
    )

private fun domainToData(app: DomainBlockedApp): DataBlockedApp =
    DataBlockedApp(
        packageName = app.packageName,
        appName = app.appName,
        isCurrentlyBlocked = app.isCurrentlyBlocked,
        blockStartTime = app.blockStartTime,
        blockEndTime = app.blockEndTime,
        isBlockedIndefinitely = app.isBlockedIndefinitely
    )

@Singleton
class BlockListRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IBlockListRepository {

    private val TAG = "BlockListRepository"
    private val blockedAppsDao: BlockedAppsDao = AppDatabase.getInstance(context).blockedAppsDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun isAppBlocked(packageName: String): Boolean {
        val app = blockedAppsDao.getAllBlockedApps().firstOrNull()?.find { it.packageName == packageName }
        return app?.isCurrentlyBlocked == true
    }

    override suspend fun getBlockedApps(): List<DomainBlockedApp> {
        return blockedAppsDao.getAllBlockedApps().firstOrNull()?.map { dataToDomain(it) } ?: emptyList()
    }

    override suspend fun addAppToBlockList(packageName: String, appName: String) {
        val app = DataBlockedApp(
            packageName = packageName,
            appName = appName,
            isCurrentlyBlocked = true,
            blockStartTime = System.currentTimeMillis(),
            blockEndTime = 0,
            isBlockedIndefinitely = true
        )
        blockedAppsDao.insertBlockedApp(app)
    }

    override suspend fun removeAppFromBlockList(packageName: String) {
        blockedAppsDao.deleteBlockedAppByPackageName(packageName)
    }

    override suspend fun isBlockListEnabled(): Boolean {
        // Always enabled for now
        return true
    }

    override suspend fun setBlockListEnabled(enabled: Boolean) {
        // No-op for now
    }

    override fun getAllBlockedApps(): Flow<List<DomainBlockedApp>> {
        return blockedAppsDao.getAllBlockedApps().map { list -> list.map { dataToDomain(it) } }
    }

    override suspend fun unblockAppsTemporarily(packageNames: List<String>, durationMinutes: Int) {
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60 * 1000)
        val allApps = blockedAppsDao.getAllBlockedApps().firstOrNull() ?: emptyList()
        for (pkg in packageNames) {
            val app = allApps.find { it.packageName == pkg }
            if (app != null) {
                val updatedApp = app.copy(
                    blockStartTime = now,
                    blockEndTime = endTime,
                    isCurrentlyBlocked = false
                )
                blockedAppsDao.insertBlockedApp(updatedApp)
            }
        }
        AppBlockingService.reloadService(context)
        // Schedule re-block using WorkManager as before
        val workManager = androidx.work.WorkManager.getInstance(context)
        val data = androidx.work.Data.Builder()
            .putStringArray(com.example.touchkeyboard.services.ReblockAppsWorker.KEY_PACKAGE_NAMES, packageNames.toTypedArray())
            .build()
        val delayMillis = durationMinutes * 60 * 1000L
        val request = androidx.work.OneTimeWorkRequestBuilder<com.example.touchkeyboard.services.ReblockAppsWorker>()
            .setInputData(data)
            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueue(request)
    }

    override suspend fun unblockAppsTemporarilyMs(packageNames: List<String>, durationMs: Long) {
        val now = System.currentTimeMillis()
        val endTime = now + durationMs
        val allApps = blockedAppsDao.getAllBlockedApps().firstOrNull() ?: emptyList()
        for (pkg in packageNames) {
            val app = allApps.find { it.packageName == pkg }
            if (app != null) {
                val updatedApp = app.copy(
                    blockStartTime = now,
                    blockEndTime = endTime,
                    isCurrentlyBlocked = false
                )
                blockedAppsDao.insertBlockedApp(updatedApp)
            }
        }
        AppBlockingService.reloadService(context)
        // Schedule re-block using WorkManager as before
        val workManager = androidx.work.WorkManager.getInstance(context)
        val data = androidx.work.Data.Builder()
            .putStringArray(com.example.touchkeyboard.services.ReblockAppsWorker.KEY_PACKAGE_NAMES, packageNames.toTypedArray())
            .build()
        val request = androidx.work.OneTimeWorkRequestBuilder<com.example.touchkeyboard.services.ReblockAppsWorker>()
            .setInputData(data)
            .setInitialDelay(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueue(request)
    }

    override suspend fun isAppTemporarilyUnblocked(packageName: String): Boolean {
        val app = blockedAppsDao.getAllBlockedApps().firstOrNull()?.find { it.packageName == packageName }
        val now = System.currentTimeMillis()
        return app != null && !app.isCurrentlyBlocked && now < app.blockEndTime
    }

    fun reloadBlockedApps() {
        // No-op: always live from DB
    }
}
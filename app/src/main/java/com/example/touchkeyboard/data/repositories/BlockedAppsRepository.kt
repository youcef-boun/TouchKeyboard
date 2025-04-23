package com.touchkeyboard.data.repositories

import com.touchkeyboard.data.local.dao.BlockedAppsDao
import com.touchkeyboard.data.models.BlockedApp as DataBlockedApp
import com.touchkeyboard.domain.models.BlockedApp as DomainBlockedApp
import com.touchkeyboard.domain.repositories.IBlockedAppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedAppsRepository @Inject constructor(
    private val blockedAppsDao: BlockedAppsDao
) : IBlockedAppsRepository {

    override fun getAllBlockedApps(): Flow<List<DomainBlockedApp>> {
        return blockedAppsDao.getAllBlockedApps().map { list ->
            list.map { mapToDomainModel(it) }
        }
    }

    override fun getCurrentlyBlockedApps(): Flow<List<DomainBlockedApp>> {
        return blockedAppsDao.getCurrentlyBlockedApps().map { list ->
            list.map { mapToDomainModel(it) }
        }
    }

    override suspend fun addBlockedApp(packageName: String, appName: String) {
        val blockedApp = DataBlockedApp(
            packageName = packageName,
            appName = appName,
            isCurrentlyBlocked = true,
            blockStartTime = System.currentTimeMillis(),
            blockEndTime = 0,
            isBlockedIndefinitely = true
        )
        
        blockedAppsDao.insertBlockedApp(blockedApp)
    }

    override suspend fun removeBlockedApp(packageName: String) {
        blockedAppsDao.deleteBlockedAppByPackageName(packageName)
    }

    override suspend fun updateBlockStatus(packageName: String, isBlocked: Boolean) {
        blockedAppsDao.updateBlockStatus(packageName, isBlocked)
    }

    override suspend fun unblockAppsTemporarily(packageNames: List<String>, durationMinutes: Int) {
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60 * 1000)
        
        packageNames.forEach { packageName ->
            blockedAppsDao.updateBlockEndTime(packageName, endTime)
            blockedAppsDao.updateBlockStatus(packageName, false)
        }
    }

    override suspend fun checkAndUpdateBlockStatus() {
        val now = System.currentTimeMillis()
        blockedAppsDao.unblockExpiredApps(now)
    }

    private fun mapToDomainModel(dataModel: DataBlockedApp): DomainBlockedApp {
        val now = System.currentTimeMillis()
        val remainingTime = if (dataModel.blockEndTime > now) {
            dataModel.blockEndTime - now
        } else {
            0
        }
        
        return DomainBlockedApp(
            packageName = dataModel.packageName,
            appName = dataModel.appName,
            isCurrentlyBlocked = dataModel.isCurrentlyBlocked,
            blockStartTime = dataModel.blockStartTime,
            blockEndTime = dataModel.blockEndTime,
            isBlockedIndefinitely = dataModel.isBlockedIndefinitely,
            remainingBlockTime = remainingTime
        )
    }
} 
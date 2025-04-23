package com.touchkeyboard.domain.usecases.blockedapps

import com.example.touchkeyboard.domain.repositories.IBlockListRepository
import com.touchkeyboard.domain.repositories.IBlockedAppsRepository
import javax.inject.Inject

/**
 * Use case for managing blocked applications
 */
class ManageBlockedAppsUseCase @Inject constructor(
    private val blockListRepository: IBlockListRepository
) {
    /**
     * Add a new app to the blocked list
     */
    suspend fun addAppToBlockList(packageName: String, appName: String) {
        blockListRepository.addAppToBlockList(packageName, appName)
    }

    /**
     * Remove an app from the blocked list
     */
    suspend fun removeAppFromBlockList(packageName: String) {
        blockListRepository.removeAppFromBlockList(packageName)
    }

    /**
     * Update the block status of an app
     */
    suspend fun updateBlockStatus(packageName: String, appName: String, isBlocked: Boolean) {
        if (isBlocked) {
            blockListRepository.addAppToBlockList(packageName, appName)
        } else {
            blockListRepository.removeAppFromBlockList(packageName)
        }
    }

    /**
     * Temporarily unblock apps for a specified duration
     */
    suspend fun unblockAppsTemporarily(packageNames: List<String>, durationMinutes: Int) {
        blockListRepository.unblockAppsTemporarily(packageNames, durationMinutes)
    }

    /**
     * Check and update block status based on timing rules
     */
    suspend fun checkAndUpdateBlockStatus() {
        // Implementation needed
    }
}
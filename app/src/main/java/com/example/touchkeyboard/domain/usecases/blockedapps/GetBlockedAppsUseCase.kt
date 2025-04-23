package com.touchkeyboard.domain.usecases.blockedapps

import com.example.touchkeyboard.domain.repositories.IBlockListRepository
import com.touchkeyboard.domain.models.BlockedApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


/**
 * Use case for retrieving blocked applications
 */
class GetBlockedAppsUseCase @Inject constructor(
    private val blockListRepository: IBlockListRepository
) {
    /**
     * Get all blocked apps regardless of their current block status
     */
    fun getAllBlockedApps(): Flow<List<BlockedApp>> {
        return blockListRepository.getAllBlockedApps()
    }
}
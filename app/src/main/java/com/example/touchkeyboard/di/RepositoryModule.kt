package com.example.touchkeyboard.di

import com.example.touchkeyboard.data.repositories.BlockListRepositoryImpl
import com.example.touchkeyboard.domain.repositories.IBlockListRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBlockListRepository(
        blockListRepositoryImpl: BlockListRepositoryImpl
    ): IBlockListRepository
}
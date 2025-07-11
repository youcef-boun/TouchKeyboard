package com.example.touchkeyboard.data.repositories

import android.content.Context

object BlockListRepositoryProvider {
    @Volatile
    private var instance: BlockListRepositoryImpl? = null

    fun get(context: Context): BlockListRepositoryImpl {
        return instance ?: synchronized(this) {
            instance ?: BlockListRepositoryImpl(context.applicationContext).also { instance = it }
        }
    }
}

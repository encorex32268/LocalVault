package com.lihan.localvault.core.domain.datastore

import kotlinx.coroutines.flow.Flow


interface AppDataStore {
    val lastUsedColor: Flow<Long?>
    val lastUsedIconIndex: Flow<Int?>
    suspend fun saveLastUsedVaultStyle(color: Long, iconIndex: Int)
}

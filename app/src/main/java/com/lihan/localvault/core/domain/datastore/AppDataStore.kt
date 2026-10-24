package com.lihan.localvault.core.domain.datastore

import kotlinx.coroutines.flow.Flow


interface AppDataStore {

    val isDarkTheme: Flow<Boolean>
    suspend fun setDarkTheme(isDark: Boolean)

    suspend fun toggleTheme()
    val lastUsedColor: Flow<Long?>
    val lastUsedIconIndex: Flow<Int?>

    suspend fun saveLastUsedVaultStyle(color: Long, iconIndex: Int)

    val isLimitCardCollapsed: Flow<Boolean>
    suspend fun setLimitCardCollapsed(isCollapsed: Boolean)
    suspend fun toggleLimitCardCollapsed()
}

package com.lihan.localvault.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lihan.localvault.core.domain.datastore.AppDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class DefaultAppDataStore(
    private val dataStore: DataStore<Preferences>
) : AppDataStore {

    private companion object {
        val KEY_LAST_USED_COLOR = longPreferencesKey("last_used_color")
        val KEY_LAST_USED_ICON_INDEX = intPreferencesKey("last_used_icon_index")
    }

    private val safePreferencesFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val lastUsedColor: Flow<Long?> = safePreferencesFlow
        .map { preferences ->
            preferences[KEY_LAST_USED_COLOR]
        }

    override val lastUsedIconIndex: Flow<Int?> = safePreferencesFlow
        .map { preferences ->
            preferences[KEY_LAST_USED_ICON_INDEX]
        }

    override suspend fun saveLastUsedVaultStyle(color: Long, iconIndex: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_USED_COLOR] = color
            preferences[KEY_LAST_USED_ICON_INDEX] = iconIndex
        }
    }
}

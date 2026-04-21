package com.nigdroid.quantummessenger.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

@Singleton
class SessionManager @Inject constructor(
    private val context: Context
) {
    private val IS_USER_REGISTERED = booleanPreferencesKey("is_user_registered")

    val isUserRegistered: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_USER_REGISTERED] ?: false
        }

    suspend fun setUserRegistered(isRegistered: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_USER_REGISTERED] = isRegistered
        }
    }
}

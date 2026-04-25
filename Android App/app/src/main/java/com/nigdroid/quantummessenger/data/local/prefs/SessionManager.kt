package com.nigdroid.quantummessenger.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    private val IS_USER_REGISTERED  = booleanPreferencesKey("is_user_registered")
    private val TEXT_FINGERPRINT    = stringPreferencesKey("text_fingerprint")

    // ── Reactive flows ─────────────────────────────────────────────────────────

    val isUserRegistered: Flow<Boolean> = context.dataStore.data
        .map { it[IS_USER_REGISTERED] ?: false }

    /**
     * The user's permanent anonymous identity — a 64-char hex SHA-256 fingerprint.
     * Used as the Bearer token for HTTP auth and the fingerprint for Socket.io auth.
     */
    val textFingerprint: Flow<String?> = context.dataStore.data
        .map { it[TEXT_FINGERPRINT] }

    // ── Mutators ───────────────────────────────────────────────────────────────

    suspend fun setUserRegistered(isRegistered: Boolean) {
        context.dataStore.edit { it[IS_USER_REGISTERED] = isRegistered }
    }

    suspend fun setTextFingerprint(fingerprint: String?) {
        context.dataStore.edit { prefs ->
            if (fingerprint != null) {
                prefs[TEXT_FINGERPRINT] = fingerprint
            } else {
                prefs.remove(TEXT_FINGERPRINT)
            }
        }
    }
}

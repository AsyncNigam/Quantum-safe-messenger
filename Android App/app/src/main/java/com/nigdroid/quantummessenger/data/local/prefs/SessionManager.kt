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
    private val DISPLAY_NAME        = stringPreferencesKey("display_name")
    private val ML_KEM_PUBLIC_KEY   = stringPreferencesKey("ml_kem_public_key")
    private val X25519_PUBLIC_KEY   = stringPreferencesKey("x25519_public_key")
    private val FCM_TOKEN           = stringPreferencesKey("fcm_token")

    // ── Reactive flows ─────────────────────────────────────────────────────────

    val isUserRegistered: Flow<Boolean> = context.dataStore.data
        .map { it[IS_USER_REGISTERED] ?: false }

    val textFingerprint: Flow<String?> = context.dataStore.data
        .map { it[TEXT_FINGERPRINT] }

    val displayName: Flow<String?> = context.dataStore.data
        .map { it[DISPLAY_NAME] }

    val mlKemPublicKey: Flow<String?> = context.dataStore.data
        .map { it[ML_KEM_PUBLIC_KEY] }

    val x25519PublicKey: Flow<String?> = context.dataStore.data
        .map { it[X25519_PUBLIC_KEY] }

    /** Firebase Cloud Messaging token for push notifications. */
    val fcmToken: Flow<String?> = context.dataStore.data
        .map { it[FCM_TOKEN] }

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

    suspend fun setDisplayName(name: String?) {
        context.dataStore.edit { prefs ->
            if (name != null) {
                prefs[DISPLAY_NAME] = name
            } else {
                prefs.remove(DISPLAY_NAME)
            }
        }
    }

    suspend fun setPublicKeys(mlKemBase64: String, x25519Base64: String) {
        context.dataStore.edit { prefs ->
            prefs[ML_KEM_PUBLIC_KEY] = mlKemBase64
            prefs[X25519_PUBLIC_KEY] = x25519Base64
        }
    }

    suspend fun clearPublicKeys() {
        context.dataStore.edit { prefs ->
            prefs.remove(ML_KEM_PUBLIC_KEY)
            prefs.remove(X25519_PUBLIC_KEY)
        }
    }

    suspend fun setFcmToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token != null) {
                prefs[FCM_TOKEN] = token
            } else {
                prefs.remove(FCM_TOKEN)
            }
        }
    }
}

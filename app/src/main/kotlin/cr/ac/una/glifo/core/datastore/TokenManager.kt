package cr.ac.una.glifo.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "glifo_prefs")

class TokenManager private constructor(
    private val dataStoreProvider: () -> DataStore<Preferences>
) {

    constructor(context: Context) : this({ context.dataStore })

    internal constructor(dataStore: DataStore<Preferences>) : this({ dataStore })

    private val dataStore: DataStore<Preferences>
        get() = dataStoreProvider()

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")

        // Identidad estable de la instalación de la app y token FCM (#48)
        private val INSTALLATION_ID_KEY = stringPreferencesKey("installation_id")
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        private val REGISTERED_DEVICE_ID_KEY = longPreferencesKey("registered_device_id")
        private val REGISTERED_DEVICE_USER_ID_KEY = longPreferencesKey("registered_device_user_id")
        private val LAST_SYNCED_FCM_TOKEN_KEY = stringPreferencesKey("last_synced_fcm_token")
        private val NOTIFY_SYNC_DONE_KEY = booleanPreferencesKey("notify_sync_done")
        private val SEEN_NOTIFICATION_EVENTS_KEY = stringPreferencesKey("seen_notification_events")
        private const val MAX_SEEN_EVENTS = 50
        private const val LAST_REPORTED_SYNC_OP_PREFIX = "last_reported_sync_op_"

        private fun lastReportedSyncOpKey(userId: Long) =
            longPreferencesKey("$LAST_REPORTED_SYNC_OP_PREFIX$userId")
    }

    suspend fun saveToken(token: String, userId: Long, userEmail: String) {
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = userEmail
        }
    }

    suspend fun getToken(): String? {
        return dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun getUserId(): Long? {
        return dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun getOrCreateInstallationId(): String {
        val current = dataStore.data.first()[INSTALLATION_ID_KEY]
        if (!current.isNullOrBlank()) {
            return current
        }
        val generated = UUID.randomUUID().toString()
        var resolved = generated
        dataStore.edit { preferences ->
            val existing = preferences[INSTALLATION_ID_KEY]
            if (existing.isNullOrBlank()) {
                preferences[INSTALLATION_ID_KEY] = generated
            } else {
                resolved = existing
            }
        }
        return resolved
    }

    suspend fun saveFcmToken(token: String) {
        val normalized = token.trim()
        if (normalized.isEmpty()) return
        dataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = normalized
        }
    }

    suspend fun getFcmToken(): String? {
        return dataStore.data.first()[FCM_TOKEN_KEY]
    }

    suspend fun saveRegisteredDevice(deviceId: Long, userId: Long, syncedToken: String?) {
        dataStore.edit { preferences ->
            preferences[REGISTERED_DEVICE_ID_KEY] = deviceId
            preferences[REGISTERED_DEVICE_USER_ID_KEY] = userId
            if (syncedToken != null) {
                preferences[LAST_SYNCED_FCM_TOKEN_KEY] = syncedToken
            } else {
                preferences.remove(LAST_SYNCED_FCM_TOKEN_KEY)
            }
        }
    }

    suspend fun getRegisteredDeviceId(): Long? {
        return dataStore.data.first()[REGISTERED_DEVICE_ID_KEY]
    }

    suspend fun getRegisteredDeviceUserId(): Long? {
        return dataStore.data.first()[REGISTERED_DEVICE_USER_ID_KEY]
    }

    suspend fun getLastSyncedFcmToken(): String? {
        return dataStore.data.first()[LAST_SYNCED_FCM_TOKEN_KEY]
    }

    suspend fun clearRegisteredDevice() {
        dataStore.edit { preferences ->
            preferences.remove(REGISTERED_DEVICE_ID_KEY)
            preferences.remove(REGISTERED_DEVICE_USER_ID_KEY)
            preferences.remove(LAST_SYNCED_FCM_TOKEN_KEY)
        }
    }

    suspend fun getLastReportedSyncOpId(userId: Long): Long {
        return dataStore.data.first()[lastReportedSyncOpKey(userId)] ?: 0L
    }

    suspend fun setLastReportedSyncOpId(userId: Long, opId: Long) {
        dataStore.edit { preferences ->
            val key = lastReportedSyncOpKey(userId)
            val current = preferences[key] ?: 0L
            if (opId > current) {
                preferences[key] = opId
            }
        }
    }

    suspend fun isNotifySyncDoneEnabled(): Boolean {
        return dataStore.data.first()[NOTIFY_SYNC_DONE_KEY] ?: true
    }

    suspend fun setNotifySyncDoneEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFY_SYNC_DONE_KEY] = enabled
        }
    }

    suspend fun hasSeenNotificationEvent(eventId: String): Boolean {
        val raw = dataStore.data.first()[SEEN_NOTIFICATION_EVENTS_KEY].orEmpty()
        return raw.split(",").any { it == eventId }
    }

    /**
     * Registra un `event_id` recibido por FCM. Devuelve `true` si es la primera vez que se ve,
     * o `false` si ya había sido procesado en este dispositivo.
     */
    suspend fun markNotificationEventSeen(eventId: String): Boolean {
        val clean = eventId.trim()
        if (clean.isEmpty()) return false
        var wasNew = false
        dataStore.edit { preferences ->
            val existing = preferences[SEEN_NOTIFICATION_EVENTS_KEY]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                .orEmpty()
            if (clean !in existing) {
                wasNew = true
                val updated = (existing + clean).takeLast(MAX_SEEN_EVENTS)
                preferences[SEEN_NOTIFICATION_EVENTS_KEY] = updated.joinToString(",")
            }
        }
        return wasNew
    }

    /**
     * Limpia el contexto de sesión del usuario autenticado conservando el `installation_id`
     * de la app, el `fcm_token` actual del dispositivo y las preferencias locales.
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(REGISTERED_DEVICE_ID_KEY)
            preferences.remove(REGISTERED_DEVICE_USER_ID_KEY)
            preferences.remove(LAST_SYNCED_FCM_TOKEN_KEY)
            preferences.remove(SEEN_NOTIFICATION_EVENTS_KEY)
            preferences.asMap().keys
                .filter { it.name.startsWith(LAST_REPORTED_SYNC_OP_PREFIX) }
                .forEach { preferences.remove(it) }
        }
    }

    val tokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[TOKEN_KEY] != null
    }

    val notifySyncDoneFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFY_SYNC_DONE_KEY] ?: true
    }
}

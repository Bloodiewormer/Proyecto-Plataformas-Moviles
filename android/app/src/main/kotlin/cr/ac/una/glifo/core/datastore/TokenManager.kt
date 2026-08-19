package cr.ac.una.glifo.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "glifo_prefs")

class TokenManager @Inject constructor(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ROLES_KEY = stringSetPreferencesKey("user_roles")
    }

    suspend fun saveToken(token: String, userId: Long, userEmail: String, roles: List<String> = emptyList()) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = userEmail
            preferences[USER_ROLES_KEY] = roles.toSet()
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun getUserId(): Long? {
        return context.dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun getRoles(): Set<String> {
        return context.dataStore.data.first()[USER_ROLES_KEY] ?: emptySet()
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val rolesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[USER_ROLES_KEY] ?: emptySet()
    }

    val isAdminFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val roles = preferences[USER_ROLES_KEY] ?: emptySet()
        roles.contains("ROLE_ADMIN") || roles.contains("ADMINISTRADOR")
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY] != null
    }
}

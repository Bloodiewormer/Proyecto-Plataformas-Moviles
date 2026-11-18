package cr.ac.una.glifo.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferencias de Ajustes que cambian cómo trabaja la app. Se guardan en el mismo DataStore
 * que la sesión y sobreviven a cerrar sesión (clearAll no las toca).
 *
 * - rejectLowQuality: con la foto borrosa, oscura o quemada no se deja "procesar igual".
 * - allowVisionEscalation: si es false, el sync no manda recortes a N2 (/repair, visión de pago).
 * - syncWifiOnly: la cola offline solo se vacía con una red sin datos móviles.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val REJECT_LOW_QUALITY_KEY = booleanPreferencesKey("reject_low_quality")
        val ALLOW_VISION_ESCALATION_KEY = booleanPreferencesKey("allow_vision_escalation")
        val SYNC_WIFI_ONLY_KEY = booleanPreferencesKey("sync_wifi_only")
        val DAILY_STUDY_REMINDER_KEY = booleanPreferencesKey("daily_study_reminder")
    }

    val rejectLowQualityFlow: Flow<Boolean> = context.dataStore.data.map { it[REJECT_LOW_QUALITY_KEY] ?: true }
    val allowVisionEscalationFlow: Flow<Boolean> = context.dataStore.data.map { it[ALLOW_VISION_ESCALATION_KEY] ?: true }
    val syncWifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[SYNC_WIFI_ONLY_KEY] ?: false }
    val dailyStudyReminderFlow: Flow<Boolean> = context.dataStore.data.map { it[DAILY_STUDY_REMINDER_KEY] ?: true }

    suspend fun isRejectLowQuality(): Boolean = rejectLowQualityFlow.first()
    suspend fun isAllowVisionEscalation(): Boolean = allowVisionEscalationFlow.first()
    suspend fun isSyncWifiOnly(): Boolean = syncWifiOnlyFlow.first()
    suspend fun isDailyStudyReminder(): Boolean = dailyStudyReminderFlow.first()

    suspend fun setRejectLowQuality(enabled: Boolean) {
        context.dataStore.edit { it[REJECT_LOW_QUALITY_KEY] = enabled }
    }

    suspend fun setAllowVisionEscalation(enabled: Boolean) {
        context.dataStore.edit { it[ALLOW_VISION_ESCALATION_KEY] = enabled }
    }

    suspend fun setSyncWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[SYNC_WIFI_ONLY_KEY] = enabled }
    }

    suspend fun setDailyStudyReminder(enabled: Boolean) {
        context.dataStore.edit { it[DAILY_STUDY_REMINDER_KEY] = enabled }
    }
}

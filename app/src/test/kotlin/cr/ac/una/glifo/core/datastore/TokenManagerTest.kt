package cr.ac.una.glifo.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TokenManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `clearAll removes all per-user keys while preserving device identity and app preferences`() = runTest {
        val dataStoreFile = tempFolder.newFile("glifo_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataStoreFile }
        )
        val tokenManager = TokenManager(dataStore)

        val installationId = tokenManager.getOrCreateInstallationId()
        tokenManager.saveFcmToken("fcm-device-token")
        tokenManager.setNotifySyncDoneEnabled(false)

        val rejectLowQualityKey = booleanPreferencesKey("reject_low_quality")
        val allowVisionKey = booleanPreferencesKey("allow_vision_escalation")
        val wifiOnlyKey = booleanPreferencesKey("sync_wifi_only")
        dataStore.edit { prefs ->
            prefs[rejectLowQualityKey] = false
            prefs[allowVisionKey] = false
            prefs[wifiOnlyKey] = true
        }

        tokenManager.saveToken("jwt-user-1", userId = 42L, userEmail = "ana@una.ac.cr")
        tokenManager.saveRegisteredDevice(deviceId = 77L, userId = 42L, syncedToken = "fcm-device-token")
        tokenManager.setLastReportedSyncOpId(userId = 42L, opId = 15L)
        tokenManager.setLastReportedSyncOpId(userId = 99L, opId = 8L)
        assertTrue(tokenManager.markNotificationEventSeen("evt-123"))

        tokenManager.clearAll()

        assertNull(tokenManager.getToken())
        assertNull(tokenManager.getUserId())
        assertNull(tokenManager.getRegisteredDeviceId())
        assertNull(tokenManager.getRegisteredDeviceUserId())
        assertNull(tokenManager.getLastSyncedFcmToken())
        assertFalse(tokenManager.hasSeenNotificationEvent("evt-123"))
        assertEquals(0L, tokenManager.getLastReportedSyncOpId(42L))
        assertEquals(0L, tokenManager.getLastReportedSyncOpId(99L))

        assertEquals(installationId, tokenManager.getOrCreateInstallationId())
        assertEquals("fcm-device-token", tokenManager.getFcmToken())
        assertFalse(tokenManager.isNotifySyncDoneEnabled())

        val remainingPrefs = dataStore.data.first()
        assertEquals(false, remainingPrefs[rejectLowQualityKey])
        assertEquals(false, remainingPrefs[allowVisionKey])
        assertEquals(true, remainingPrefs[wifiOnlyKey])
    }
}

package cr.ac.una.glifo.core.push

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.core.sync.SyncScheduler
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.AuthResponse
import cr.ac.una.glifo.data.dto.DeviceResponse
import cr.ac.una.glifo.data.dto.NotePageResponse
import cr.ac.una.glifo.data.dto.SyncCompleteRequest
import cr.ac.una.glifo.data.dto.SyncCompleteResponse
import cr.ac.una.glifo.data.dto.UserDto
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.data.repository.AuthRepositoryImpl
import cr.ac.una.glifo.data.repository.SyncRepositoryImpl
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.SyncRunOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class DevicePushAndSyncNotificationTest {

    private val api = mockk<GlifoApi>()
    private val syncDao = mockk<SyncOperationDao>(relaxed = true)
    private val noteDao = mockk<NoteDao>(relaxed = true)
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val tokenManager = mockk<TokenManager>(relaxed = true)

    private var storedJwt: String? = null
    private var storedUserId: Long? = null
    private var storedInstallationId: String = "inst-device-a-0001"
    private var storedFcmToken: String? = null
    private var registeredDeviceId: Long? = null
    private var registeredDeviceUserId: Long? = null
    private var lastSyncedFcmToken: String? = null
    private var notifySyncDoneEnabled: Boolean = true
    private val lastReportedOpByUser = mutableMapOf<Long, Long>()
    private val seenEvents = mutableSetOf<String>()

    private lateinit var devicePushManager: DevicePushManager

    @Before
    fun setUp() {
        storedJwt = null
        storedUserId = null
        storedInstallationId = "inst-device-a-0001"
        storedFcmToken = null
        registeredDeviceId = null
        registeredDeviceUserId = null
        lastSyncedFcmToken = null
        notifySyncDoneEnabled = true
        lastReportedOpByUser.clear()
        seenEvents.clear()

        coEvery { tokenManager.saveToken(any(), any(), any()) } answers {
            storedJwt = firstArg()
            storedUserId = secondArg()
        }
        coEvery { tokenManager.getToken() } answers { storedJwt }
        coEvery { tokenManager.getUserId() } answers { storedUserId }
        coEvery { tokenManager.getOrCreateInstallationId() } answers { storedInstallationId }
        coEvery { tokenManager.saveFcmToken(any()) } answers { storedFcmToken = firstArg() }
        coEvery { tokenManager.getFcmToken() } answers { storedFcmToken }
        coEvery { tokenManager.saveRegisteredDevice(any(), any(), any()) } answers {
            registeredDeviceId = firstArg()
            registeredDeviceUserId = secondArg()
            lastSyncedFcmToken = thirdArg()
        }
        coEvery { tokenManager.getRegisteredDeviceId() } answers { registeredDeviceId }
        coEvery { tokenManager.getRegisteredDeviceUserId() } answers { registeredDeviceUserId }
        coEvery { tokenManager.getLastSyncedFcmToken() } answers { lastSyncedFcmToken }
        coEvery { tokenManager.clearRegisteredDevice() } answers {
            registeredDeviceId = null
            registeredDeviceUserId = null
            lastSyncedFcmToken = null
        }
        coEvery { tokenManager.getLastReportedSyncOpId(any()) } answers {
            lastReportedOpByUser[firstArg()] ?: 0L
        }
        coEvery { tokenManager.setLastReportedSyncOpId(any(), any()) } answers {
            val uid = firstArg<Long>()
            val opId = secondArg<Long>()
            val current = lastReportedOpByUser[uid] ?: 0L
            if (opId > current) {
                lastReportedOpByUser[uid] = opId
            }
        }
        coEvery { tokenManager.isNotifySyncDoneEnabled() } answers { notifySyncDoneEnabled }
        coEvery { tokenManager.markNotificationEventSeen(any()) } answers {
            seenEvents.add(firstArg())
        }
        coEvery { tokenManager.clearAll() } answers {
            storedJwt = null
            storedUserId = null
            registeredDeviceId = null
            registeredDeviceUserId = null
            lastSyncedFcmToken = null
        }

        devicePushManager = DevicePushManager(
            api = api,
            tokenManager = tokenManager,
            syncDao = syncDao,
            fcmTokenFetcher = FcmTokenFetcher { storedFcmToken }
        )
    }

    @Test
    fun `persists FCM token before login and registers device with token upon authentication`() = runTest {
        // 1. El token llega antes de que el usuario haya iniciado sesión
        devicePushManager.onNewFcmToken("fcm-prelogin-token-123456")

        assertEquals("fcm-prelogin-token-123456", storedFcmToken)
        coVerify(exactly = 0) { api.registerDevice(any()) }
        coVerify(exactly = 0) { api.updateDeviceToken(any(), any()) }

        // 2. El usuario inicia sesión y AuthRepositoryImpl reconcilia el dispositivo
        coEvery { api.login(any()) } returns AuthResponse(
            token = "jwt-user-1",
            user = UserDto(id = 10L, email = "est@una.ac.cr", firstName = "Ana", lastName = "Soto", role = "STUDENT")
        )
        coEvery { api.registerDevice(any()) } returns ApiResponse(
            DeviceResponse(
                id = 77L,
                userId = 10L,
                installationId = storedInstallationId,
                platform = "ANDROID",
                active = true,
                hasFcmToken = true,
                lastSeenAt = "2026-10-27T12:00:00Z",
                updatedAt = "2026-10-27T12:00:00Z"
            )
        )

        val authRepo = AuthRepositoryImpl(api, tokenManager, devicePushManager)
        val loginResult = authRepo.login("est@una.ac.cr", "password123")

        assertTrue(loginResult is Result.Success)
        assertEquals(77L, registeredDeviceId)
        assertEquals(10L, registeredDeviceUserId)
        assertEquals("fcm-prelogin-token-123456", lastSyncedFcmToken)
        coVerify(exactly = 1) {
            api.registerDevice(
                match {
                    it.installationId == "inst-device-a-0001" &&
                        it.platform == "ANDROID" &&
                        it.fcmToken == "fcm-prelogin-token-123456"
                }
            )
        }
    }

    @Test
    fun `updates FCM token on server when onNewToken fires during an active session`() = runTest {
        storedJwt = "jwt-user-1"
        storedUserId = 10L
        registeredDeviceId = 77L
        registeredDeviceUserId = 10L
        lastSyncedFcmToken = "old-token-0001"

        coEvery { api.updateDeviceToken(77L, any()) } returns ApiResponse(
            DeviceResponse(
                id = 77L,
                userId = 10L,
                installationId = storedInstallationId,
                platform = "ANDROID",
                active = true,
                hasFcmToken = true,
                lastSeenAt = "2026-10-27T12:05:00Z",
                updatedAt = "2026-10-27T12:05:00Z"
            )
        )

        devicePushManager.onNewFcmToken("rotated-token-9999")

        assertEquals("rotated-token-9999", storedFcmToken)
        assertEquals("rotated-token-9999", lastSyncedFcmToken)
        coVerify(exactly = 1) {
            api.updateDeviceToken(77L, match { it.fcmToken == "rotated-token-9999" })
        }
    }

    @Test
    fun `detects real queue completion, sends a single completion signal with deterministic key, and skips empty runs`() = runTest {
        storedJwt = "jwt-user-1"
        storedUserId = 10L
        storedFcmToken = "fcm-token-device-a"
        registeredDeviceId = 77L
        registeredDeviceUserId = 10L
        lastSyncedFcmToken = "fcm-token-device-a"

        val emptyAnalysis = PageAnalysis(emptyList(), 1200, 1600, 0, 0).toSnapshotJson()
        val page1 = NotePageEntity(3L, 7L, 0, "c3a1f0e98b7d6245", "/p1.jpg", "N1", 0.8f, emptyAnalysis, 0L)
        val page2 = NotePageEntity(4L, 7L, 1, "d4b2f0e98b7d6246", "/p2.jpg", "N1", 0.85f, emptyAnalysis, 0L)
        val syncedNote = NoteEntity(7L, 42L, 1L, "Apunte", "2026-09-30", "PROCESSING", null, 0L)

        val op1 = SyncOperationEntity(1L, SyncOperationEntity.TYPE_UPLOAD_PAGE, "k1", 7L, 3L, createdAt = 0L, updatedAt = 0L)
        val op2 = SyncOperationEntity(2L, SyncOperationEntity.TYPE_UPLOAD_PAGE, "k2", 7L, 4L, createdAt = 0L, updatedAt = 0L)

        coEvery { syncDao.runnable() } returns listOf(op1, op2) andThen emptyList()
        coEvery { noteDao.noteById(7L) } returns syncedNote
        coEvery { noteDao.pageById(3L) } returns page1
        coEvery { noteDao.pageById(4L) } returns page2
        coEvery { api.uploadPage(42L, any()) } returns ApiResponse(NotePageResponse(9L, 42L, 0))
        coEvery { syncDao.pendingCount() } returns 0
        coEvery { syncDao.maxDoneId() } returns 2L
        coEvery { syncDao.countDoneAfter(0L) } returns 2
        coEvery { syncDao.syncedServerNoteIdsAfter(0L) } returns listOf(42L)

        val requestSlot = slot<SyncCompleteRequest>()
        coEvery { api.completeSync(capture(requestSlot)) } returns ApiResponse(
            SyncCompleteResponse(
                eventId = "11111111-2222-3333-4444-555555555555",
                notificationId = 500L,
                deviceId = 77L,
                uploadedCount = 2,
                notificationStatus = "ACCEPTED",
                fcmMessageId = "projects/glifoapp/messages/1001",
                duplicate = false
            )
        )

        val syncRepository = SyncRepositoryImpl(api, syncDao, noteDao, scheduler, devicePushManager)

        // Primera pasada: sube 2 páginas y vacía la cola -> informa 1 sola vez al final
        val firstRun = (syncRepository.syncNow() as Result.Success).value
        assertEquals(2, firstRun.uploaded)
        assertEquals(SyncRunOutcome.Completed, firstRun.outcome)
        coVerify(exactly = 2) { api.uploadPage(42L, any()) }
        coVerify(exactly = 1) { api.completeSync(any()) }
        assertEquals(77L, requestSlot.captured.deviceId)
        assertEquals(2, requestSlot.captured.uploadedCount)
        assertEquals(0, requestSlot.captured.pendingCount)
        assertEquals(0, requestSlot.captured.failedCount)
        assertEquals(listOf(42L), requestSlot.captured.noteIds)

        // Segunda pasada sin trabajo nuevo -> no vuelve a invocar completeSync
        val secondRun = (syncRepository.syncNow() as Result.Success).value
        assertEquals(0, secondRun.uploaded)
        assertEquals(SyncRunOutcome.Completed, secondRun.outcome)
        coVerify(exactly = 1) { api.completeSync(any()) }
    }

    @Test
    fun `does not report queue completion when pending or failed operations remain, and retries completeSync with same idempotency key`() = runTest {
        storedJwt = "jwt-user-1"
        storedUserId = 10L
        storedFcmToken = "fcm-token-device-a"
        registeredDeviceId = 77L
        registeredDeviceUserId = 10L
        lastSyncedFcmToken = "fcm-token-device-a"

        // Caso A: todavía quedan elementos pendientes en la cola local
        coEvery { syncDao.pendingCount() } returns 1
        assertEquals(QueueCompletionReportOutcome.NoOp, devicePushManager.reportQueueCompletionIfNeeded(1))
        coVerify(exactly = 0) { api.completeSync(any()) }

        // Caso B: la cola está vacía pero completeSync falla por red en el primer intento y luego tiene éxito
        coEvery { syncDao.pendingCount() } returns 0
        coEvery { syncDao.maxDoneId() } returns 5L
        coEvery { syncDao.countDoneAfter(0L) } returns 3
        coEvery { syncDao.syncedServerNoteIdsAfter(0L) } returns listOf(42L)

        val capturedRequests = mutableListOf<SyncCompleteRequest>()
        coEvery { api.completeSync(capture(capturedRequests)) } throws IOException("timeout") andThen ApiResponse(
            SyncCompleteResponse(
                eventId = "22222222-3333-4444-5555-666666666666",
                notificationId = 501L,
                deviceId = 77L,
                uploadedCount = 3,
                notificationStatus = "ACCEPTED",
                fcmMessageId = "projects/glifoapp/messages/1002",
                duplicate = false
            )
        )

        val firstAttempt = devicePushManager.reportQueueCompletionIfNeeded(3)
        assertEquals(QueueCompletionReportOutcome.RetryLater, firstAttempt)
        assertEquals(0L, lastReportedOpByUser[10L] ?: 0L)

        val secondAttempt = devicePushManager.reportQueueCompletionIfNeeded(0)
        assertEquals(QueueCompletionReportOutcome.Completed, secondAttempt)
        assertEquals(5L, lastReportedOpByUser[10L])
        assertEquals(2, capturedRequests.size)
        assertEquals(capturedRequests[0].idempotencyKey, capturedRequests[1].idempotencyKey)
    }

    @Test
    fun `validates notification payload, formats offline queue message without claiming AI finished, and deduplicates by event_id`() = runTest {
        storedJwt = "jwt-user-1"
        storedUserId = 10L
        registeredDeviceId = 77L
        registeredDeviceUserId = 10L

        val context = mockk<Context>(relaxed = true)
        every { context.getString(R.string.sync_notification_body_single) } returns
            "Se subió 1 elemento de la cola sin conexión desde este dispositivo."
        every { context.getString(R.string.sync_notification_body_plural, 3) } returns
            "Se subieron 3 elementos de la cola sin conexión desde este dispositivo."

        val bodySingle = SyncNotificationPresenter.buildNotificationBody(context, 1)
        val bodyPlural = SyncNotificationPresenter.buildNotificationBody(context, 3)
        assertTrue(bodySingle.contains("cola sin conexión"))
        assertFalse(bodySingle.contains("IA", ignoreCase = true))
        assertTrue(bodyPlural.contains("3 elementos de la cola sin conexión"))
        assertFalse(bodyPlural.contains("IA", ignoreCase = true))

        val payload = mapOf(
            "schema_version" to "1",
            "kind" to "SYNC_COMPLETE",
            "event_id" to "evt-uuid-0001",
            "notification_id" to "900",
            "device_id" to "77",
            "user_id" to "10",
            "uploaded_count" to "3"
        )

        val validated = SyncNotificationPayloadValidator.validate(
            data = payload,
            notifyEnabled = true,
            currentUserId = 10L,
            registeredDeviceId = 77L
        )
        assertNotNull(validated)
        assertEquals("evt-uuid-0001", validated?.eventId)
        assertEquals(3, validated?.uploadedCount)

        // Primera vez que se ve el event_id -> nuevo
        assertTrue(tokenManager.markNotificationEventSeen("evt-uuid-0001"))
        // Segunda vez con el mismo event_id -> duplicado descartado
        assertFalse(tokenManager.markNotificationEventSeen("evt-uuid-0001"))
    }

    @Test
    fun `does not crash or post notification when POST_NOTIFICATIONS permission is missing`() = runTest {
        storedJwt = "jwt-user-1"
        storedUserId = 10L
        registeredDeviceId = 77L
        registeredDeviceUserId = 10L

        val context = mockk<Context>(relaxed = true)
        val presenter = spyk(SyncNotificationPresenter(context, tokenManager))
        every { presenter.hasNotificationPermission() } returns false

        val payload = mapOf(
            "schema_version" to "1",
            "kind" to "SYNC_COMPLETE",
            "event_id" to "evt-no-perm-0001",
            "device_id" to "77",
            "user_id" to "10",
            "uploaded_count" to "2"
        )

        val shown = presenter.handleIncomingDataMessage(payload)
        assertFalse(shown)
    }

    @Test
    fun `unlinks device on logout and rejects notifications from previous account or other device`() = runTest {
        storedJwt = "jwt-user-1"
        storedUserId = 10L
        storedFcmToken = "fcm-shared-phone-token"
        registeredDeviceId = 77L
        registeredDeviceUserId = 10L
        lastSyncedFcmToken = "fcm-shared-phone-token"

        coEvery { api.unlinkDevice(77L) } returns Response.success(204, Unit)

        // 1. Cerrar sesión desvincula el dispositivo en el servidor y limpia la sesión local
        devicePushManager.unlinkDeviceOnLogout()

        coVerify(exactly = 1) { api.unlinkDevice(77L) }
        assertNull(storedJwt)
        assertNull(storedUserId)
        assertNull(registeredDeviceId)
        // El installation_id y el fcm_token del teléfono se conservan para el siguiente inicio de sesión
        assertEquals("inst-device-a-0001", storedInstallationId)
        assertEquals("fcm-shared-phone-token", storedFcmToken)

        // 2. Si llega un mensaje rezagado de la cuenta 10 sin sesión activa, se descarta
        val payloadForOldUser = mapOf(
            "schema_version" to "1",
            "kind" to "SYNC_COMPLETE",
            "event_id" to "evt-old-user-10",
            "device_id" to "77",
            "user_id" to "10",
            "uploaded_count" to "1"
        )
        assertNull(
            SyncNotificationPayloadValidator.validate(
                data = payloadForOldUser,
                notifyEnabled = true,
                currentUserId = storedUserId,
                registeredDeviceId = registeredDeviceId
            )
        )

        // 3. Inicia sesión otra cuenta (userId = 20L, deviceId = 88L) en el mismo teléfono
        storedJwt = "jwt-user-2"
        storedUserId = 20L
        registeredDeviceId = 88L
        registeredDeviceUserId = 20L

        // El evento de la cuenta 10 sigue siendo rechazado
        assertNull(
            SyncNotificationPayloadValidator.validate(
                data = payloadForOldUser,
                notifyEnabled = true,
                currentUserId = storedUserId,
                registeredDeviceId = registeredDeviceId
            )
        )

        // Un evento para el mismo usuario 20 pero dirigido a otro dispositivo (device_id = 99) también se rechaza
        val payloadForOtherDevice = mapOf(
            "schema_version" to "1",
            "kind" to "SYNC_COMPLETE",
            "event_id" to "evt-other-device-99",
            "device_id" to "99",
            "user_id" to "20",
            "uploaded_count" to "1"
        )
        assertNull(
            SyncNotificationPayloadValidator.validate(
                data = payloadForOtherDevice,
                notifyEnabled = true,
                currentUserId = storedUserId,
                registeredDeviceId = registeredDeviceId
            )
        )
    }
}

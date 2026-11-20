package cr.ac.una.glifo.ops

import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.ops.entity.SyncQueueEntry
import cr.ac.una.glifo.ops.fcm.FcmClient
import cr.ac.una.glifo.ops.fcm.FcmErrorKind
import cr.ac.una.glifo.ops.fcm.FcmSendOutcome
import cr.ac.una.glifo.ops.fcm.FcmTargetedMessage
import cr.ac.una.glifo.ops.repository.DeviceRepository
import cr.ac.una.glifo.ops.repository.NotificationRepository
import cr.ac.una.glifo.ops.repository.SyncQueueRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Suite de integración para el issue #48:
 * - Registro inicial, rotación de token, reasignación entre usuarios, logout y autorización.
 * - Condición de finalización de cola offline, direccionamiento exclusivo por `device_id`,
 *   idempotencia ante reintentos, transacciones cerradas antes de llamar a FCM, reintentos
 *   acotados ante errores transitorios e invalidación selectiva de tokens (`UNREGISTERED`).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(FcmPushNotificationIntegrationTest.RecordingFcmTestConfig::class)
class FcmPushNotificationIntegrationTest {

    @TestConfiguration
    class RecordingFcmTestConfig {
        @Bean
        @Primary
        fun recordingFcmClient(): RecordingFcmClient = RecordingFcmClient()
    }

    class RecordingFcmClient : FcmClient {
        val sentMessages = CopyOnWriteArrayList<FcmTargetedMessage>()
        val activeTransactionDuringSend = CopyOnWriteArrayList<Boolean>()
        var scriptedOutcomes = ArrayDeque<FcmSendOutcome>()
        var defaultOutcome: FcmSendOutcome = FcmSendOutcome.Accepted("projects/glifoapp/messages/msg-default-001")

        fun reset() {
            sentMessages.clear()
            activeTransactionDuringSend.clear()
            scriptedOutcomes.clear()
            defaultOutcome = FcmSendOutcome.Accepted("projects/glifoapp/messages/msg-default-001")
        }

        override fun send(message: FcmTargetedMessage): FcmSendOutcome {
            sentMessages.add(message)
            activeTransactionDuringSend.add(TransactionSynchronizationManager.isActualTransactionActive())
            return if (scriptedOutcomes.isNotEmpty()) {
                scriptedOutcomes.removeFirst()
            } else {
                defaultOutcome
            }
        }
    }

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var recordingFcmClient: RecordingFcmClient
    @Autowired lateinit var deviceRepository: DeviceRepository
    @Autowired lateinit var notificationRepository: NotificationRepository
    @Autowired lateinit var syncQueueRepository: SyncQueueRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var courseRepository: CourseRepository

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
        recordingFcmClient.reset()
        cleanOpsTables()
    }

    @AfterEach
    fun tearDown() {
        cleanOpsTables()
    }

    private fun cleanOpsTables() {
        notificationRepository.deleteAllInBatch()
        syncQueueRepository.deleteAllInBatch()
        deviceRepository.deleteAllInBatch()
    }

    private fun bearer(email: String, password: String): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun studentToken() = bearer("estudiante@est.una.ac.cr", "Estudiante123")
    private fun teacherToken() = bearer("docente@una.ac.cr", "Docente123")

    private fun registerDevice(
        bearerToken: String,
        installationId: String,
        fcmToken: String?,
        platform: String = "ANDROID",
    ): Long {
        val tokenJson = if (fcmToken != null) """"$fcmToken"""" else "null"
        val body = mvc.post("/api/v1/devices") {
            header("Authorization", bearerToken)
            contentType = MediaType.APPLICATION_JSON
            content = """{"installation_id":"$installationId","fcm_token":$tokenJson,"platform":"$platform"}"""
        }.andExpect {
            status { is2xxSuccessful() }
        }.andReturn().response.contentAsString
        return json.readTree(body)["data"]["id"].asLong()
    }

    // -------------------------------------------------------------------------
    // 1. Registro inicial y 2. Rotación de token del mismo device_id
    // -------------------------------------------------------------------------

    @Test
    fun `registro inicial y rotacion de token conservan el mismo device_id`() {
        val token = studentToken()
        val installationId = "inst-student-phone-001"
        val initialFcmToken = "fcm-token-initial-student-00001"
        val rotatedFcmToken = "fcm-token-rotated-student-00002"

        val deviceId = registerDevice(token, installationId, initialFcmToken)
        val initialDevice = deviceRepository.findById(deviceId).orElseThrow()
        assertEquals(initialFcmToken, initialDevice.fcmToken)
        assertTrue(initialDevice.active)

        // Rotación mediante PUT /api/v1/devices/{id}/token
        mvc.put("/api/v1/devices/$deviceId/token") {
            header("Authorization", token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"fcm_token":"$rotatedFcmToken"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(deviceId) }
            jsonPath("$.data.is_active") { value(true) }
            jsonPath("$.data.token_registered") { value(true) }
        }

        val updatedDevice = deviceRepository.findById(deviceId).orElseThrow()
        assertEquals(deviceId, updatedDevice.id)
        assertEquals(rotatedFcmToken, updatedDevice.fcmToken)

        // Reconciliación mediante POST /api/v1/devices con el mismo installation_id conserva el mismo device_id
        val reconciledId = registerDevice(token, installationId, "fcm-token-rotated-student-00003")
        assertEquals(deviceId, reconciledId)
        assertEquals(1, deviceRepository.findAll().size)
    }

    // -------------------------------------------------------------------------
    // 3. Reasignación segura si un token pasa a otro usuario y 4. Logout
    // -------------------------------------------------------------------------

    @Test
    fun `reasignacion de un mismo token FCM a otra cuenta desvincula la cuenta anterior sin borrar historico`() {
        val student = studentToken()
        val teacher = teacherToken()
        val sharedPhysicalToken = "fcm-token-shared-physical-phone-9999"

        val studentDeviceId = registerDevice(student, "inst-shared-phone-01", sharedPhysicalToken)

        // El docente inicia sesión en el mismo teléfono físico y registra el mismo token FCM
        val teacherDeviceId = registerDevice(teacher, "inst-shared-phone-01", sharedPhysicalToken)

        val studentDevice = deviceRepository.findById(studentDeviceId).orElseThrow()
        val teacherDevice = deviceRepository.findById(teacherDeviceId).orElseThrow()

        assertNull(studentDevice.fcmToken, "El token debe desvincularse del dispositivo de la cuenta anterior")
        assertFalse(studentDevice.active)
        assertEquals(sharedPhysicalToken, teacherDevice.fcmToken)
        assertTrue(teacherDevice.active)
    }

    @Test
    fun `desvinculacion en logout limpia fcm_token e inactiva el dispositivo sin eliminar la fila`() {
        val student = studentToken()
        val deviceId = registerDevice(student, "inst-logout-device-01", "fcm-token-logout-test-12345")

        mvc.delete("/api/v1/devices/$deviceId") {
            header("Authorization", student)
        }.andExpect {
            status { isNoContent() }
        }

        val deviceAfterLogout = deviceRepository.findById(deviceId).orElseThrow()
        assertNull(deviceAfterLogout.fcmToken)
        assertFalse(deviceAfterLogout.active)
    }

    // -------------------------------------------------------------------------
    // 5. Rechazo al operar sobre un device_id ajeno
    // -------------------------------------------------------------------------

    @Test
    fun `operar sobre un device_id ajeno responde 403 Forbidden en token, logout y sync complete`() {
        val student = studentToken()
        val teacher = teacherToken()
        val studentDeviceId = registerDevice(student, "inst-student-private-01", "fcm-token-student-private-01")

        // El docente intenta rotar el token del dispositivo del estudiante
        mvc.put("/api/v1/devices/$studentDeviceId/token") {
            header("Authorization", teacher)
            contentType = MediaType.APPLICATION_JSON
            content = """{"fcm_token":"fcm-token-hijack-attempt-0001"}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        // El docente intenta desvincular el dispositivo del estudiante
        mvc.delete("/api/v1/devices/$studentDeviceId") {
            header("Authorization", teacher)
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        // El docente intenta disparar cierre de cola sobre el dispositivo del estudiante
        mvc.post("/api/v1/sync/complete") {
            header("Authorization", teacher)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $studentDeviceId,
                  "idempotency_key": "${UUID.randomUUID()}",
                  "uploaded_count": 2,
                  "pending_count": 0,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        assertTrue(recordingFcmClient.sentMessages.isEmpty())
    }

    // -------------------------------------------------------------------------
    // 6. Subida parcial con elementos aún pendientes: no envía FCM
    // -------------------------------------------------------------------------

    @Test
    fun `subida parcial con elementos pendientes o fallidos responde 409 y no envia FCM`() {
        val student = studentToken()
        val deviceId = registerDevice(student, "inst-partial-sync-01", "fcm-token-partial-sync-0001")

        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $deviceId,
                  "idempotency_key": "${UUID.randomUUID()}",
                  "uploaded_count": 2,
                  "pending_count": 1,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("SYNC_QUEUE_INCOMPLETE") }
        }

        // También rechaza si el servidor tiene una operación en estado PENDING/FAILED para ese dispositivo
        val deviceEntity = deviceRepository.findById(deviceId).orElseThrow()
        val userEntity = userRepository.findByEmail("estudiante@est.una.ac.cr")!!
        syncQueueRepository.saveAndFlush(
            SyncQueueEntry().apply {
                this.user = userEntity
                this.device = deviceEntity
                this.entityType = "NOTE_PAGE"
                this.idempotencyKey = UUID.randomUUID()
                this.payload = """{"schemaVersion":1}"""
                this.status = "PENDING"
                this.attempts = 1
            },
        )

        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $deviceId,
                  "idempotency_key": "${UUID.randomUUID()}",
                  "uploaded_count": 2,
                  "pending_count": 0,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("SYNC_QUEUE_INCOMPLETE") }
        }

        assertTrue(recordingFcmClient.sentMessages.isEmpty())
        assertEquals(0, notificationRepository.count())
    }

    // -------------------------------------------------------------------------
    // 7. Dos dispositivos del mismo usuario: solo notifica a device_A
    // -------------------------------------------------------------------------

    @Test
    fun `finalizacion de cola de device_A con device_B registrado para el mismo usuario solo envia FCM a device_A`() {
        val student = studentToken()
        val tokenDeviceA = "fcm-token-student-device-A-11111"
        val tokenDeviceB = "fcm-token-student-device-B-22222"

        val deviceAId = registerDevice(student, "inst-student-device-A", tokenDeviceA)
        val deviceBId = registerDevice(student, "inst-student-device-B", tokenDeviceB)
        val idempotencyKey = UUID.randomUUID().toString()

        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $deviceAId,
                  "idempotency_key": "$idempotencyKey",
                  "uploaded_count": 3,
                  "pending_count": 0,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.device_id") { value(deviceAId) }
            jsonPath("$.data.queue_status") { value("DONE") }
            jsonPath("$.data.notification_status") { value("ACCEPTED") }
            jsonPath("$.data.duplicate") { value(false) }
        }

        assertEquals(1, recordingFcmClient.sentMessages.size, "Debe enviarse exactamente 1 mensaje FCM")
        val sent = recordingFcmClient.sentMessages.single()
        assertEquals(tokenDeviceA, sent.fcmToken, "El mensaje debe dirigirse exclusivamente al token de device_A")
        assertFalse(sent.fcmToken == tokenDeviceB)
        assertEquals(deviceAId.toString(), sent.data["device_id"])
        assertEquals(idempotencyKey, sent.data["event_id"])
        assertEquals("SYNC_COMPLETE", sent.data["kind"])
        assertEquals("1", sent.data["schema_version"])
        assertEquals("3", sent.data["uploaded_count"])

        // Verificar que la llamada a FCM ocurrió fuera de cualquier transacción de BD abierta
        assertEquals(listOf(false), recordingFcmClient.activeTransactionDuringSend)
        assertTrue(deviceBId != deviceAId)
    }

    // -------------------------------------------------------------------------
    // 8. Reintento del mismo lote (idempotencia) y 9. Ejecución sin trabajo nuevo
    // -------------------------------------------------------------------------

    @Test
    fun `reintento del cierre del mismo lote es idempotente y no duplica notifications ni envia segundo FCM`() {
        val student = studentToken()
        val deviceId = registerDevice(student, "inst-idempotent-device-01", "fcm-token-idempotent-00001")
        val idempotencyKey = UUID.randomUUID().toString()

        val payload = """
            {
              "device_id": $deviceId,
              "idempotency_key": "$idempotencyKey",
              "uploaded_count": 2,
              "pending_count": 0,
              "failed_count": 0,
              "note_ids": []
            }
        """.trimIndent()

        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.duplicate") { value(false) }
            jsonPath("$.data.notification_status") { value("ACCEPTED") }
        }

        // Segundo llamado con el mismo idempotency_key
        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.duplicate") { value(true) }
            jsonPath("$.data.notification_status") { value("ACCEPTED") }
        }

        assertEquals(1, recordingFcmClient.sentMessages.size, "No debe enviar un segundo mensaje a FCM")
        assertEquals(1, notificationRepository.count(), "No debe duplicar filas en notifications")
        assertEquals(1, syncQueueRepository.count(), "No debe duplicar filas en sync_queue")
    }

    @Test
    fun `ejecucion sin elementos nuevos sincronizados no crea notificacion ni invoca FCM`() {
        val student = studentToken()
        val deviceId = registerDevice(student, "inst-noop-device-01", "fcm-token-noop-device-00001")

        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $deviceId,
                  "idempotency_key": "${UUID.randomUUID()}",
                  "uploaded_count": 0,
                  "pending_count": 0,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.queue_status") { value("IDLE") }
            jsonPath("$.data.notification_status") { value("NO_NEW_UPLOADS") }
        }

        assertTrue(recordingFcmClient.sentMessages.isEmpty())
        assertEquals(0, notificationRepository.count())
    }

    // -------------------------------------------------------------------------
    // 10. Éxito de FCM, 11. Fallo transitorio y 12. UNREGISTERED vs INVALID_ARGUMENT
    // -------------------------------------------------------------------------

    @Test
    fun `exito de FCM registra ACCEPTED y sent_at con message_id sin marcar read_at ni afirmar entrega en el telefono`() {
        val student = studentToken()
        val deviceId = registerDevice(student, "inst-accepted-device-01", "fcm-token-accepted-00001")
        val idempotencyUuid = UUID.randomUUID()
        recordingFcmClient.defaultOutcome = FcmSendOutcome.Accepted("projects/glifoapp/messages/msg-98765")

        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $deviceId,
                  "idempotency_key": "$idempotencyUuid",
                  "uploaded_count": 1,
                  "pending_count": 0,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.notification_status") { value("ACCEPTED") }
        }

        val notification = notificationRepository.findByIdempotencyKey(idempotencyUuid)
        assertNotNull(notification)
        assertEquals("ACCEPTED", notification!!.status)
        assertEquals(1, notification.attempts)
        assertEquals("projects/glifoapp/messages/msg-98765", notification.fcmMessageId)
        assertNotNull(notification.sentAt)
        assertNull(notification.readAt, "La aceptación de FCM no implica que el dispositivo haya recibido o leído el mensaje")
    }

    @Test
    fun `fallo transitorio de FCM reintenta de forma acotada y si persiste conserva la subida con estado FAILED_RETRYABLE`() {
        val student = studentToken()
        val deviceId = registerDevice(student, "inst-retry-device-01", "fcm-token-retryable-00001")
        val idempotencyUuid = UUID.randomUUID()

        // Los 3 intentos fallan con UNAVAILABLE
        repeat(3) {
            recordingFcmClient.scriptedOutcomes.addLast(
                FcmSendOutcome.Failed(
                    errorKind = FcmErrorKind.UNAVAILABLE,
                    errorCode = "UNAVAILABLE",
                    sanitizedMessage = "Servicio FCM temporalmente no disponible",
                ),
            )
        }

        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $deviceId,
                  "idempotency_key": "$idempotencyUuid",
                  "uploaded_count": 2,
                  "pending_count": 0,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.queue_status") { value("DONE") }
            jsonPath("$.data.notification_status") { value("FAILED_RETRYABLE") }
        }

        assertEquals(3, recordingFcmClient.sentMessages.size, "Debe agotar los 3 intentos configurados")
        val syncEntry = syncQueueRepository.findByIdempotencyKey(idempotencyUuid)
        assertNotNull(syncEntry)
        assertEquals("DONE", syncEntry!!.status, "El fallo de FCM no debe revertir el cierre de sincronización")

        val notification = notificationRepository.findByIdempotencyKey(idempotencyUuid)!!
        assertEquals("FAILED_RETRYABLE", notification.status)
        assertEquals(3, notification.attempts)

        // El token NO debe invalidarse ante un fallo transitorio
        val device = deviceRepository.findById(deviceId).orElseThrow()
        assertEquals("fcm-token-retryable-00001", device.fcmToken)
        assertTrue(device.active)
    }

    @Test
    fun `error UNREGISTERED invalida el token del dispositivo sin borrar historico pero INVALID_ARGUMENT conserva el token`() {
        val student = studentToken()
        val deviceId = registerDevice(student, "inst-unregistered-01", "fcm-token- will-stay-on-invalid-arg")

        // 1. Primero un fallo INVALID_ARGUMENT: NO debe invalidar el token ni reintentar
        recordingFcmClient.scriptedOutcomes.addLast(
            FcmSendOutcome.Failed(
                errorKind = FcmErrorKind.INVALID_ARGUMENT,
                errorCode = "INVALID_ARGUMENT",
                sanitizedMessage = "Payload rechazado",
            ),
        )

        val keyInvalidArg = UUID.randomUUID()
        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $deviceId,
                  "idempotency_key": "$keyInvalidArg",
                  "uploaded_count": 1,
                  "pending_count": 0,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.notification_status") { value("FAILED_PERMANENT") }
        }

        assertEquals(1, recordingFcmClient.sentMessages.size, "Un error permanente no se reintenta")
        val deviceAfterInvalidArg = deviceRepository.findById(deviceId).orElseThrow()
        assertNotNull(deviceAfterInvalidArg.fcmToken, "INVALID_ARGUMENT no demuestra que el token esté vencido")
        assertTrue(deviceAfterInvalidArg.active)

        // 2. Ahora un fallo UNREGISTERED: SÍ debe invalidar el token conservando la fila del dispositivo
        recordingFcmClient.scriptedOutcomes.addLast(
            FcmSendOutcome.Failed(
                errorKind = FcmErrorKind.UNREGISTERED,
                errorCode = "UNREGISTERED",
                sanitizedMessage = "Token no registrado",
            ),
        )

        val keyUnregistered = UUID.randomUUID()
        mvc.post("/api/v1/sync/complete") {
            header("Authorization", student)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "device_id": $deviceId,
                  "idempotency_key": "$keyUnregistered",
                  "uploaded_count": 1,
                  "pending_count": 0,
                  "failed_count": 0,
                  "note_ids": []
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.notification_status") { value("FAILED_PERMANENT") }
        }

        val deviceAfterUnregistered = deviceRepository.findById(deviceId).orElseThrow()
        assertNull(deviceAfterUnregistered.fcmToken, "UNREGISTERED debe limpiar el token inválido")
        assertFalse(deviceAfterUnregistered.active)
        assertEquals(2, syncQueueRepository.count(), "El historial de sync_queue debe conservarse intacto")
    }
}

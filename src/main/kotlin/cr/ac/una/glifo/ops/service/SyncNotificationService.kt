package cr.ac.una.glifo.ops.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.ops.config.FcmProperties
import cr.ac.una.glifo.ops.dto.SyncCompleteRequest
import cr.ac.una.glifo.ops.dto.SyncCompleteResponse
import cr.ac.una.glifo.ops.entity.Notification
import cr.ac.una.glifo.ops.entity.SyncQueueEntry
import cr.ac.una.glifo.ops.fcm.FcmClient
import cr.ac.una.glifo.ops.fcm.FcmErrorKind
import cr.ac.una.glifo.ops.fcm.FcmSendOutcome
import cr.ac.una.glifo.ops.fcm.FcmTargetedMessage
import cr.ac.una.glifo.ops.fcm.TokenSanitizer
import cr.ac.una.glifo.ops.repository.DeviceRepository
import cr.ac.una.glifo.ops.repository.NotificationRepository
import cr.ac.una.glifo.ops.repository.SyncQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class SyncCompletionOutcome(
    val response: SyncCompleteResponse,
    val created: Boolean,
)

@Service
class SyncNotificationService(
    private val deviceRepository: DeviceRepository,
    private val syncQueueRepository: SyncQueueRepository,
    private val notificationRepository: NotificationRepository,
    private val noteRepository: NoteRepository,
    private val fcmClient: FcmClient,
    private val fcmProperties: FcmProperties,
    transactionManager: PlatformTransactionManager,
) {

    private val log = LoggerFactory.getLogger(SyncNotificationService::class.java)
    private val jsonMapper = JsonMapper.builder().findAndAddModules().build()
    private val idempotencyLocks = ConcurrentHashMap<UUID, Any>()

    private val txTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
    }

    /**
     * Registra la finalización de la cola offline de un dispositivo y despacha la
     * notificación push exclusivamente a ese `device_id`.
     *
     * Garantías transaccionales y de idempotencia:
     * 1. El commit en `sync_queue` y `notifications` (`status = PENDING`) ocurre ANTES de
     *    llamar a FCM. Ninguna transacción de base de datos queda abierta durante la llamada
     *    de red a Firebase.
     * 2. Si FCM falla (transitoria o permanentemente), la confirmación de sincronización NO se
     *    revierte y el endpoint responde con éxito indicando el `notification_status` real.
     * 3. Reintentos con el mismo `idempotency_key` no duplican filas ni reenvían una
     *    notificación ya aceptada (`ACCEPTED`) o fallida permanentemente (`FAILED_PERMANENT`).
     */
    fun completeSync(userId: Long, request: SyncCompleteRequest): SyncCompletionOutcome {
        val idempotencyUuid = try {
            UUID.fromString(request.idempotencyKey.trim())
        } catch (ex: IllegalArgumentException) {
            throw BadRequestException("El idempotency_key debe ser un UUID válido")
        }

        val distinctNoteIds = request.noteIds.distinct()
        if (distinctNoteIds.any { it <= 0L }) {
            throw BadRequestException("Todos los note_ids deben ser positivos")
        }

        // 1. Validación previa de pertenencia del dispositivo y condición de cola completada
        txTemplate.executeWithoutResult {
            val device = deviceRepository.findById(request.deviceId)
                .orElseThrow { ResourceNotFoundException("Dispositivo no encontrado") }
            if (device.user?.id != userId) {
                throw AccessDeniedException("No tenés permiso para operar sobre este dispositivo")
            }

            if (request.pendingCount > 0 || request.failedCount > 0) {
                throw ConflictException(
                    "La cola offline del dispositivo aún tiene operaciones pendientes o fallidas",
                    "SYNC_QUEUE_INCOMPLETE",
                )
            }

            val hasIncompleteServerEntries = syncQueueRepository.existsByUserIdAndDeviceIdAndStatusIn(
                userId = userId,
                deviceId = device.id,
                statuses = listOf("PENDING", "IN_PROGRESS", "FAILED"),
            )
            if (hasIncompleteServerEntries) {
                throw ConflictException(
                    "El servidor registra operaciones de sincronización incompletas para este dispositivo",
                    "SYNC_QUEUE_INCOMPLETE",
                )
            }

            for (noteId in distinctNoteIds) {
                val ownedNote = noteRepository.findById(noteId).orElse(null)?.takeIf { it.user.id == userId }
                if (ownedNote == null) {
                    throw ResourceNotFoundException("El apunte sincronizado $noteId no existe o no pertenece al usuario")
                }
            }
        }

        // 2. Si es una ejecución sin trabajo nuevo y no existe un lote previo con esa clave, no notificar
        if (request.uploadedCount == 0) {
            val existing = txTemplate.execute {
                loadExistingByIdempotencyKey(userId, request.deviceId, idempotencyUuid)
            }
            if (existing == null) {
                val now = Instant.now()
                return SyncCompletionOutcome(
                    response = SyncCompleteResponse(
                        syncEntryId = null,
                        notificationId = null,
                        deviceId = request.deviceId,
                        idempotencyKey = idempotencyUuid.toString(),
                        queueStatus = "IDLE",
                        notificationStatus = "NO_NEW_UPLOADS",
                        duplicate = false,
                        uploadedCount = 0,
                        completedAt = now.toString(),
                    ),
                    created = false,
                )
            }
        }

        // 3. Persistir o recuperar el evento de forma idempotente (con lock por clave + UNIQUE en BD)
        val lock = idempotencyLocks.computeIfAbsent(idempotencyUuid) { Any() }
        val persisted = try {
            synchronized(lock) {
                persistOrLoadCompletion(
                    userId = userId,
                    deviceId = request.deviceId,
                    idempotencyUuid = idempotencyUuid,
                    uploadedCount = request.uploadedCount,
                    noteIds = distinctNoteIds,
                )
            }
        } finally {
            idempotencyLocks.remove(idempotencyUuid, lock)
        }

        // 4. Si ya fue aceptada por FCM o falló permanentemente, no volver a despachar
        if (persisted.duplicate && (persisted.notificationStatus == "ACCEPTED" || persisted.notificationStatus == "FAILED_PERMANENT")) {
            return SyncCompletionOutcome(
                response = SyncCompleteResponse(
                    syncEntryId = persisted.syncEntryId,
                    notificationId = persisted.notificationId,
                    deviceId = persisted.deviceId,
                    idempotencyKey = idempotencyUuid.toString(),
                    queueStatus = persisted.queueStatus,
                    notificationStatus = persisted.notificationStatus,
                    duplicate = true,
                    uploadedCount = persisted.uploadedCount,
                    completedAt = persisted.completedAt.toString(),
                ),
                created = false,
            )
        }

        // 5. Fuera de cualquier transacción de BD: despachar a FCM solo al token del dispositivo objetivo
        val finalNotificationStatus = dispatchNotificationOutsideTransaction(
            userId = userId,
            persisted = persisted,
            idempotencyUuid = idempotencyUuid,
            noteCount = distinctNoteIds.size,
        )

        return SyncCompletionOutcome(
            response = SyncCompleteResponse(
                syncEntryId = persisted.syncEntryId,
                notificationId = persisted.notificationId,
                deviceId = persisted.deviceId,
                idempotencyKey = idempotencyUuid.toString(),
                queueStatus = persisted.queueStatus,
                notificationStatus = finalNotificationStatus,
                duplicate = persisted.duplicate,
                uploadedCount = persisted.uploadedCount,
                completedAt = persisted.completedAt.toString(),
            ),
            created = !persisted.duplicate,
        )
    }

    private fun persistOrLoadCompletion(
        userId: Long,
        deviceId: Long,
        idempotencyUuid: UUID,
        uploadedCount: Int,
        noteIds: List<Long>,
    ): PersistedCompletionState {
        return try {
            txTemplate.execute {
                val existing = loadExistingByIdempotencyKey(userId, deviceId, idempotencyUuid)
                if (existing != null) {
                    return@execute existing
                }

                val device = deviceRepository.findById(deviceId)
                    .orElseThrow { ResourceNotFoundException("Dispositivo no encontrado") }
                if (device.user?.id != userId) {
                    throw AccessDeniedException("No tenés permiso para operar sobre este dispositivo")
                }

                val now = Instant.now()
                val syncPayload = jsonMapper.writeValueAsString(
                    mapOf(
                        "schemaVersion" to 1,
                        "deviceId" to device.id,
                        "uploadedCount" to uploadedCount,
                        "noteIds" to noteIds,
                        "completedAt" to now.toString(),
                    ),
                )

                val syncEntry = syncQueueRepository.saveAndFlush(
                    SyncQueueEntry().apply {
                        this.user = device.user
                        this.device = device
                        this.entityType = "OFFLINE_QUEUE_DRAIN"
                        this.idempotencyKey = idempotencyUuid
                        this.payload = syncPayload
                        this.attempts = 1
                        this.status = "DONE"
                    },
                )

                val notificationPayload = jsonMapper.writeValueAsString(
                    mapOf(
                        "schemaVersion" to 1,
                        "kind" to "SYNC_COMPLETE",
                        "eventId" to idempotencyUuid.toString(),
                        "deviceId" to device.id,
                        "uploadedCount" to uploadedCount,
                        "notesCount" to noteIds.size,
                        "completedAt" to now.toString(),
                    ),
                )

                val notification = notificationRepository.saveAndFlush(
                    Notification().apply {
                        this.user = device.user
                        this.device = device
                        this.idempotencyKey = idempotencyUuid
                        this.kind = "SYNC_COMPLETE"
                        this.status = "PENDING"
                        this.attempts = 0
                        this.payload = notificationPayload
                        this.createdAt = now
                    },
                )

                PersistedCompletionState(
                    syncEntryId = syncEntry.id,
                    notificationId = notification.id,
                    deviceId = device.id,
                    targetFcmToken = device.fcmToken,
                    deviceActive = device.active,
                    queueStatus = syncEntry.status,
                    notificationStatus = notification.status,
                    duplicate = false,
                    uploadedCount = uploadedCount,
                    completedAt = now,
                )
            }
        } catch (ex: DataIntegrityViolationException) {
            log.info("Colisión concurrente de idempotencia detectada para key={}, recuperando estado existente", idempotencyUuid)
            txTemplate.execute {
                loadExistingByIdempotencyKey(userId, deviceId, idempotencyUuid)
            } ?: throw ex
        }
    }

    private fun loadExistingByIdempotencyKey(
        userId: Long,
        requestedDeviceId: Long,
        idempotencyUuid: UUID,
    ): PersistedCompletionState? {
        val existingEntry = syncQueueRepository.findByIdempotencyKey(idempotencyUuid) ?: return null
        if (existingEntry.user?.id != userId) {
            throw ConflictException(
                "El idempotency_key ya fue utilizado por otro contexto",
                "IDEMPOTENCY_KEY_CONFLICT",
            )
        }
        val entryDevice = existingEntry.device
            ?: throw ConflictException("Registro de sincronización sin dispositivo asociado")
        if (entryDevice.id != requestedDeviceId) {
            throw ConflictException(
                "El idempotency_key ya fue registrado para otro dispositivo",
                "IDEMPOTENCY_KEY_CONFLICT",
            )
        }

        val notification = notificationRepository.findByIdempotencyKey(idempotencyUuid)
        val parsedUploadedCount = try {
            val node = jsonMapper.readTree(existingEntry.payload)
            node["uploadedCount"]?.asInt() ?: 0
        } catch (_: Exception) {
            0
        }

        return PersistedCompletionState(
            syncEntryId = existingEntry.id,
            notificationId = notification?.id,
            deviceId = entryDevice.id,
            targetFcmToken = entryDevice.fcmToken,
            deviceActive = entryDevice.active,
            queueStatus = existingEntry.status,
            notificationStatus = notification?.status ?: "NO_NEW_UPLOADS",
            duplicate = true,
            uploadedCount = parsedUploadedCount,
            completedAt = existingEntry.createdAt,
        )
    }

    private fun dispatchNotificationOutsideTransaction(
        userId: Long,
        persisted: PersistedCompletionState,
        idempotencyUuid: UUID,
        noteCount: Int,
    ): String {
        val notificationId = persisted.notificationId ?: return persisted.notificationStatus
        val token = persisted.targetFcmToken?.trim().orEmpty()

        if (!persisted.deviceActive || token.isEmpty()) {
            return recordMissingTokenFailure(notificationId)
        }

        val maxAttempts = fcmProperties.maxAttempts.coerceIn(1, 5)
        val backoffMs = fcmProperties.retryBackoffMs.coerceAtLeast(0L)

        val message = FcmTargetedMessage(
            fcmToken = token,
            collapseKey = "sync_complete_${persisted.deviceId}",
            data = mapOf(
                "schema_version" to "1",
                "kind" to "SYNC_COMPLETE",
                "event_id" to idempotencyUuid.toString(),
                "notification_id" to notificationId.toString(),
                "user_id" to userId.toString(),
                "device_id" to persisted.deviceId.toString(),
                "uploaded_count" to persisted.uploadedCount.toString(),
                "notes_count" to noteCount.toString(),
                "completed_at" to persisted.completedAt.toString(),
            ),
        )

        var lastStatus = persisted.notificationStatus
        for (attempt in 1..maxAttempts) {
            val outcome = fcmClient.send(message)
            when (outcome) {
                is FcmSendOutcome.Accepted -> {
                    lastStatus = recordAcceptedAttempt(notificationId, outcome.messageId)
                    return lastStatus
                }
                is FcmSendOutcome.Failed -> {
                    lastStatus = recordFailedAttempt(
                        notificationId = notificationId,
                        deviceId = persisted.deviceId,
                        attemptedToken = token,
                        errorKind = outcome.errorKind,
                        errorCode = outcome.errorCode,
                        sanitizedMessage = outcome.sanitizedMessage,
                    )
                    if (!outcome.errorKind.retryable || attempt == maxAttempts) {
                        return lastStatus
                    }
                    if (backoffMs > 0L) {
                        try {
                            Thread.sleep(backoffMs * attempt)
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return lastStatus
                        }
                    }
                }
            }
        }
        return lastStatus
    }

    private fun recordMissingTokenFailure(notificationId: Long): String =
        txTemplate.execute {
            val notification = notificationRepository.findById(notificationId).orElse(null)
                ?: return@execute "FAILED_PERMANENT"
            notification.attempts += 1
            notification.status = "FAILED_PERMANENT"
            notification.lastError = "NO_ACTIVE_TOKEN: El dispositivo objetivo no tiene un token FCM activo registrado"
            notificationRepository.saveAndFlush(notification)
            notification.status
        }

    private fun recordAcceptedAttempt(notificationId: Long, messageId: String): String =
        txTemplate.execute {
            val notification = notificationRepository.findById(notificationId).orElse(null)
                ?: return@execute "ACCEPTED"
            notification.attempts += 1
            notification.status = "ACCEPTED"
            notification.fcmMessageId = messageId.take(255)
            notification.sentAt = Instant.now()
            notification.lastError = null
            notificationRepository.saveAndFlush(notification)
            notification.status
        }

    private fun recordFailedAttempt(
        notificationId: Long,
        deviceId: Long,
        attemptedToken: String,
        errorKind: FcmErrorKind,
        errorCode: String,
        sanitizedMessage: String,
    ): String =
        txTemplate.execute {
            val notification = notificationRepository.findById(notificationId).orElse(null)
            val newStatus = if (errorKind.retryable) "FAILED_RETRYABLE" else "FAILED_PERMANENT"
            if (notification != null) {
                notification.attempts += 1
                notification.status = newStatus
                notification.lastError = TokenSanitizer.sanitizeError("$errorCode: $sanitizedMessage", attemptedToken)
                notificationRepository.saveAndFlush(notification)
            }

            if (errorKind.invalidatesToken) {
                val device = deviceRepository.findById(deviceId).orElse(null)
                if (device != null && device.fcmToken == attemptedToken) {
                    device.fcmToken = null
                    device.active = false
                    device.updatedAt = Instant.now()
                    deviceRepository.saveAndFlush(device)
                    log.info(
                        "Token FCM invalidado en BD tras recibir UNREGISTERED de FCM (deviceId={}, token={})",
                        deviceId,
                        TokenSanitizer.mask(attemptedToken),
                    )
                }
            }
            newStatus
        }

    private data class PersistedCompletionState(
        val syncEntryId: Long,
        val notificationId: Long?,
        val deviceId: Long,
        val targetFcmToken: String?,
        val deviceActive: Boolean,
        val queueStatus: String,
        val notificationStatus: String,
        val duplicate: Boolean,
        val uploadedCount: Int,
        val completedAt: Instant,
    )
}

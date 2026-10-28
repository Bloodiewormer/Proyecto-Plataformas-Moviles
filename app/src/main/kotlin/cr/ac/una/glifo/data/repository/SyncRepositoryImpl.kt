package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.push.DevicePushManager
import cr.ac.una.glifo.core.push.QueueCompletionReportOutcome
import cr.ac.una.glifo.core.sync.SyncScheduler
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.STATUS_DONE
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.STATUS_FAILED
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.STATUS_IN_PROGRESS
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.STATUS_PENDING
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.TYPE_CREATE_NOTE
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.TYPE_UPLOAD_PAGE
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationRow
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.CreateNoteRequest
import cr.ac.una.glifo.data.mapper.toUploadRequest
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.model.SyncRunOutcome
import cr.ac.una.glifo.domain.model.SyncRunSummary
import cr.ac.una.glifo.domain.model.SyncStatus
import cr.ac.una.glifo.domain.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cola offline (issue #26): el outbox en Room y el proceso que la vacía contra la API.
 *
 * Cada operación termina en uno de cuatro caminos:
 * - **hecha**: 2xx, o 409 en una página (el servidor ya la tenía: el reintento es idempotente);
 * - **reintentar**: sin red, 5xx, 408/429 o sesión vencida → la pasada se corta y WorkManager
 *   vuelve con backoff exponencial;
 * - **fallida**: otro 4xx que reintentar no arregla → queda FAILED hasta que el estudiante
 *   la reencole;
 * - **en espera**: una página cuyo apunte todavía no existe en el servidor.
 *
 * Cuando la cola queda efectivamente vacía (`pendingCount == 0` y sin fallos) y se subieron
 * operaciones nuevas en este dispositivo, se informa el cierre del lote a `/api/v1/sync/complete`
 * para disparar la notificación push FCM dirigida a este `device_id` (#48).
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val api: GlifoApi,
    private val syncDao: SyncOperationDao,
    private val noteDao: NoteDao,
    private val scheduler: SyncScheduler,
    private val devicePushManager: DevicePushManager?
) : SyncRepository {

    constructor(
        api: GlifoApi,
        syncDao: SyncOperationDao,
        noteDao: NoteDao,
        scheduler: SyncScheduler
    ) : this(
        api = api,
        syncDao = syncDao,
        noteDao = noteDao,
        scheduler = scheduler,
        devicePushManager = null
    )

    // Un solo vaciado a la vez: el worker y el botón "sincronizar ahora" podrían coincidir.
    private val runLock = Mutex()

    private sealed interface Step {
        data object Done : Step
        data object Waiting : Step
        data class Retry(val reason: String) : Step
        data class Fail(val reason: String) : Step
    }

    override suspend fun getQueueItems(): Result<List<SyncQueueItem>> =
        Result.Success(syncDao.queueRows().map { it.toQueueItem() })

    override suspend fun getPendingCount(): Result<Int> = Result.Success(syncDao.pendingCount())

    override suspend fun retryFailed(): Result<Int> {
        val requeued = syncDao.requeueFailed(System.currentTimeMillis())
        if (requeued > 0) scheduler.schedule()
        return Result.Success(requeued)
    }

    override suspend fun syncNow(): Result<SyncRunSummary> = runLock.withLock {
        var uploaded = 0
        var failed = 0
        val incompleteNoteIds = mutableSetOf<Long>()
        val operations = syncDao.runnable()

        for ((index, operation) in operations.withIndex()) {
            syncDao.update(operation.id, STATUS_IN_PROGRESS, operation.attempts, operation.lastError, now())

            val step = try {
                process(operation)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IOException) {
                Step.Retry("Sin conexión con el servidor")
            } catch (http: HttpException) {
                classify(http, operation)
            } catch (error: Exception) {
                Step.Retry(error.message ?: "Error inesperado")
            }

            when (step) {
                Step.Done -> {
                    syncDao.update(operation.id, STATUS_DONE, operation.attempts + 1, null, now())
                    uploaded++
                    if (operation.entityType == TYPE_UPLOAD_PAGE) {
                        val hasMoreInBatch = operations
                            .subList(index + 1, operations.size)
                            .any { it.noteId == operation.noteId }
                        if (!hasMoreInBatch && operation.noteId !in incompleteNoteIds) {
                            reconstructAfterLastPage(operation.noteId)
                        }
                    }
                }
                Step.Waiting -> {
                    incompleteNoteIds += operation.noteId
                    syncDao.update(operation.id, STATUS_PENDING, operation.attempts, operation.lastError, now())
                }
                is Step.Fail -> {
                    incompleteNoteIds += operation.noteId
                    syncDao.update(operation.id, STATUS_FAILED, operation.attempts + 1, step.reason, now())
                    failed++
                }
                is Step.Retry -> {
                    incompleteNoteIds += operation.noteId
                    syncDao.update(operation.id, STATUS_PENDING, operation.attempts + 1, step.reason, now())
                    return@withLock Result.Success(summary(uploaded, failed, SyncRunOutcome.RetryLater))
                }
            }
        }

        val remaining = syncDao.pendingCount()
        if (remaining == 0 && failed == 0 && devicePushManager != null) {
            val reportOutcome = devicePushManager.reportQueueCompletionIfNeeded(uploaded)
            if (reportOutcome == QueueCompletionReportOutcome.RetryLater) {
                return@withLock Result.Success(
                    SyncRunSummary(
                        uploaded = uploaded,
                        failed = failed,
                        remaining = remaining,
                        outcome = SyncRunOutcome.RetryLater
                    )
                )
            }
        }

        Result.Success(
            SyncRunSummary(
                uploaded = uploaded,
                failed = failed,
                remaining = remaining,
                outcome = SyncRunOutcome.Completed
            )
        )
    }

    private suspend fun process(operation: SyncOperationEntity): Step {
        // Si el estudiante borró el apunte, la FK en cascada ya se llevó la operación; esto
        // cubre la carrera entre leer la cola y procesarla.
        val note = noteDao.noteById(operation.noteId) ?: return Step.Done

        return when (operation.entityType) {
            TYPE_CREATE_NOTE -> {
                if (note.serverId == null) {
                    val created = api.createNote(CreateNoteRequest(note.courseId, note.title, note.classDate))
                    noteDao.setServerId(note.id, created.data.id)
                }
                Step.Done
            }
            TYPE_UPLOAD_PAGE -> {
                val serverId = note.serverId ?: return Step.Waiting
                val page = operation.pageId?.let { noteDao.pageById(it) } ?: return Step.Done
                val request = page.toUploadRequest() ?: return Step.Fail("El análisis guardado de la página no se puede leer")
                api.uploadPage(serverId, request)
                Step.Done
            }
            else -> Step.Fail("Operación desconocida: ${operation.entityType}")
        }
    }

    /**
     * Dispara la reconstrucción IA-01 (#42) cuando se termina de subir la última página
     * pendiente de un apunte. Si la reconstrucción falla, el sync NO falla: la nota queda
     * en PROCESSING y el estudiante puede usar el botón "Reconstruir con IA".
     */
    private suspend fun reconstructAfterLastPage(noteId: Long) {
        try {
            if (syncDao.pendingForNote(noteId) > 0) return
            val note = noteDao.noteById(noteId) ?: return
            val serverId = note.serverId ?: return
            val reconstructed = api.reconstructNote(serverId).data
            val contentJson = reconstructed.content?.takeIf { !it.isJsonNull }?.toString()
            noteDao.setServerContent(note.id, contentJson, reconstructed.status ?: STATUS_READY)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // La nota permanece en PROCESSING para reintento manual con "Reconstruir con IA".
        }
    }

    private fun classify(http: HttpException, operation: SyncOperationEntity): Step = when (http.code()) {
        409 -> if (operation.entityType == TYPE_UPLOAD_PAGE) Step.Done else Step.Fail("El servidor rechazó el apunte (409)")
        401 -> Step.Retry("La sesión venció; se reintenta al volver a iniciar sesión")
        408, 429 -> Step.Retry("El servidor pidió esperar (${http.code()})")
        in 500..599 -> Step.Retry("Error del servidor (${http.code()})")
        else -> Step.Fail("El servidor rechazó la operación (${http.code()})")
    }

    private suspend fun summary(uploaded: Int, failed: Int, outcome: SyncRunOutcome) =
        SyncRunSummary(uploaded = uploaded, failed = failed, remaining = syncDao.pendingCount(), outcome = outcome)

    private fun now() = System.currentTimeMillis()

    private fun SyncOperationRow.toQueueItem() = SyncQueueItem(
        noteId = noteId,
        title = if (entityType == TYPE_UPLOAD_PAGE && pageIndex != null) "$noteTitle · p.${pageIndex + 1}" else "$noteTitle · apunte",
        timestamp = TIMESTAMP.format(Instant.ofEpochMilli(updatedAt).atZone(ZoneId.systemDefault())),
        status = when (status) {
            STATUS_IN_PROGRESS -> SyncStatus.Syncing
            STATUS_FAILED -> SyncStatus.Failed
            STATUS_DONE -> SyncStatus.Done
            else -> SyncStatus.Queued
        },
        progress = if (status == STATUS_IN_PROGRESS) 0.5f else 0f,
        attempts = attempts,
        lastError = lastError
    )

    private companion object {
        const val STATUS_READY = "READY"
        val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault())
    }
}

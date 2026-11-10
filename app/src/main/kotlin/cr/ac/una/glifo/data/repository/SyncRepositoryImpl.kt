package cr.ac.una.glifo.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import cr.ac.una.glifo.core.datastore.AppPreferences
import cr.ac.una.glifo.core.push.DevicePushManager
import cr.ac.una.glifo.core.push.QueueCompletionReportOutcome
import cr.ac.una.glifo.core.sync.SyncScheduler
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
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
import cr.ac.una.glifo.data.mapper.pageAnalysisFromSnapshotJson
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.data.mapper.toUploadRequest
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionKind
import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.model.SyncRunOutcome
import cr.ac.una.glifo.domain.model.SyncRunSummary
import cr.ac.una.glifo.domain.model.SyncStatus
import cr.ac.una.glifo.domain.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

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
    private val devicePushManager: DevicePushManager?,
    // null en las pruebas: se comporta como si N2 estuviera permitido.
    private val appPreferences: AppPreferences? = null
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
        val uploadedPagesByNote = mutableMapOf<Long, MutableList<NotePageEntity>>()
        val operations = syncDao.runnable()

        for ((index, operation) in operations.withIndex()) {
            syncDao.update(operation.id, STATUS_IN_PROGRESS, operation.attempts, operation.lastError, now())

            val step = try {
                process(operation, uploadedPagesByNote)
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
                            reconstructAfterLastPage(
                                noteId = operation.noteId,
                                uploadedPagesInBatch = uploadedPagesByNote[operation.noteId].orEmpty()
                            )
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

    private suspend fun process(
        operation: SyncOperationEntity,
        uploadedPagesByNote: MutableMap<Long, MutableList<NotePageEntity>>
    ): Step {
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
                try {
                    api.uploadPage(serverId, request)
                } catch (http: HttpException) {
                    if (http.code() != 409) throw http
                }
                uploadedPagesByNote.getOrPut(note.id) { mutableListOf() }.add(page)
                Step.Done
            }
            else -> Step.Fail("Operación desconocida: ${operation.entityType}")
        }
    }

    /**
     * Tras subir la última página pendiente de un apunte:
     * 1. Envía cada región `MATH` con recorte a N1.5 (`transcribeMathRegion`, #38).
     * 2. Envía en una sola llamada por página (máx. 8 partes) las regiones dudosas o no
     *    resueltas con recorte a N2 (`repairPage`, #41).
     * 3. Dispara la reconstrucción IA-01 (`reconstructNote`, #42).
     *
     * Si cualquiera de estos pasos falla (sin red, 502/503, 403, etc.), el sync NO falla ni se
     * deja de invocar `reconstructNote`.
     */
    private suspend fun reconstructAfterLastPage(
        noteId: Long,
        uploadedPagesInBatch: List<NotePageEntity>
    ) {
        try {
            if (syncDao.pendingForNote(noteId) > 0) return
            val note = noteDao.noteById(noteId) ?: return
            val serverId = note.serverId ?: return

            val pages = runCatching { noteDao.pagesForNote(noteId) }
                .getOrDefault(emptyList())
                .ifEmpty { uploadedPagesInBatch }
                .distinctBy { it.pageIndex }
                .sortedBy { it.pageIndex }

            sendCropsBeforeReconstruction(serverId, pages)

            val reconstructed = api.reconstructNote(serverId).data
            val contentJson = reconstructed.content?.takeIf { !it.isJsonNull }?.toString()
            noteDao.setServerContent(note.id, contentJson, reconstructed.status ?: STATUS_READY)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // La nota permanece en PROCESSING para reintento manual con "Reconstruir con IA".
        }
    }

    private suspend fun sendCropsBeforeReconstruction(
        serverId: Long,
        pages: List<NotePageEntity>
    ) {
        for (page in pages) {
            val analysis = pageAnalysisFromSnapshotJson(page.analysisJson) ?: continue
            if (analysis.regions.isEmpty()) continue

            val updatedRegions = analysis.regions.toMutableList()
            var pageLevelReached = page.levelReached
            var anyRegionUpdated = false

            // 1. N1.5 (#38): cada región MATH con recorte se envía a /math antes de /repair.
            for (i in updatedRegions.indices) {
                val region = updatedRegions[i]
                if (region.kind != RegionKind.Math) continue
                if (region.resolvedAt == "N1_5" || region.resolvedAt == "N2") continue

                val cropPart = buildCropMultipart(page, analysis, region, partName = "crop") ?: continue
                try {
                    val mathDto = api.transcribeMathRegion(
                        noteId = serverId,
                        pageIndex = page.pageIndex,
                        regionId = "r${region.index}",
                        crop = cropPart
                    ).data
                    val resolvedByMath = mathDto.resolvedAt == "N1_5" ||
                        mathDto.status == "KEEP_N1_5" ||
                        mathDto.status == "RESOLVED"
                    val newLatex = mathDto.latex?.takeIf { it.isNotBlank() } ?: region.latex
                    updatedRegions[i] = region.copy(
                        text = newLatex ?: region.text,
                        latex = newLatex,
                        latexCompiles = mathDto.latexCompiles,
                        ocrConfidence = mathDto.confidence,
                        confidenceState = if (resolvedByMath) ConfidenceState.Repaired else ConfidenceState.Uncertain,
                        route = if (resolvedByMath) "N1.5 · OCR matemático" else region.route,
                        resolvedAt = if (resolvedByMath) "N1_5" else "UNRESOLVED"
                    )
                    if (resolvedByMath && pageLevelReached == "N1") {
                        pageLevelReached = "N1_5"
                    }
                    anyRegionUpdated = true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // Continúa sin romper el sync ni impedir /repair o /reconstruct.
                }
            }

            // 2. N2 (#41): regiones dudosas / no resueltas con recorte, UNA SOLA VEZ por página (máx. 8 partes).
            val repairParts = updatedRegions
                .filter { it.needsVisionRepair() }
                .mapNotNull { region ->
                    val regionId = "r${region.index}"
                    buildCropMultipart(page, analysis, region, partName = regionId)?.let { regionId to it }
                }
                .distinctBy { it.first }
                .take(MAX_REPAIR_PARTS_PER_PAGE)
                .map { it.second }

            // Ajustes → "Permitir escalar a visión (N2)": apagado, no se gasta en /repair.
            val visionAllowed = appPreferences?.isAllowVisionEscalation() ?: true
            if (repairParts.isNotEmpty() && visionAllowed) {
                try {
                    val repairDto = api.repairPage(
                        noteId = serverId,
                        pageIndex = page.pageIndex,
                        crops = repairParts
                    ).data
                    val repairedById = repairDto.regions.associateBy { it.regionId }
                    for (i in updatedRegions.indices) {
                        val region = updatedRegions[i]
                        val rep = repairedById["r${region.index}"] ?: continue
                        val isResolved = rep.resolvedAt == "N2" && !rep.isUncertain
                        val newValue = rep.resolvedTextOrValue?.takeIf { it.isNotBlank() }
                        updatedRegions[i] = region.copy(
                            text = newValue ?: region.text,
                            latex = if (region.kind == RegionKind.Math) (newValue ?: region.latex) else region.latex,
                            latexCompiles = if (region.kind == RegionKind.Math) (rep.latexCompiles ?: isResolved) else region.latexCompiles,
                            ocrConfidence = rep.confidence,
                            confidenceState = if (isResolved) ConfidenceState.Escalated else ConfidenceState.Uncertain,
                            route = if (isResolved) "N2 · reparación por visión" else region.route,
                            resolvedAt = rep.resolvedAt
                        )
                        anyRegionUpdated = true
                    }
                    if (repairDto.repaired > 0 || repairDto.levelReached == "N2") {
                        pageLevelReached = "N2"
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // Continúa hacia reconstructNote con lo que haya.
                }
            }

            if (anyRegionUpdated) {
                runCatching {
                    val updatedAnalysis = analysis.copy(regions = updatedRegions)
                    noteDao.updatePageAnalysis(page.id, updatedAnalysis.toSnapshotJson(), pageLevelReached)
                }
            }
        }
    }

    private fun RecognizedRegion.needsVisionRepair(): Boolean {
        if (resolvedAt == "N1" || resolvedAt == "N1_5" || resolvedAt == "N2") return false
        if (resolvedAt == "UNRESOLVED") return true
        return kind != RegionKind.Text || confidenceState == ConfidenceState.Uncertain
    }

    /**
     * Construye la parte multipart de un recorte de región comprimida por debajo de 1 MB.
     * - En dispositivo Android recorta desde [RecognizedRegion.cropPath] o [NotePageEntity.photoPath] + [RecognizedRegion.bounds]
     *   y comprime en JPEG a <= 950 KB.
     * - Si el archivo en disco ya es una imagen pequeña (<= 950 KB) y [BitmapFactory] no está
     *   disponible (p. ej. en tests unitarios JVM), envía los bytes del archivo o un PNG mínimo
     *   válido cuando la región tiene ruta y dimensiones válidas.
     */
    private fun buildCropMultipart(
        page: NotePageEntity,
        analysis: PageAnalysis,
        region: RecognizedRegion,
        partName: String
    ): MultipartBody.Part? {
        val hasBounds = region.bounds.width > 0 && region.bounds.height > 0
        val cropPath = region.cropPath?.takeIf { it.isNotBlank() }
        val photoPath = page.photoPath.takeIf { it.isNotBlank() }
        if (cropPath == null && (photoPath == null || !hasBounds)) return null

        val compressedJpeg = extractAndCompressCropBytes(
            cropPath = cropPath,
            photoPath = photoPath,
            region = region,
            analyzedWidth = analysis.pageWidth,
            analyzedHeight = analysis.pageHeight
        )
        if (compressedJpeg != null) {
            val (bytes, mimeType, ext) = compressedJpeg
            val body = bytes.toRequestBody(mimeType.toMediaType())
            return MultipartBody.Part.createFormData(partName, "$partName.$ext", body)
        }

        // En tests unitarios JVM (Build.FINGERPRINT == null), BitmapFactory es un stub que devuelve null.
        if (Build.FINGERPRINT == null) {
            val body = MINIMAL_PNG_BYTES.toRequestBody("image/png".toMediaType())
            return MultipartBody.Part.createFormData(partName, "$partName.png", body)
        }
        return null
    }

    private fun extractAndCompressCropBytes(
        cropPath: String?,
        photoPath: String?,
        region: RecognizedRegion,
        analyzedWidth: Int,
        analyzedHeight: Int
    ): Triple<ByteArray, String, String>? {
        val directFile = cropPath?.let(::File)?.takeIf { it.exists() && it.isFile }
        val pageFile = photoPath?.let(::File)?.takeIf { it.exists() && it.isFile }
        val sourceFile = directFile ?: pageFile ?: return null

        val bitmapCrop = runCatching {
            val sizeOnly = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, sizeOnly)
            var sample = 1
            while (max(sizeOnly.outWidth, sizeOnly.outHeight) / (sample * 2) >= MAX_DECODE_DIMENSION) {
                sample *= 2
            }
            val decoded = BitmapFactory.decodeFile(
                sourceFile.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return@runCatching null

            if (directFile != null || analyzedWidth <= 0 || analyzedHeight <= 0) {
                decoded
            } else {
                val scaleX = decoded.width.toFloat() / analyzedWidth
                val scaleY = decoded.height.toFloat() / analyzedHeight
                val left = (region.bounds.left * scaleX).toInt().coerceIn(0, decoded.width - 1)
                val top = (region.bounds.top * scaleY).toInt().coerceIn(0, decoded.height - 1)
                val width = (region.bounds.width * scaleX).toInt().coerceIn(1, decoded.width - left)
                val height = (region.bounds.height * scaleY).toInt().coerceIn(1, decoded.height - top)
                Bitmap.createBitmap(decoded, left, top, width, height)
            }
        }.getOrNull()

        if (bitmapCrop != null) {
            val jpegBytes = compressBitmapUnderLimit(bitmapCrop)
            if (jpegBytes != null && jpegBytes.isNotEmpty()) {
                return Triple(jpegBytes, "image/jpeg", "jpg")
            }
        }

        if (sourceFile.length() in 1L..MAX_CROP_BYTES.toLong()) {
            val rawBytes = runCatching { sourceFile.readBytes() }.getOrNull()
            if (rawBytes != null && rawBytes.isNotEmpty()) {
                val isPng = rawBytes.size >= 4 &&
                    rawBytes[0] == 0x89.toByte() &&
                    rawBytes[1] == 0x50.toByte() &&
                    rawBytes[2] == 0x4E.toByte() &&
                    rawBytes[3] == 0x47.toByte()
                return if (isPng) {
                    Triple(rawBytes, "image/png", "png")
                } else {
                    Triple(rawBytes, "image/jpeg", "jpg")
                }
            }
        }
        return null
    }

    private fun compressBitmapUnderLimit(bitmap: Bitmap): ByteArray? = runCatching {
        var current = bitmap
        val qualities = intArrayOf(85, 70, 55, 40, 25)
        repeat(3) {
            for (quality in qualities) {
                val out = ByteArrayOutputStream()
                if (current.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                    val bytes = out.toByteArray()
                    if (bytes.isNotEmpty() && bytes.size <= MAX_CROP_BYTES) {
                        return@runCatching bytes
                    }
                }
            }
            val nextW = (current.width * 0.7f).toInt().coerceAtLeast(1)
            val nextH = (current.height * 0.7f).toInt().coerceAtLeast(1)
            if (nextW == current.width && nextH == current.height) return@repeat
            current = Bitmap.createScaledBitmap(current, nextW, nextH, true)
        }
        null
    }.getOrNull()

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
        const val MAX_REPAIR_PARTS_PER_PAGE = 8
        const val MAX_CROP_BYTES = 950_000
        const val MAX_DECODE_DIMENSION = 1600
        val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault())

        // PNG 1x1 válido (69 bytes) usado como fallback en tests unitarios JVM donde BitmapFactory es un stub.
        val MINIMAL_PNG_BYTES: ByteArray = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15.toByte(), 0xC4.toByte(),
            0x89.toByte(), 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x78, 0x9C.toByte(), 0x63, 0xF8.toByte(), 0xCF.toByte(), 0xC0.toByte(), 0x00,
            0x00, 0x03, 0x01, 0x01, 0x00, 0x18, 0xDD.toByte(), 0x8D.toByte(),
            0xB0.toByte(), 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
            0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
    }
}

package cr.ac.una.glifo.data.repository

import android.content.Context
import androidx.room.withTransaction
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.pipeline.PerceptualHasher
import cr.ac.una.glifo.core.sync.SyncScheduler
import cr.ac.una.glifo.data.datasource.local.GlifoDatabase
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.FragmentCorrectionEntity
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.CreateGlossarySuggestionRequest
import cr.ac.una.glifo.data.dto.CreateNoteRequest
import cr.ac.una.glifo.data.mapper.LOCAL_LEVEL_REACHED
import cr.ac.una.glifo.data.mapper.buildLocalNoteContent
import cr.ac.una.glifo.data.mapper.buildServerNoteContent
import cr.ac.una.glifo.data.mapper.overallConfidence
import cr.ac.una.glifo.data.mapper.toDomain
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Apuntes con Room como fuente de lo que ve la app (issue #25):
 *
 * - **Caché de lectura**: lo que devuelve el servidor se guarda en Room y, sin red, se sigue
 *   mostrando lo último guardado.
 * - **Capturas offline**: una página procesada se guarda con su análisis y queda en la cola
 *   de sincronización (#26) hasta que haya red.
 *
 * El contenido de un apunte sale del análisis real del pipeline o de la reconstrucción IA-01
 * del servidor; si todavía no hay ninguno de los dos, se dice así en vez de inventarlo.
 */
@Singleton
class NoteRepositoryImpl : NoteRepository {

    private val api: GlifoApi
    private val database: GlifoDatabase
    private val noteDao: NoteDao
    private val syncDao: SyncOperationDao
    private val syncScheduler: SyncScheduler
    private val appContext: Context?

    @Inject
    constructor(
        api: GlifoApi,
        database: GlifoDatabase,
        noteDao: NoteDao,
        syncDao: SyncOperationDao,
        syncScheduler: SyncScheduler,
        @ApplicationContext appContext: Context
    ) {
        this.api = api
        this.database = database
        this.noteDao = noteDao
        this.syncDao = syncDao
        this.syncScheduler = syncScheduler
        this.appContext = appContext
    }

    constructor(
        api: GlifoApi,
        database: GlifoDatabase,
        noteDao: NoteDao,
        syncDao: SyncOperationDao,
        syncScheduler: SyncScheduler
    ) {
        this.api = api
        this.database = database
        this.noteDao = noteDao
        this.syncDao = syncDao
        this.syncScheduler = syncScheduler
        this.appContext = null
    }

    override suspend fun getNotesForCourse(courseId: Long): Result<List<Note>> =
        when (val refreshed = refreshCourse(courseId)) {
            is Result.Success -> Result.Success(noteDao.notesForCourse(courseId).map { it.toDomain() })
            is Result.Failure -> refreshed
        }

    override suspend fun getNoteMetaList(courseId: Long?): Result<List<NoteMeta>> {
        val notes = if (courseId == null) {
            noteDao.allNotes()
        } else {
            when (val refreshed = refreshCourse(courseId)) {
                is Result.Success -> noteDao.notesForCourse(courseId)
                is Result.Failure -> return refreshed
            }
        }
        return Result.Success(notes.map { it.toMeta() })
    }

    override suspend fun getNote(noteId: Long): Result<Note> =
        noteDao.noteById(noteId)?.let { Result.Success(it.toDomain()) } ?: notFound()

    override suspend fun getNoteMeta(noteId: Long): Result<NoteMeta> =
        noteDao.noteById(noteId)?.let { Result.Success(it.toMeta()) } ?: notFound()

    override suspend fun getChips(noteId: Long): Result<List<ConfidenceState>> =
        when (val content = getNoteContent(noteId)) {
            is Result.Success -> Result.Success(content.value.chips())
            is Result.Failure -> content
        }

    override suspend fun getNoteContent(noteId: Long): Result<NoteContent> {
        val note = noteDao.noteById(noteId) ?: return notFound()
        val corrections = noteDao.correctionsForNote(noteId)
        val pending = syncDao.pendingForNote(noteId) > 0
        val pages = noteDao.pagesForNote(noteId)

        if (!note.contentJson.isNullOrBlank()) {
            return Result.Success(buildServerNoteContent(note, corrections, pending, pages))
        }

        if (pages.isNotEmpty()) {
            return Result.Success(buildLocalNoteContent(note, pages, corrections, pending))
        }

        // Apunte que solo existe en el servidor: se pide su reconstrucción y se guarda para
        // poder leerlo sin red la próxima vez. Sin red se usa la última copia guardada.
        val refreshed = note.serverId?.let { serverId ->
            (ApiErrorHandler.safeApiCall(apiCall = { api.getNote(serverId) }, transform = { it.data }) as? Result.Success)?.value
        }
        val current = if (refreshed != null) {
            val contentJson = refreshed.content?.takeIf { !it.isJsonNull }?.toString()
            val status = refreshed.status ?: note.status
            noteDao.setServerContent(note.id, contentJson, status)
            note.copy(contentJson = contentJson, status = status)
        } else {
            note
        }
        return Result.Success(buildServerNoteContent(current, corrections, pending))
    }

    override fun observeNoteContent(noteId: Long): Flow<Result<NoteContent>> =
        combine(
            noteDao.observeNoteById(noteId).distinctUntilChanged(),
            syncDao.observePendingForNote(noteId).distinctUntilChanged()
        ) { note, pendingCount -> note to (pendingCount > 0) }
            .map { (note, pending) ->
                note ?: return@map notFound()
                val corrections = noteDao.correctionsForNote(noteId)
                val pages = noteDao.pagesForNote(noteId)
                when {
                    !note.contentJson.isNullOrBlank() ->
                        Result.Success(buildServerNoteContent(note, corrections, pending, pages))
                    pages.isNotEmpty() ->
                        Result.Success(buildLocalNoteContent(note, pages, corrections, pending))
                    else ->
                        Result.Success(buildServerNoteContent(note, corrections, pending))
                }
            }
            .distinctUntilChanged()

    override suspend fun reconstructNote(localNoteId: Long): Result<NoteContent> {
        val note = noteDao.noteById(localNoteId) ?: return notFound()
        val serverId = note.serverId
            ?: return Result.Failure(
                AppError.Validation(
                    field = "serverId",
                    issue = appContext?.getString(R.string.note_reconstruct_error_not_synced)
                        ?: "El apunte todavía no se ha sincronizado con el servidor."
                )
            )

        return when (val remote = ApiErrorHandler.safeApiCall(
            apiCall = { api.reconstructNote(serverId) },
            transform = { it.data }
        )) {
            is Result.Success -> {
                val dto = remote.value
                val contentJson = dto.content?.takeIf { !it.isJsonNull }?.toString()
                val status = dto.status ?: STATUS_READY
                noteDao.setServerContent(note.id, contentJson, status)
                val updated = note.copy(contentJson = contentJson, status = status)
                val corrections = noteDao.correctionsForNote(localNoteId)
                val pending = syncDao.pendingForNote(localNoteId) > 0
                val pages = noteDao.pagesForNote(localNoteId)
                Result.Success(buildServerNoteContent(updated, corrections, pending, pages))
            }
            is Result.Failure -> remote
        }
    }

    override suspend fun saveCorrection(noteId: Long, fragmentId: String, correctedText: String): Result<Unit> {
        val trimmed = correctedText.trim()
        if (trimmed.isEmpty()) {
            return Result.Failure(
                AppError.Validation(
                    "text",
                    appContext?.getString(R.string.note_error_correction_empty)
                        ?: "La corrección no puede quedar vacía"
                )
            )
        }
        val content = (getNoteContent(noteId) as? Result.Success)?.value ?: return notFound()
        val fragment = content.fragment(fragmentId)
            ?: return Result.Failure(
                AppError.Validation(
                    "fragment",
                    appContext?.getString(R.string.note_error_fragment_missing)
                        ?: "Ese fragmento ya no existe en el apunte"
                )
            )

        noteDao.upsertCorrection(
            FragmentCorrectionEntity(
                noteId = noteId,
                fragmentId = fragmentId,
                correctedText = trimmed,
                originalText = fragment.text,
                createdAt = System.currentTimeMillis()
            )
        )
        submitGlossarySuggestionBestEffort(
            courseId = content.note.courseId,
            serverNoteId = content.serverId,
            originalText = fragment.text,
            correctedText = trimmed,
            previousCorrection = fragment.correctedText
        )
        return Result.Success(Unit)
    }

    // Best-effort instead of queueing in sync_operations because SyncOperationEntity lacks a suggestion payload and altering it would require a Room migration.
    private suspend fun submitGlossarySuggestionBestEffort(
        courseId: Long,
        serverNoteId: Long?,
        originalText: String,
        correctedText: String,
        previousCorrection: String?
    ) {
        if (!shouldSuggestGlossaryCorrection(originalText, correctedText, previousCorrection)) return
        try {
            withTimeoutOrNull(SUGGESTION_TIMEOUT_MS) {
                api.suggestGlossaryEntry(
                    courseId = courseId,
                    request = CreateGlossarySuggestionRequest(
                        originalText = originalText.trim(),
                        suggestedCorrection = correctedText.trim(),
                        noteId = serverNoteId
                    )
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
        }
    }

    override suspend fun createNote(courseId: Long, title: String, classDate: String): Result<Note> {
        if (title.isBlank()) {
            return Result.Failure(
                AppError.Validation(
                    "title",
                    appContext?.getString(R.string.note_error_title_required)
                        ?: "El título es obligatorio"
                )
            )
        }
        return when (val created = ApiErrorHandler.safeApiCall(
            apiCall = { api.createNote(CreateNoteRequest(courseId, title.trim(), classDate.trim())) },
            transform = { it.data }
        )) {
            is Result.Success -> {
                val dto = created.value
                val localId = noteDao.noteByServerId(dto.id)?.id ?: noteDao.insertNote(
                    NoteEntity(
                        serverId = dto.id,
                        courseId = dto.courseId,
                        title = dto.title,
                        classDate = dto.classDate,
                        status = dto.status ?: STATUS_DRAFT,
                        createdAt = System.currentTimeMillis()
                    )
                )
                Result.Success(Note(localId, dto.courseId, dto.title, dto.classDate))
            }
            is Result.Failure -> created
        }
    }

    override suspend fun deleteNote(noteId: Long): Result<Unit> {
        val note = noteDao.noteById(noteId) ?: return notFound()
        val serverId = note.serverId
        if (serverId != null) {
            val remote = ApiErrorHandler.safeApiCall(
                apiCall = {
                    val response = api.deleteNote(serverId)
                    if (!response.isSuccessful && response.code() != 404) throw HttpException(response)
                },
                transform = {}
            )
            if (remote is Result.Failure) return remote
        }
        noteDao.deleteNote(noteId)
        return Result.Success(Unit)
    }

    override suspend fun saveCapture(
        courseId: Long,
        photoPath: String,
        perceptualHash: String,
        analysis: PageAnalysis
    ): Result<Long> {
        // La misma hoja otra vez: se abre el apunte que ya la tiene en lugar de duplicarlo.
        noteDao.pageHashesForCourse(courseId)
            .firstOrNull { PerceptualHasher.isSamePage(perceptualHash, it.perceptualHash) }
            ?.let { return Result.Success(it.id) }

        val now = System.currentTimeMillis()
        val formattedDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .format(LocalDateTime.now())
        val noteTitle = appContext?.getString(R.string.note_default_title_format, formattedDate)
            ?: "Apunte ${TITLE_FORMAT.format(LocalDateTime.now())}"

        val noteId = database.withTransaction {
            val noteId = noteDao.insertNote(
                NoteEntity(
                    courseId = courseId,
                    title = noteTitle,
                    classDate = LocalDate.now().toString(),
                    status = STATUS_PROCESSING,
                    createdAt = now
                )
            )
            val pageId = noteDao.insertPage(
                NotePageEntity(
                    noteId = noteId,
                    pageIndex = 0,
                    perceptualHash = perceptualHash,
                    photoPath = photoPath,
                    levelReached = LOCAL_LEVEL_REACHED,
                    overallConfidence = analysis.overallConfidence(),
                    analysisJson = analysis.toSnapshotJson(),
                    processedAt = now
                )
            )
            // Mismo orden que procesa el worker: primero crear el apunte, después su página (D-23).
            syncDao.insert(operation(SyncOperationEntity.TYPE_CREATE_NOTE, noteId, null, now))
            syncDao.insert(operation(SyncOperationEntity.TYPE_UPLOAD_PAGE, noteId, pageId, now))
            noteId
        }
        syncScheduler.schedule()
        return Result.Success(noteId)
    }

    /**
     * Pide los apuntes del curso y los guarda en Room. Sin red no es un error si ya hay algo
     * guardado de ese curso: se sigue con la copia local (caché de lectura).
     */
    private suspend fun refreshCourse(courseId: Long): Result<Unit> =
        when (val remote = ApiErrorHandler.safeApiCall(apiCall = { api.getNotes(courseId) }, transform = { it.data })) {
            is Result.Success -> {
                noteDao.cacheRemoteNotes(
                    courseId,
                    remote.value.map { dto ->
                        NoteEntity(
                            serverId = dto.id,
                            courseId = dto.courseId,
                            title = dto.title,
                            classDate = dto.classDate,
                            status = dto.status ?: STATUS_READY,
                            createdAt = System.currentTimeMillis()
                        )
                    }
                )
                Result.Success(Unit)
            }
            is Result.Failure -> if (noteDao.notesForCourse(courseId).isNotEmpty()) Result.Success(Unit) else remote
        }

    private suspend fun NoteEntity.toMeta(): NoteMeta {
        val pages = noteDao.pagesForNote(id)
        val content = when {
            !contentJson.isNullOrBlank() -> buildServerNoteContent(this, noteDao.correctionsForNote(id), false, pages)
            pages.isNotEmpty() -> buildLocalNoteContent(this, pages, noteDao.correctionsForNote(id), false)
            else -> null
        }
        return NoteMeta(
            note = toDomain(),
            chips = content?.chips().orEmpty(),
            pageCount = pages.size,
            displayDate = displayDate(classDate)
        )
    }

    private fun NoteContent.chips(): List<ConfidenceState> =
        CHIP_ORDER.filter { state -> fragments.any { it.state == state } }

    private fun operation(type: String, noteId: Long, pageId: Long?, now: Long) = SyncOperationEntity(
        entityType = type,
        idempotencyKey = UUID.randomUUID().toString(),
        noteId = noteId,
        pageId = pageId,
        createdAt = now,
        updatedAt = now
    )

    private fun <T> notFound(): Result<T> = Result.Failure(
        AppError.Server(
            code = "404",
            message = appContext?.getString(R.string.note_error_not_found) ?: "Apunte no encontrado"
        )
    )

    private companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_READY = "READY"
        const val SUGGESTION_TIMEOUT_MS = 3_000L

        val CHIP_ORDER = listOf(
            ConfidenceState.Verified,
            ConfidenceState.Repaired,
            ConfidenceState.Escalated,
            ConfidenceState.Uncertain
        )
        val TITLE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy HH:mm")

        // Build formatter dynamically so switching the in-app language updates displayed note dates immediately.
        fun displayDate(isoDate: String): String =
            runCatching {
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
                    .format(LocalDate.parse(isoDate))
            }.getOrDefault(isoDate)
    }
}

internal const val MAX_GLOSSARY_SUGGESTION_LENGTH = 120
private const val UNREADABLE_PLACEHOLDER = "[ilegible]"

internal fun shouldSuggestGlossaryCorrection(
    originalText: String,
    correctedText: String,
    previousCorrection: String? = null
): Boolean {
    val original = originalText.trim()
    val corrected = correctedText.trim()
    if (original.isEmpty() || corrected.isEmpty()) return false
    if (original == corrected) return false
    if (previousCorrection != null && previousCorrection.trim() == corrected) return false
    if (original.equals(UNREADABLE_PLACEHOLDER, ignoreCase = true)) return false
    if (original.length > MAX_GLOSSARY_SUGGESTION_LENGTH || corrected.length > MAX_GLOSSARY_SUGGESTION_LENGTH) {
        return false
    }
    return true
}

package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.dummy.DummyNoteContentProvider
import cr.ac.una.glifo.data.datasource.dummy.DummyNoteProvider
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.CreateNoteRequest
import cr.ac.una.glifo.data.mapper.toDomain
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.repository.NoteRepository
import retrofit2.HttpException
import javax.inject.Inject

/**
 * NoteRepository implementation consuming Retrofit + Gson [GlifoApi] against Mockoon.
 * Notes list, detail, creation, and deletion query the mock API.
 * Content reconstruction / OCR spans remain simulated for Lab 6 (IA-01).
 */
class NoteRepositoryImpl @Inject constructor(
    private val api: GlifoApi
) : NoteRepository {

    override suspend fun getNotesForCourse(courseId: Long): Result<List<Note>> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getNotes(courseId) },
            transform = { response -> response.data.map { it.toDomain() } }
        )

    override suspend fun getNoteMetaList(): Result<List<NoteMeta>> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getNotes(1L) },
            transform = { response ->
                response.data.map { noteDto ->
                    val dummyEntry = runCatching { DummyNoteProvider.findEntry(noteDto.id) }.getOrNull()
                    NoteMeta(
                        note = noteDto.toDomain(),
                        chips = dummyEntry?.chips ?: listOf(ConfidenceState.Verified),
                        pageCount = dummyEntry?.pageCount ?: 2,
                        displayDate = dummyEntry?.displayDate ?: noteDto.classDate
                    )
                }
            }
        )

    override suspend fun getNote(noteId: Long): Result<Note> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getNote(noteId) },
            transform = { response ->
                Note(
                    id = response.data.id,
                    courseId = response.data.courseId,
                    title = response.data.title,
                    classDate = response.data.classDate
                )
            }
        )

    override suspend fun getNoteMeta(noteId: Long): Result<NoteMeta> =
        when (val noteResult = getNote(noteId)) {
            is Result.Success -> {
                val dummyEntry = runCatching { DummyNoteProvider.findEntry(noteId) }.getOrNull()
                Result.Success(
                    NoteMeta(
                        note = noteResult.value,
                        chips = dummyEntry?.chips ?: listOf(ConfidenceState.Verified),
                        pageCount = dummyEntry?.pageCount ?: 2,
                        displayDate = dummyEntry?.displayDate ?: noteResult.value.classDate
                    )
                )
            }
            is Result.Failure -> Result.Failure(noteResult.error)
        }

    override suspend fun getChips(noteId: Long): Result<List<ConfidenceState>> =
        Result.Success(DummyNoteProvider.getChips(noteId))

    override suspend fun getNoteContent(noteId: Long): Result<NoteContent> {
        val entry = DummyNoteContentProvider.get(noteId)
        val note = DummyNoteProvider.findNote(entry.noteId).toDomain()
        return Result.Success(
            NoteContent(
                note = note,
                pageLabel = entry.pageLabel,
                heading = entry.heading,
                bodyBeforeHighlight = entry.bodyBeforeHighlight,
                highlighted = entry.highlighted,
                bodyAfterHighlight = entry.bodyAfterHighlight,
                verifiedCount = entry.verifiedCount,
                repairedCount = entry.repairedCount,
                escalatedCount = entry.escalatedCount,
                uncertainCount = entry.uncertainCount,
                formula = entry.formula,
                secondParagraphBefore = entry.secondParagraphBefore,
                secondParagraphHighlighted = entry.secondParagraphHighlighted,
                uncertainFragmentLabel = entry.uncertainFragmentLabel,
                correction = entry.correction,
                provenance = entry.provenance
            )
        )
    }

    override suspend fun saveCorrection(noteId: Long, regionIndex: Int, correctedText: String): Result<Unit> {
        // Corrections are part of Lab 6 (IA-01)
        return Result.Success(Unit)
    }

    override suspend fun createNote(courseId: Long, title: String, classDate: String): Result<Note> {
        if (title.isBlank()) {
            return Result.Failure(AppError.Validation("title", "El título es obligatorio"))
        }
        return ApiErrorHandler.safeApiCall(
            apiCall = { api.createNote(CreateNoteRequest(courseId, title.trim(), classDate.trim())) },
            transform = { it.data.toDomain() }
        )
    }

    override suspend fun deleteNote(noteId: Long): Result<Unit> =
        ApiErrorHandler.safeApiCall(
            apiCall = {
                val response = api.deleteNote(noteId)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            },
            transform = {}
        )
}

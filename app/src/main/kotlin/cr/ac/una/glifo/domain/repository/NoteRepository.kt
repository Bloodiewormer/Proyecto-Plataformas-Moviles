package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.NoteMeta

interface NoteRepository {
    suspend fun getNotesForCourse(courseId: Long): Result<List<Note>>
    suspend fun getNoteMetaList(): Result<List<NoteMeta>>
    suspend fun getNote(noteId: Long): Result<Note>
    suspend fun getNoteMeta(noteId: Long): Result<NoteMeta>
    suspend fun getChips(noteId: Long): Result<List<ConfidenceState>>
    suspend fun getNoteContent(noteId: Long): Result<NoteContent>
    suspend fun saveCorrection(noteId: Long, regionIndex: Int, correctedText: String): Result<Unit>
}

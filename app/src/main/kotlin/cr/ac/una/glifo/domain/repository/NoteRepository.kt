package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.model.PageAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Los ids de apunte que circulan por la app son los locales (Room). Un apunte del servidor
 * recibe id local la primera vez que se lista; uno capturado offline lo tiene desde el inicio.
 */
interface NoteRepository {
    /** Refresca desde el servidor si hay red y devuelve lo que quedó en Room. */
    suspend fun getNotesForCourse(courseId: Long): Result<List<Note>>

    /** Con [courseId] refresca ese curso; sin él devuelve todo lo guardado, sin red. */
    suspend fun getNoteMetaList(courseId: Long? = null): Result<List<NoteMeta>>
    suspend fun getNote(noteId: Long): Result<Note>
    suspend fun getNoteMeta(noteId: Long): Result<NoteMeta>
    suspend fun getChips(noteId: Long): Result<List<ConfidenceState>>
    suspend fun getNoteContent(noteId: Long): Result<NoteContent>

    /**
     * Observa cambios en Room sobre el apunte y sus operaciones de sincronización para que
     * el detalle se actualice apenas termine el `SyncWorker`.
     */
    fun observeNoteContent(noteId: Long): Flow<Result<NoteContent>> = emptyFlow()

    /**
     * Solicita la reconstrucción IA-01 del apunte en el servidor (`POST notes/{id}/reconstruct`)
     * y persiste `content` y `status = READY` en Room (#42). Si el apunte todavía no tiene
     * `serverId`, falla con un error claro sin llamar a la red.
     */
    suspend fun reconstructNote(localNoteId: Long): Result<NoteContent>

    suspend fun saveCorrection(noteId: Long, fragmentId: String, correctedText: String): Result<Unit>
    suspend fun createNote(courseId: Long, title: String, classDate: String): Result<Note>
    suspend fun deleteNote(noteId: Long): Result<Unit>

    /**
     * Guarda una página recién procesada (#25) y encola su subida (#26). Si la misma hoja ya
     * pertenece a un apunte del curso (hash perceptual casi igual), devuelve ese apunte en
     * vez de duplicarlo. Devuelve el id local del apunte.
     */
    suspend fun saveCapture(courseId: Long, photoPath: String, perceptualHash: String, analysis: PageAnalysis): Result<Long>
}

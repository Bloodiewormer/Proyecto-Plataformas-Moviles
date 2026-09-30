package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.model.PageAnalysis

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

package cr.ac.una.glifo.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cr.ac.una.glifo.data.datasource.local.entity.FragmentCorrectionEntity
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.PageHashRow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE course_id = :courseId ORDER BY class_date DESC, created_at DESC")
    suspend fun notesForCourse(courseId: Long): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY class_date DESC, created_at DESC")
    suspend fun allNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun noteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE server_id = :serverId")
    suspend fun noteByServerId(serverId: Long): NoteEntity?

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Query("UPDATE notes SET title = :title, class_date = :classDate, course_id = :courseId WHERE id = :id")
    suspend fun updateNoteHeader(id: Long, title: String, classDate: String, courseId: Long)

    @Query("UPDATE notes SET server_id = :serverId WHERE id = :id")
    suspend fun setServerId(id: Long, serverId: Long)

    @Query("UPDATE notes SET content_json = :contentJson, status = :status WHERE id = :id")
    suspend fun setServerContent(id: Long, contentJson: String?, status: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    /** Apuntes del servidor que ya no existen allá y que no tienen nada pendiente de subir. */
    @Query(
        """
        DELETE FROM notes
        WHERE course_id = :courseId AND server_id IS NOT NULL AND server_id NOT IN (:keepServerIds)
          AND id NOT IN (SELECT note_id FROM note_pages)
        """
    )
    suspend fun deleteStaleRemoteNotes(courseId: Long, keepServerIds: List<Long>)

    @Insert
    suspend fun insertPage(page: NotePageEntity): Long

    @Query("SELECT * FROM note_pages WHERE note_id = :noteId ORDER BY page_index")
    suspend fun pagesForNote(noteId: Long): List<NotePageEntity>

    @Query("SELECT * FROM note_pages WHERE id = :id")
    suspend fun pageById(id: Long): NotePageEntity?

    @Query("SELECT COUNT(*) FROM note_pages WHERE note_id = :noteId")
    suspend fun pageCount(noteId: Long): Int

    /** Hashes de las páginas ya guardadas del curso, para no crear dos apuntes de la misma hoja. */
    @Query(
        """
        SELECT p.note_id AS id, p.perceptual_hash FROM note_pages p
        JOIN notes n ON n.id = p.note_id WHERE n.course_id = :courseId
        """
    )
    suspend fun pageHashesForCourse(courseId: Long): List<PageHashRow>

    @Upsert
    suspend fun upsertCorrection(correction: FragmentCorrectionEntity)

    @Query("SELECT * FROM fragment_corrections WHERE note_id = :noteId")
    suspend fun correctionsForNote(noteId: Long): List<FragmentCorrectionEntity>

    /**
     * Guarda en Room lo que devolvió el servidor sin pisar lo local: un apunte que ya existe
     * (mismo server_id) solo actualiza su encabezado.
     */
    @Transaction
    suspend fun cacheRemoteNotes(courseId: Long, remote: List<NoteEntity>) {
        remote.forEach { note ->
            val existing = note.serverId?.let { noteByServerId(it) }
            if (existing == null) {
                insertNote(note)
            } else {
                updateNoteHeader(existing.id, note.title, note.classDate, note.courseId)
            }
        }
        deleteStaleRemoteNotes(courseId, remote.mapNotNull { it.serverId })
    }

}

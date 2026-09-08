package cr.ac.una.glifo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cr.ac.una.glifo.core.database.entity.NoteEntity
import cr.ac.una.glifo.core.database.entity.NotePageEntity

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE user_id = :userId AND course_id = :courseId")
    suspend fun getNotesByCourse(userId: Long, courseId: Long): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotePage(page: NotePageEntity)

    @Query("SELECT * FROM note_pages WHERE note_id = :noteId ORDER BY page_index ASC")
    suspend fun getPagesForNote(noteId: Long): List<NotePageEntity>
}

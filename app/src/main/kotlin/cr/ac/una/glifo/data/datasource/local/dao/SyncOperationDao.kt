package cr.ac.una.glifo.data.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationRow

@Dao
interface SyncOperationDao {

    @Insert
    suspend fun insert(operation: SyncOperationEntity): Long

    /** Lo que el worker tiene que procesar, en el orden en que se encoló (D-23). */
    @Query("SELECT * FROM sync_operations WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY id")
    suspend fun runnable(): List<SyncOperationEntity>

    @Query("UPDATE sync_operations SET status = :status, attempts = :attempts, last_error = :lastError, updated_at = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, status: String, attempts: Int, lastError: String?, updatedAt: Long)

    /** Un FAILED vuelve a la cola cuando el estudiante pide reintentar. */
    @Query("UPDATE sync_operations SET status = 'PENDING', last_error = NULL, updated_at = :updatedAt WHERE status = 'FAILED'")
    suspend fun requeueFailed(updatedAt: Long): Int

    @Query("SELECT COUNT(*) FROM sync_operations WHERE status != 'DONE'")
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_operations WHERE note_id = :noteId AND status != 'DONE'")
    suspend fun pendingForNote(noteId: Long): Int

    @Query(
        """
        SELECT o.id, o.entity_type, o.note_id, n.title AS note_title, p.page_index,
               o.status, o.attempts, o.last_error, o.updated_at
        FROM sync_operations o
        JOIN notes n ON n.id = o.note_id
        LEFT JOIN note_pages p ON p.id = o.page_id
        ORDER BY CASE o.status WHEN 'IN_PROGRESS' THEN 0 WHEN 'PENDING' THEN 1 WHEN 'FAILED' THEN 2 ELSE 3 END,
                 o.id DESC
        LIMIT 50
        """
    )
    suspend fun queueRows(): List<SyncOperationRow>
}

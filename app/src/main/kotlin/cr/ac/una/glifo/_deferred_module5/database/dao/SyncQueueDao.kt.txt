package cr.ac.una.glifo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cr.ac.una.glifo.core.database.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingOperations(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncQueueEntity)

    @Update
    suspend fun update(operation: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE status = 'DONE'")
    suspend fun clearDoneOperations()
}

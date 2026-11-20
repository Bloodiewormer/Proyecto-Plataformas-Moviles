package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.model.SyncRunSummary
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    suspend fun getQueueItems(): Result<List<SyncQueueItem>>
    suspend fun getPendingCount(): Result<Int>
    fun observePendingCount(): Flow<Int>

    /** Procesa la cola en orden (CREATE_NOTE antes que sus UPLOAD_PAGE, D-23). */
    suspend fun syncNow(): Result<SyncRunSummary>

    /** Devuelve a la cola lo que quedó FAILED y programa otra pasada. */
    suspend fun retryFailed(): Result<Int>
}

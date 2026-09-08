package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.SyncQueueItem

interface SyncRepository {
    suspend fun getQueueItems(): Result<List<SyncQueueItem>>
    suspend fun getPendingCount(): Result<Int>
}

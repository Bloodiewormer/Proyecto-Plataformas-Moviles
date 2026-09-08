package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.dummy.DummySyncProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.repository.SyncRepository
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor() : SyncRepository {
    override suspend fun getQueueItems(): Result<List<SyncQueueItem>> =
        Result.Success(DummySyncProvider.getQueueItems())

    override suspend fun getPendingCount(): Result<Int> =
        Result.Success(DummySyncProvider.getPendingCount())
}

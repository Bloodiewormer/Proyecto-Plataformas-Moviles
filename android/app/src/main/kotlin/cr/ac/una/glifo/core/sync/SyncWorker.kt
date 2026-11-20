package cr.ac.una.glifo.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cr.ac.una.glifo.domain.error.Result as DomainResult
import cr.ac.una.glifo.domain.model.SyncRunOutcome
import cr.ac.una.glifo.domain.repository.SyncRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.work.ListenableWorker.Result as WorkResult

/**
 * Vacía la cola offline (issue #26). La lógica vive en [SyncRepository.syncNow]; el worker
 * solo traduce el resultado a lo que entiende WorkManager: `retry()` activa el backoff
 * exponencial de [SyncScheduler].
 *
 * Hilt no inyecta Workers sin la librería hilt-work, así que el repositorio se obtiene con
 * un EntryPoint del grafo de la aplicación.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun syncRepository(): SyncRepository
    }

    override suspend fun doWork(): WorkResult {
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, SyncWorkerEntryPoint::class.java)
            .syncRepository()

        return when (val result = repository.syncNow()) {
            is DomainResult.Success -> when (result.value.outcome) {
                SyncRunOutcome.Completed -> WorkResult.success()
                SyncRunOutcome.RetryLater -> WorkResult.retry()
            }
            is DomainResult.Failure -> WorkResult.retry()
        }
    }
}

package cr.ac.una.glifo.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background sync worker (WorkManager). Per D-24 / Glifo_Bitacora_Decisiones.md, Room and
 * any local persistence (`data/local/`, DAOs) are out of scope until Module 5 / Applied
 * Research — this worker previously read a `SyncQueueDao` that lived in `core/database/`,
 * which has been removed from the active source tree for that reason (see
 * `_deferred_module5/` at the repo root for the preserved Room modeling work).
 *
 * Left as a scaffold, not wired into any DI graph or WorkManager schedule yet: the real
 * queue this will drain is `domain.repository.SyncRepository` / `SyncOperation`, once
 * there's a persistence layer to back it. Do not reintroduce a Room dependency here before
 * Module 5.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        // TODO(Module 5): drain the persisted sync queue once data/local/ exists.
        // Until then there is nothing to process — dummy data lives entirely in memory.
        ListenableWorker.Result.success()
    }
}

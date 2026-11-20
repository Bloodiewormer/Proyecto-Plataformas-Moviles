package cr.ac.una.glifo.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cr.ac.una.glifo.core.datastore.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa el vaciado de la cola offline (issue #26). WorkManager espera a que haya red,
 * sobrevive a que se cierre la app y reintenta con backoff exponencial cuando el worker
 * pide `retry()` (Glifo_Alcance.md §6.5: "Retries with backoff and idempotency").
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {

    // Leer la preferencia de Ajustes es suspend: se hace fuera del hilo principal.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun schedule() {
        scope.launch { enqueue(wifiOnly = appPreferences.isSyncWifiOnly()) }
    }

    private fun enqueue(wifiOnly: Boolean) {
        // "Solo con Wi-Fi" en Ajustes: UNMETERED espera una red sin datos móviles.
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

        // APPEND_OR_REPLACE: si llega una captura mientras el worker corre, se encadena otra
        // pasada en vez de perderla; si la anterior falló, se reemplaza.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "glifo-sync-queue"
        private const val BACKOFF_SECONDS = 15L
    }
}

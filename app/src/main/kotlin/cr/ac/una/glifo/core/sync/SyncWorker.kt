package cr.ac.una.glifo.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import cr.ac.una.glifo.core.database.dao.SyncQueueDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncQueueDao: SyncQueueDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        try {
            val pendingOperations = syncQueueDao.getPendingOperations()
            
            if (pendingOperations.isEmpty()) {
                return@withContext ListenableWorker.Result.success()
            }

            // Group or process sequentially ensuring CREATE_NOTE happens before UPLOAD_PAGE
            // For now, process sequentially as the query returns them ordered by created_at ascending
            
            for (operation in pendingOperations) {
                try {
                    // Update to processing
                    syncQueueDao.update(operation.copy(status = "PROCESSING", attempts = operation.attempts + 1))
                    
                    // Simulate processing for now. Real implementation will call ApiService
                    // ApiService.executeSyncOperation(operation)
                    
                    // Mark as done
                    syncQueueDao.update(operation.copy(status = "DONE"))
                } catch (e: Exception) {
                    // Mark as pending or failed if too many attempts
                    val newStatus = if (operation.attempts >= 3) "FAILED" else "PENDING"
                    syncQueueDao.update(operation.copy(
                        status = newStatus, 
                        lastError = e.message?.take(500)
                    ))
                }
            }
            
            // Clean up done operations
            syncQueueDao.clearDoneOperations()
            
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.retry()
        }
    }
}

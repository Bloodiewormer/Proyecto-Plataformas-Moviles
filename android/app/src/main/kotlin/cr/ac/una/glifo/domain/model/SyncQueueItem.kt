package cr.ac.una.glifo.domain.model

enum class SyncStatus {
    Syncing,
    Queued,
    /** El servidor la rechazó por algo que reintentar no arregla; el estudiante puede reencolarla. */
    Failed,
    Done
}

data class SyncQueueItem(
    val noteId: Long,
    val title: String,
    val timestamp: String,
    val status: SyncStatus,
    val progress: Float = 0f,
    val attempts: Int = 0,
    val lastError: String? = null,
    val pageCount: Int? = null
)

enum class SyncRunOutcome {
    /** Se vació lo que se podía; lo que quedó FAILED necesita intervención. */
    Completed,
    /** Sin red, servidor caído o sesión vencida: WorkManager reintenta con backoff. */
    RetryLater
}

data class SyncRunSummary(
    val uploaded: Int,
    val failed: Int,
    val remaining: Int,
    val outcome: SyncRunOutcome
)

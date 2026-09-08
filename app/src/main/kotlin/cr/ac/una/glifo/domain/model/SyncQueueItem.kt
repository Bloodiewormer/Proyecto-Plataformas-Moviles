package cr.ac.una.glifo.domain.model

enum class SyncStatus { Syncing, Queued, Done }

data class SyncQueueItem(
    val noteId: Long,
    val title: String,
    val timestamp: String,
    val status: SyncStatus,
    val progress: Float = 0f
)

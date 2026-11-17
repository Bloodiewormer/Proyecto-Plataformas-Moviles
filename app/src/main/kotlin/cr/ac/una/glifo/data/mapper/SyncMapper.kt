package cr.ac.una.glifo.data.mapper

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.data.dto.SyncQueueDto
import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.model.SyncStatus

// DTO -> Domain
fun SyncQueueDto.toDomain(context: Context? = null): SyncQueueItem {
    val syncStatus = when (status.uppercase()) {
        "SYNCING", "IN_PROGRESS" -> SyncStatus.Syncing
        "DONE", "COMPLETED", "SUCCESS" -> SyncStatus.Done
        else -> SyncStatus.Queued
    }
    return SyncQueueItem(
        noteId = id,
        title = context?.getString(R.string.sync_op_title, entityType, id) ?: "Operación $entityType #$id",
        timestamp = context?.getString(R.string.sync_timestamp_recent) ?: "Reciente",
        status = syncStatus,
        progress = if (syncStatus == SyncStatus.Done) 1f else if (syncStatus == SyncStatus.Syncing) 0.5f else 0f
    )
}

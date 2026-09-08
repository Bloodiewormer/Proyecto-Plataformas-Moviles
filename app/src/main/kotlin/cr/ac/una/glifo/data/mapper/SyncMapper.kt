package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.core.database.entity.SyncQueueEntity
import cr.ac.una.glifo.data.dto.SyncQueueDto
import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.model.SyncStatus

// DTO -> Domain
fun SyncQueueDto.toDomain(): SyncQueueItem {
    val syncStatus = when (status.uppercase()) {
        "SYNCING", "IN_PROGRESS" -> SyncStatus.Syncing
        "DONE", "COMPLETED", "SUCCESS" -> SyncStatus.Done
        else -> SyncStatus.Queued
    }
    return SyncQueueItem(
        noteId = id,
        title = "Operación $entityType #$id",
        timestamp = "Reciente",
        status = syncStatus,
        progress = if (syncStatus == SyncStatus.Done) 1f else if (syncStatus == SyncStatus.Syncing) 0.5f else 0f
    )
}

// Entity (Room) -> Domain
fun SyncQueueEntity.toDomain(): SyncQueueItem {
    val syncStatus = when (status.uppercase()) {
        "SYNCING", "IN_PROGRESS" -> SyncStatus.Syncing
        "DONE", "COMPLETED", "SUCCESS" -> SyncStatus.Done
        else -> SyncStatus.Queued
    }
    return SyncQueueItem(
        noteId = id,
        title = "Operación $entityType #$id",
        timestamp = "En cola",
        status = syncStatus,
        progress = if (syncStatus == SyncStatus.Done) 1f else if (syncStatus == SyncStatus.Syncing) 0.5f else 0f
    )
}

// Domain -> Entity (Room)
fun SyncQueueItem.toEntity(entityType: String = "NOTE", payload: String = "{}"): SyncQueueEntity = SyncQueueEntity(
    id = noteId,
    entityType = entityType,
    payload = payload,
    status = when (status) {
        SyncStatus.Syncing -> "SYNCING"
        SyncStatus.Done -> "DONE"
        SyncStatus.Queued -> "PENDING"
    }
)

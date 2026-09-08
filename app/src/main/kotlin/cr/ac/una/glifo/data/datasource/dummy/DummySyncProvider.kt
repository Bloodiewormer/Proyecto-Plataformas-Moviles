package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.model.SyncStatus

object DummySyncProvider {
    fun getQueueItems(): List<SyncQueueItem> = listOf(
        SyncQueueItem(1L, "Integrales impropias · p.1", "12 ago · 9:41 AM", SyncStatus.Syncing, progress = 0.72f),
        SyncQueueItem(2L, "Criterio de comparación · p.1", "7 ago · 8:20 AM", SyncStatus.Queued),
        SyncQueueItem(3L, "Series de potencias · p.2", "2 ago · 7:55 AM", SyncStatus.Done),
        SyncQueueItem(4L, "Prueba de la integral · p.1", "28 jul · 10:12 AM", SyncStatus.Done)
    )

    fun getPendingCount(): Int = getQueueItems().count { it.status != SyncStatus.Done }
}

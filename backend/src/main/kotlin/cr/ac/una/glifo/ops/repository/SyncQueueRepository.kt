package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.SyncQueueEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SyncQueueRepository : JpaRepository<SyncQueueEntry, Long> {
    fun findByStatusOrderByCreatedAtAsc(status: String): List<SyncQueueEntry>
    fun findByIdempotencyKey(key: UUID): SyncQueueEntry?
}

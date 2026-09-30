package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.SyncQueueEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SyncQueueRepository : JpaRepository<SyncQueueEntry, Long> {
    fun findByStatusOrderByCreatedAtAsc(status: String): List<SyncQueueEntry>
    fun findByIdempotencyKey(key: UUID): SyncQueueEntry?
    fun findByUserIdAndStatus(userId: Long, status: String): List<SyncQueueEntry>
    fun existsByIdempotencyKey(key: UUID): Boolean
}

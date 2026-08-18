package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity(name = "sync_queue")
class SyncQueueEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    var device: Device? = null

    @Column(name = "entity_type", nullable = false)
    var entityType: String = ""

    @Column(name = "idempotency_key", nullable = false, unique = true)
    var idempotencyKey: UUID = UUID.randomUUID()

    @Column(columnDefinition = "jsonb")
    var payload: String = ""

    var attempts: Int = 0

    @Column(name = "last_error")
    var lastError: String? = null

    var status: String = "PENDING"

    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is SyncQueueEntry && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

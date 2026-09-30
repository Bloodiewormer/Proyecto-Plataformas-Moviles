package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sync_queue")
class SyncQueueEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    var device: Device? = null

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = ""

    @Column(name = "idempotency_key", nullable = false, unique = true)
    var idempotencyKey: UUID = UUID.randomUUID()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var payload: String = ""

    @Column(nullable = false)
    var attempts: Int = 0

    @Column(name = "last_error")
    var lastError: String? = null

    @Column(nullable = false, length = 30)
    var status: String = "PENDING"

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is SyncQueueEntry && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "SyncQueueEntry(id=$id, entityType='$entityType', status='$status', attempts=$attempts)"
}

package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity(name = "sync_queue")
@Table(
    name = "sync_queue",
    indexes = [
        Index(name = "idx_sync_queue_status", columnList = "status, created_at"),
    ],
    check = [
        CheckConstraint(
            name = "ck_sync_queue_status",
            constraint = "status IN ('PENDING', 'IN_PROGRESS', 'FAILED', 'DONE')",
        ),
    ],
)
class SyncQueueEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    // Dispositivo que subió el lote: el push de "sync completo" va solo a ese (D-18).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    var device: Device? = null

    @Column(name = "entity_type", nullable = false, length = 40)
    var entityType: String = ""

    // Lo genera el cliente antes del primer intento; un reintento duplicado choca con el
    // UNIQUE en vez de procesarse dos veces.
    @Column(name = "idempotency_key", nullable = false, unique = true)
    var idempotencyKey: UUID = UUID.randomUUID()

    // Operación serializada de cualquier tipo de entidad.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var payload: String = ""

    @Column(nullable = false)
    var attempts: Int = 0

    @Column(name = "last_error", length = 500)
    var lastError: String? = null

    @Column(nullable = false, length = 12)
    var status: String = "PENDING"

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is SyncQueueEntry && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

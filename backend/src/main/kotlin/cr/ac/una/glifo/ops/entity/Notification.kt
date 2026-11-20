package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_user_read", columnList = "user_id, read_at"),
        Index(name = "idx_notifications_device_status", columnList = "device_id, status"),
    ],
    check = [
        CheckConstraint(
            name = "ck_notifications_kind",
            constraint = "kind IN ('SYNC_COMPLETE', 'REVIEW_DUE', 'COVERAGE_DROP')",
        ),
        CheckConstraint(
            name = "ck_notifications_status",
            constraint = "status IN ('PENDING', 'ACCEPTED', 'FAILED_RETRYABLE', 'FAILED_PERMANENT')",
        ),
    ],
)
class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    /** Dispositivo objetivo cuando la notificación es dirigida (por ejemplo, `SYNC_COMPLETE`). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    var device: Device? = null

    /** Clave de idempotencia del evento lógico para evitar duplicar envíos en reintentos. */
    @Column(name = "idempotency_key", unique = true)
    var idempotencyKey: UUID? = null

    @Column(nullable = false, length = 30)
    var kind: String = ""

    /**
     * Estado del despacho hacia FCM:
     * - `PENDING`: evento registrado en BD, envío a FCM pendiente o en curso.
     * - `ACCEPTED`: FCM aceptó el mensaje (`fcmMessageId` y `sentAt` presentes; no implica entrega al SO).
     * - `FAILED_RETRYABLE`: falló por causa transitoria tras agotar los intentos acotados.
     * - `FAILED_PERMANENT`: falló por causa permanente (`UNREGISTERED`, `INVALID_ARGUMENT`, `NOT_CONFIGURED`, etc.).
     */
    @Column(nullable = false, length = 24)
    var status: String = "PENDING"

    @Column(nullable = false)
    var attempts: Int = 0

    @Column(name = "fcm_message_id", length = 255)
    var fcmMessageId: String? = null

    @Column(name = "last_error", length = 500)
    var lastError: String? = null

    // El contenido cambia según el tipo de notificación.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var payload: String = ""

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "sent_at")
    var sentAt: Instant? = null

    @Column(name = "read_at")
    var readAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Notification
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "Notification(id=$id, kind='$kind', status='$status', attempts=$attempts, sentAt=$sentAt, readAt=$readAt)"
}

package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_user_read", columnList = "user_id, read_at"),
    ],
    check = [
        CheckConstraint(
            name = "ck_notifications_kind",
            constraint = "kind IN ('SYNC_COMPLETE', 'REVIEW_DUE', 'COVERAGE_DROP')",
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

    @Column(nullable = false, length = 30)
    var kind: String = ""

    // El contenido cambia según el tipo de notificación.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var payload: String = ""

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
    override fun toString(): String = "Notification(id=$id, kind='$kind', sentAt=$sentAt, readAt=$readAt)"
}

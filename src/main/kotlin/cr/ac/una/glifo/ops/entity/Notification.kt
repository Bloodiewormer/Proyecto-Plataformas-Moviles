package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "notifications")
class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @Column(nullable = false, length = 50)
    var kind: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var payload: String = ""

    @Column(name = "sent_at")
    var sentAt: Instant? = null

    @Column(name = "read_at")
    var readAt: Instant? = null

    override fun equals(other: Any?): Boolean = other is Notification && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Notification(id=$id, kind='$kind', sentAt=$sentAt, readAt=$readAt)"
}

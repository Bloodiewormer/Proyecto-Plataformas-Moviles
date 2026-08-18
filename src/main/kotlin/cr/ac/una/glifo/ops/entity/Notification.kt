package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "notifications")
class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @Column(nullable = false)
    var kind: String = ""

    @Column(columnDefinition = "jsonb")
    var payload: String = ""

    @Column(name = "sent_at")
    var sentAt: Instant? = null

    @Column(name = "read_at")
    var readAt: Instant? = null

    override fun equals(other: Any?): Boolean = other is Notification && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "devices")
@Table(
    name = "devices",
    check = [
        CheckConstraint(name = "ck_devices_platform", constraint = "platform IN ('ANDROID', 'IOS', 'WEB')"),
    ],
)
class Device {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @Column(name = "fcm_token", nullable = false, unique = true)
    var fcmToken: String = ""

    @Column(nullable = false, length = 12)
    var platform: String = ""

    @Column(name = "registered_at", nullable = false)
    var registeredAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is Device && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

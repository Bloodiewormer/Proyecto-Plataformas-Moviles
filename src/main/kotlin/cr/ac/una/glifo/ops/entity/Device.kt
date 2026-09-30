package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "devices")
class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @Column(name = "fcm_token", nullable = false, unique = true, length = 255)
    var fcmToken: String = ""

    @Column(nullable = false, length = 50)
    var platform: String = ""

    @Column(name = "registered_at", nullable = false)
    var registeredAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is Device && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Device(id=$id, platform='$platform', registeredAt=$registeredAt)"
}

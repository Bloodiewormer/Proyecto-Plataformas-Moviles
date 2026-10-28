package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "devices",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_devices_user_installation",
            columnNames = ["user_id", "installation_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_devices_user_active", columnList = "user_id, is_active"),
    ],
    check = [
        CheckConstraint(name = "ck_devices_platform", constraint = "platform IN ('ANDROID', 'IOS', 'WEB')"),
    ],
)
class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    /**
     * Identificador estable de la instalación de la app en el teléfono (generado una vez
     * en DataStore, sin identificadores físicos). Permite que el mismo dispositivo conserve
     * su `device_id` (`id`) cuando Firebase rota el `fcm_token`.
     */
    @Column(name = "installation_id", nullable = false, length = 64)
    var installationId: String = UUID.randomUUID().toString()

    /**
     * Token actual de Firebase Cloud Messaging. Es anulable porque al cerrar sesión o
     * detectar `UNREGISTERED` se desvincula sin borrar la fila (referenciada por `sync_queue`
     * y `notifications`). En PostgreSQL `UNIQUE` permite múltiples `NULL` y garantiza que
     * un mismo token activo no quede asociado a más de un registro.
     */
    @Column(name = "fcm_token", unique = true, length = 512)
    var fcmToken: String? = null

    @Column(nullable = false, length = 12)
    var platform: String = "ANDROID"

    @Column(name = "is_active", nullable = false)
    var active: Boolean = true

    @Column(name = "registered_at", nullable = false)
    var registeredAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Device
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "Device(id=$id, installationId='$installationId', platform='$platform', active=$active, registeredAt=$registeredAt)"
}

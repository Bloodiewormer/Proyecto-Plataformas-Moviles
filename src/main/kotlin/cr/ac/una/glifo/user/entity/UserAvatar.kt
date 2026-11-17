package cr.ac.una.glifo.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Foto de perfil del usuario, ya reducida por la app (unos KB), una por usuario. */
@Entity
@Table(name = "user_avatars")
class UserAvatar(
    @Id
    @Column(name = "user_id")
    var userId: Long = 0,

    @Column(name = "content_type", nullable = false, length = 32)
    var contentType: String = "",

    @Column(name = "data", nullable = false)
    var data: ByteArray = ByteArray(0),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

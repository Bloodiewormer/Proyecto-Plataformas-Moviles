package cr.ac.una.glifo.user.entity

import jakarta.persistence.*
import java.time.Instant

@Entity(name = "users")
class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @Column(nullable = false, unique = true)
    var email: String = ""
    
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = ""
    
    @Column(name = "first_name")
    var firstName: String? = null
    
    @Column(name = "last_name")
    var lastName: String? = null
    
    @Column(name = "avatar_url")
    var avatarUrl: String? = null
    
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
    
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<Role> = mutableSetOf()
    
    override fun equals(other: Any?): Boolean = other is User && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

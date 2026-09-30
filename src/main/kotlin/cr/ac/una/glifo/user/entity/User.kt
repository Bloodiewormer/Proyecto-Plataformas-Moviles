package cr.ac.una.glifo.user.entity

import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @Column(nullable = false, unique = true)
    var email: String = ""
    
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = ""
    
    @Column(name = "first_name", length = 100)
    var firstName: String? = null

    @Column(name = "last_name", length = 100)
    var lastName: String? = null

    @Column(name = "avatar_url", length = 512)
    var avatarUrl: String? = null
    
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<Role> = mutableSetOf()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as User
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "User(id=$id, email='$email', firstName=$firstName, lastName=$lastName, isActive=$isActive)"
}

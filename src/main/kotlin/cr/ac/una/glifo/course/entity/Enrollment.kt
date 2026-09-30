package cr.ac.una.glifo.course.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.time.Instant

@Entity
@Table(name = "enrollments")
class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course? = null
    
    @Column(nullable = false, length = 30)
    var status: String = "ACTIVE"
    
    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Enrollment
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "Enrollment(id=$id, status='$status', joinedAt=$joinedAt)"
}

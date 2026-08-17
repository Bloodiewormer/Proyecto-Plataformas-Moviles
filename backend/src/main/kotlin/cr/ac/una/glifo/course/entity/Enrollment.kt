package cr.ac.una.glifo.course.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "enrollments")
class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course? = null
    
    @Column(nullable = false)
    var status: String = "ACTIVE"
    
    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now()
    
    override fun equals(other: Any?): Boolean = other is Enrollment && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

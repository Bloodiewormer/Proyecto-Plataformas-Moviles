package cr.ac.una.glifo.course.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "courses")
class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    var owner: User? = null
    
    @Column(nullable = false)
    var code: String = ""
    
    @Column(nullable = false)
    var name: String = ""
    
    @Column(nullable = false)
    var term: String = ""
    
    @Column(name = "join_code", nullable = false, unique = true)
    var joinCode: String = ""
    
    @Column(name = "syllabus_source_uri")
    var syllabusSourceUri: String? = null
    
    @Column(name = "syllabus_parsed_at")
    var syllabusParsedAt: Instant? = null
    
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
    
    override fun equals(other: Any?): Boolean = other is Course && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

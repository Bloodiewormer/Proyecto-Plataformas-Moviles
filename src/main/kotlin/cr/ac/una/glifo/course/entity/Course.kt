package cr.ac.una.glifo.course.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "courses")
@Table(
    name = "courses",
    indexes = [
        Index(name = "idx_courses_owner", columnList = "owner_user_id"),
    ],
)
class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    // El dueño del curso (docente) se modela aquí y no como una matrícula.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    var owner: User? = null

    @Column(nullable = false, length = 20)
    var code: String = ""

    @Column(nullable = false, length = 150)
    var name: String = ""

    @Column(nullable = false, length = 20)
    var term: String = ""

    @Column(name = "join_code", nullable = false, unique = true, length = 10)
    var joinCode: String = ""

    // El syllabus era una tabla 1:1 con courses; se absorbió aquí (§4.2).
    @Column(name = "syllabus_source_uri", length = 512)
    var syllabusSourceUri: String? = null

    @Column(name = "syllabus_parsed_at")
    var syllabusParsedAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is Course && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

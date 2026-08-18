package cr.ac.una.glifo.note.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity(name = "notes")
class Note {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    lateinit var course: Course

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_topic_id")
    var syllabusTopic: SyllabusTopic? = null

    @Column(nullable = false)
    var classDate: LocalDate = LocalDate.now()

    @Column(nullable = false, length = 200)
    var title: String = ""

    @Column(nullable = false)
    var status: String = NoteStatus.DRAFT.name

    @Column(columnDefinition = "jsonb")
    var content: String? = null

    var contentGeneratedAt: Instant? = null

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is Note && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

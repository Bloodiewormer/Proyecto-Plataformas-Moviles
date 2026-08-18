package cr.ac.una.glifo.glossary.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "glossary_suggestions")
class GlossarySuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    var course: Course? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    var note: Note? = null

    @Column(nullable = false, length = 255)
    var originalText: String = ""

    @Column(nullable = false, length = 255)
    var suggestedCorrection: String = ""

    var status: String = "PENDING"
    var createdAt: Instant = Instant.now()
    var resolvedAt: Instant? = null

    override fun equals(other: Any?): Boolean = other is GlossarySuggestion && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

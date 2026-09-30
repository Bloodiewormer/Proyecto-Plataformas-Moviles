package cr.ac.una.glifo.glossary.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

// El estudiante no escribe en course_glossary (GLOSSARY_WRITE es del docente): deja una
// sugerencia que el docente aprueba o rechaza (D-15).
@Entity(name = "glossary_suggestions")
@Table(
    name = "glossary_suggestions",
    indexes = [
        Index(name = "idx_glossary_suggestions_course", columnList = "course_id, status"),
    ],
    check = [
        CheckConstraint(
            name = "ck_glossary_suggestions_status",
            constraint = "status IN ('PENDING', 'APPROVED', 'REJECTED')",
        ),
    ],
)
class GlossarySuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var course: Course? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var note: Note? = null

    @Column(nullable = false, length = 255)
    var originalText: String = ""

    @Column(nullable = false, length = 255)
    var suggestedCorrection: String = ""

    @Column(nullable = false, length = 12)
    var status: String = "PENDING"

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    var resolvedAt: Instant? = null

    override fun equals(other: Any?): Boolean = other is GlossarySuggestion && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

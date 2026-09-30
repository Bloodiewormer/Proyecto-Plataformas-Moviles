package cr.ac.una.glifo.glossary.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.time.Instant

@Entity
@Table(name = "glossary_suggestions")
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

    @Column(name = "original_text", nullable = false, length = 255)
    var originalText: String = ""

    @Column(name = "suggested_correction", nullable = false, length = 255)
    var suggestedCorrection: String = ""

    @Column(nullable = false, length = 30)
    var status: String = "PENDING"

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as GlossarySuggestion
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "GlossarySuggestion(id=$id, originalText='$originalText', suggestedCorrection='$suggestedCorrection', status='$status')"
}

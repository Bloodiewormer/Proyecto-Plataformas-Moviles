package cr.ac.una.glifo.glossary.entity

import cr.ac.una.glifo.course.entity.Course
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

// Glosario canónico de notación que mantiene el docente. Alimenta el ConfidenceScorer.
// No lleva UNIQUE(course_id, term): el mismo término puede existir con distinto kind
// (σ como símbolo y como abreviatura). Queda abierto hasta congelar kind (§4.3).
@Entity(name = "course_glossary")
@Table(
    name = "course_glossary",
    indexes = [
        Index(name = "idx_course_glossary_course", columnList = "course_id"),
    ],
    check = [
        CheckConstraint(name = "ck_course_glossary_kind", constraint = "kind IN ('TERM', 'SYMBOL', 'NOTATION')"),
    ],
)
class CourseGlossary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var course: Course? = null

    @Column(nullable = false, length = 120)
    var term: String = ""

    @Column(name = "canonical_form", nullable = false)
    var canonicalForm: String = ""

    @Column(nullable = false, length = 20)
    var kind: String = "TERM"

    override fun equals(other: Any?): Boolean = other is CourseGlossary && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

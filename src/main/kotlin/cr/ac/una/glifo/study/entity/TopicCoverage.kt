package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

// Relacional a propósito, no JSONB: state y score se filtran y agregan en cada pantalla
// de cobertura (Glifo_UML_Modeling.md §5.1).
@Entity(name = "topic_coverage")
@Table(
    name = "topic_coverage",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_topic_coverage_user_topic", columnNames = ["user_id", "syllabus_topic_id"]),
    ],
    indexes = [
        Index(name = "idx_topic_coverage_user", columnList = "user_id"),
    ],
    check = [
        CheckConstraint(
            name = "ck_topic_coverage_state",
            constraint = "state IN ('SOLID', 'PARTIAL', 'ABSENT', 'UNCERTAIN')",
        ),
        CheckConstraint(name = "ck_topic_coverage_score", constraint = "score BETWEEN 0 AND 1"),
    ],
)
class TopicCoverage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_topic_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var syllabusTopic: SyllabusTopic? = null

    @Column(nullable = false, length = 12)
    var state: String = "ABSENT"

    @Column(nullable = false)
    var score: Float = 0.0f

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is TopicCoverage && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

// Relacional a propósito, no JSONB: state y score se filtran y agregan en cada pantalla
// de cobertura (Glifo_UML_Modeling.md §5.1).
@Entity
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

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as TopicCoverage
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "TopicCoverage(id=$id, state='$state', score=$score, updatedAt=$updatedAt)"
}

package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.math.BigDecimal
import java.time.Instant

// Relacional a propósito: se agrega por tipo, nivel y periodo, que es el argumento de
// ahorro del proyecto (Glifo_UML_Modeling.md §5.1).
@Entity(name = "ai_calls")
@Table(
    name = "ai_calls",
    indexes = [
        Index(name = "idx_ai_calls_user_created", columnList = "user_id, created_at"),
    ],
    check = [
        CheckConstraint(
            name = "ck_ai_calls_type",
            constraint = "call_type IN ('OCR_M', 'IA_00', 'IA_01', 'IA_02', 'IA_03', 'IA_04')",
        ),
        CheckConstraint(
            name = "ck_ai_calls_level",
            constraint = "level IN ('N0', 'N1', 'N1_5', 'N2', 'N3', 'UNRESOLVED')",
        ),
    ],
)
class AiCall {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var course: Course? = null

    @Column(name = "call_type", nullable = false, length = 16)
    var callType: String = ""

    @Column(nullable = false, length = 12)
    var level: String = ""

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Int = 0

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Int = 0

    @Column(name = "estimated_cost", nullable = false, precision = 10, scale = 6)
    var estimatedCost: BigDecimal = BigDecimal.ZERO

    @Column(name = "latency_ms", nullable = false)
    var latencyMs: Int = 0

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is AiCall && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

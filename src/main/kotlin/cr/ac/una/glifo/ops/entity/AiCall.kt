package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "ai_calls")
class AiCall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    var course: Course? = null

    @Column(name = "call_type", nullable = false, length = 50)
    var callType: String = ""

    @Column(nullable = false, length = 20)
    var level: String = ""

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Int = 0

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Int = 0

    @Column(name = "estimated_cost", nullable = false)
    var estimatedCost: BigDecimal = BigDecimal.ZERO

    @Column(name = "latency_ms", nullable = false)
    var latencyMs: Int = 0

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is AiCall && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "AiCall(id=$id, callType='$callType', level='$level', latencyMs=$latencyMs)"
}

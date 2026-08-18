package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "topic_coverage")
class TopicCoverage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_topic_id")
    var syllabusTopic: SyllabusTopic? = null

    var state: String = "UNSEEN"
    var score: Float = 0.0f
    var updatedAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is TopicCoverage && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

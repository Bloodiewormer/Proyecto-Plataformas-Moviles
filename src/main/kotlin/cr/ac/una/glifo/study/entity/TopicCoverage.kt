package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.time.Instant

@Entity
@Table(name = "topic_coverage")
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

    @Column(nullable = false, length = 30)
    var state: String = "UNSEEN"

    @Column(nullable = false)
    var score: Float = 0.0f

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

package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "review_schedule")
class ReviewSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_item_id")
    var studyItem: StudyItem? = null

    var dueAt: Instant = Instant.now()
    var intervalDays: Int = 1
    var ease: Float = 2.5f

    override fun equals(other: Any?): Boolean = other is ReviewSchedule && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

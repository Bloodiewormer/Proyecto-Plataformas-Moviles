package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

@Entity(name = "review_schedule")
@Table(
    name = "review_schedule",
    // El estado SM-2 es el calendario actual, no un historial: una fila por (usuario, ítem).
    uniqueConstraints = [
        UniqueConstraint(name = "uq_review_schedule_user_item", columnNames = ["user_id", "study_item_id"]),
    ],
    indexes = [
        Index(name = "idx_review_schedule_user_due", columnList = "user_id, due_at"),
    ],
)
class ReviewSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_item_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var studyItem: StudyItem? = null

    @Column(nullable = false)
    var dueAt: Instant = Instant.now()

    @Column(nullable = false)
    var intervalDays: Int = 1

    @Column(nullable = false)
    var ease: Float = 2.5f

    override fun equals(other: Any?): Boolean = other is ReviewSchedule && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

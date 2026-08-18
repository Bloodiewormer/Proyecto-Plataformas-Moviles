package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "attempts")
class Attempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_item_id")
    var studyItem: StudyItem? = null

    @Column(columnDefinition = "jsonb")
    var response: String = ""

    var isCorrect: Boolean = false
    var answeredAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is Attempt && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

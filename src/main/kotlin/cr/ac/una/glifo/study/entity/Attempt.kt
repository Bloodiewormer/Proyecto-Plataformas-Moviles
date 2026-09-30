package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "attempts",
    indexes = [
        Index(name = "idx_attempts_user_item", columnList = "user_id, study_item_id"),
    ],
)
class Attempt {
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

    // La forma de la respuesta sigue al tipo de ítem (§5.4).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var response: String = ""

    @Column(name = "is_correct", nullable = false)
    var isCorrect: Boolean = false

    @Column(name = "answered_at", nullable = false)
    var answeredAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Attempt
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "Attempt(id=$id, isCorrect=$isCorrect, answeredAt=$answeredAt)"
}

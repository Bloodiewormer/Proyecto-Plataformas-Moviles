package cr.ac.una.glifo.course.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

@Entity(name = "enrollments")
@Table(
    name = "enrollments",
    // Evita que el mismo estudiante quede matriculado dos veces por un doble toque.
    uniqueConstraints = [
        UniqueConstraint(name = "uq_enrollments_user_course", columnNames = ["user_id", "course_id"]),
    ],
    indexes = [
        Index(name = "idx_enrollments_user", columnList = "user_id"),
    ],
    check = [
        CheckConstraint(name = "ck_enrollments_status", constraint = "status IN ('ACTIVE', 'PENDING', 'REMOVED')"),
    ],
)
class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var course: Course? = null

    @Column(nullable = false, length = 20)
    var status: String = "ACTIVE"

    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is Enrollment && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

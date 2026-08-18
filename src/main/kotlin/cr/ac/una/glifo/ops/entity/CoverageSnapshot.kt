package cr.ac.una.glifo.ops.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "coverage_snapshots")
class CoverageSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    var course: Course? = null

    @Column(name = "coverage_pct", nullable = false)
    var coveragePct: Float = 0.0f

    @Column(name = "taken_at")
    var takenAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is CoverageSnapshot && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

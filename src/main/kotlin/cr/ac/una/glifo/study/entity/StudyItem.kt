package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "study_items")
class StudyItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    var course: Course? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_topic_id")
    var syllabusTopic: SyllabusTopic? = null

    @Column(nullable = false, length = 50)
    var kind: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var payload: String = ""

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is StudyItem && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "StudyItem(id=$id, kind='$kind', createdAt=$createdAt)"
}

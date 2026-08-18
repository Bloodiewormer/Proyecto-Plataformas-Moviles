package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import jakarta.persistence.*
import java.time.Instant

@Entity(name = "study_items")
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

    var kind: String = ""

    @Column(columnDefinition = "jsonb")
    var payload: String = ""

    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is StudyItem && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

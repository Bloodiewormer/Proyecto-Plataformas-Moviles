package cr.ac.una.glifo.course.entity

import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
    name = "syllabus_topics",
    indexes = [
        Index(name = "idx_syllabus_topics_course", columnList = "course_id, parent_id, order_index"),
    ],
)
class SyllabusTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var course: Course? = null

    // Jerarquía autorreferenciada (tema → subtema); por eso es relacional y no JSONB.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    var parent: SyllabusTopic? = null

    @Column(nullable = false, length = 20)
    var code: String = ""

    @Column(nullable = false)
    var title: String = ""

    @Column(name = "order_index", nullable = false)
    var orderIndex: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SyllabusTopic
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "SyllabusTopic(id=$id, code='$code', title='$title', orderIndex=$orderIndex)"
}

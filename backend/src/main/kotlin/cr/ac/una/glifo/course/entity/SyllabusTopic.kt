package cr.ac.una.glifo.course.entity

import jakarta.persistence.*

@Entity(name = "syllabus_topics")
class SyllabusTopic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course? = null
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: SyllabusTopic? = null
    
    @Column(nullable = false)
    var code: String = ""
    
    @Column(nullable = false)
    var title: String = ""
    
    @Column(name = "order_index", nullable = false)
    var orderIndex: Int = 0
    
    override fun equals(other: Any?): Boolean = other is SyllabusTopic && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

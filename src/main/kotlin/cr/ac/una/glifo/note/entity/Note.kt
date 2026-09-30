package cr.ac.una.glifo.note.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "notes")
class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    lateinit var course: Course

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_topic_id")
    var syllabusTopic: SyllabusTopic? = null

    @Column(name = "class_date", nullable = false)
    var classDate: LocalDate = LocalDate.now()

    @Column(nullable = false, length = 200)
    var title: String = ""

    @Column(nullable = false, length = 50)
    var status: String = NoteStatus.DRAFT.name

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var content: String? = null

    @Column(name = "content_generated_at")
    var contentGeneratedAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @OneToMany(mappedBy = "note", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var pages: MutableList<NotePage> = mutableListOf()

    fun addPage(page: NotePage) {
        pages.add(page)
        page.note = this
    }

    fun removePage(page: NotePage) {
        pages.remove(page)
    }

    override fun equals(other: Any?): Boolean = other is Note && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Note(id=$id, title='$title', status='$status', classDate=$classDate)"
}

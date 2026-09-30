package cr.ac.una.glifo.note.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate

@Entity(name = "notes")
@Table(
    name = "notes",
    indexes = [
        Index(name = "idx_notes_user_course", columnList = "user_id, course_id"),
        Index(name = "idx_notes_status", columnList = "status"),
    ],
    check = [
        CheckConstraint(name = "ck_notes_status", constraint = "status IN ('DRAFT', 'PROCESSING', 'READY', 'ARCHIVED')"),
    ],
)
class Note {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    lateinit var course: Course

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_topic_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var syllabusTopic: SyllabusTopic? = null

    @Column(nullable = false)
    var classDate: LocalDate = LocalDate.now()

    @Column(nullable = false, length = 200)
    var title: String = ""

    @Column(nullable = false, length = 12)
    var status: String = NoteStatus.DRAFT.name

    // Reconstrucción IA-01: árbol de bloques con confianza por fragmento
    // (Glifo_UML_Modeling.md §5.5). Sin @JdbcTypeCode, Postgres rechaza el INSERT
    // porque recibe varchar para una columna jsonb.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var content: String? = null

    var contentGeneratedAt: Instant? = null

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean = other is Note && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

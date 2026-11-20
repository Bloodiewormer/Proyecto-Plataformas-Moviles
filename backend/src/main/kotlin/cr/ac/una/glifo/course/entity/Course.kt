package cr.ac.una.glifo.course.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(
    name = "courses",
    indexes = [
        Index(name = "idx_courses_owner", columnList = "owner_user_id"),
    ],
)
class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    // El dueño del curso (docente) se modela aquí y no como una matrícula.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    var owner: User? = null

    @Column(nullable = false, length = 20)
    var code: String = ""

    @Column(nullable = false, length = 150)
    var name: String = ""

    @Column(nullable = false, length = 20)
    var term: String = ""

    @Column(name = "join_code", nullable = false, unique = true, length = 10)
    var joinCode: String = ""

    // El syllabus era una tabla 1:1 con courses; se absorbió aquí (§4.2).
    @Column(name = "syllabus_source_uri", length = 512)
    var syllabusSourceUri: String? = null

    @Column(name = "syllabus_parsed_at")
    var syllabusParsedAt: Instant? = null

    // Versiones del contexto offline (D-17): la app guarda el glosario en Room con estos
    // números y lo vuelve a bajar solo si alguno cambió. Cada cambio del glosario sube
    // glossary_version; publicar el temario debería subir syllabus_version.
    @ColumnDefault("1")
    @Column(name = "syllabus_version", nullable = false)
    var syllabusVersion: Int = 1

    @ColumnDefault("1")
    @Column(name = "glossary_version", nullable = false)
    var glossaryVersion: Int = 1

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var syllabusTopics: MutableList<SyllabusTopic> = mutableListOf()

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var enrollments: MutableList<Enrollment> = mutableListOf()

    fun addSyllabusTopic(topic: SyllabusTopic) {
        syllabusTopics.add(topic)
        topic.course = this
    }

    fun addEnrollment(enrollment: Enrollment) {
        enrollments.add(enrollment)
        enrollment.course = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Course
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "Course(id=$id, code='$code', name='$name', term='$term', joinCode='$joinCode')"
}

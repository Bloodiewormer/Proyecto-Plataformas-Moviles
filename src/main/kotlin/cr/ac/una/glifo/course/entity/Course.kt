package cr.ac.una.glifo.course.entity

import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.time.Instant

@Entity
@Table(name = "courses")
class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    var owner: User? = null
    
    @Column(nullable = false, length = 20)
    var code: String = ""
    
    @Column(nullable = false, length = 150)
    var name: String = ""
    
    @Column(nullable = false, length = 30)
    var term: String = ""
    
    @Column(name = "join_code", nullable = false, unique = true, length = 30)
    var joinCode: String = ""
    
    @Column(name = "syllabus_source_uri", length = 512)
    var syllabusSourceUri: String? = null
    
    @Column(name = "syllabus_parsed_at")
    var syllabusParsedAt: Instant? = null
    
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

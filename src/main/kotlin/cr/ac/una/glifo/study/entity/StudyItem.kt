package cr.ac.una.glifo.study.entity

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "study_items",
    indexes = [
        Index(name = "idx_study_items_course_topic", columnList = "course_id, syllabus_topic_id"),
    ],
    check = [
        CheckConstraint(
            name = "ck_study_items_kind",
            constraint = "kind IN ('FLASHCARD', 'MULTIPLE_CHOICE', 'TRUE_FALSE')",
        ),
        // kind vive dos veces: como columna (para filtrar sin abrir el documento) y dentro
        // del payload (para deserializar la subclase correcta). Este CHECK las mantiene
        // iguales (Glifo_UML_Modeling.md §5.2).
        CheckConstraint(
            name = "ck_study_items_kind_matches_payload",
            constraint = "payload ->> 'kind' = kind",
        ),
        CheckConstraint(
            name = "ck_study_items_payload_version",
            constraint = "payload -> 'schemaVersion' IS NOT NULL",
        ),
    ],
)
class StudyItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var course: Course? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_topic_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var syllabusTopic: SyllabusTopic? = null

    @Column(nullable = false, length = 20)
    var kind: String = ""

    // Flashcard, selección única o falso/verdadero: cada tipo tiene otra forma, por eso
    // JSONB en lugar de las tablas quizzes/questions/question_options/answer_keys (§5.3).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var payload: String = ""

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as StudyItem
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "StudyItem(id=$id, kind='$kind', createdAt=$createdAt)"
}

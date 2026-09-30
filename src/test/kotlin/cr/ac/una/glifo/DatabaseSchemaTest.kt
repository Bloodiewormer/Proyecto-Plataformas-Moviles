package cr.ac.una.glifo

import cr.ac.una.glifo.course.entity.Enrollment
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.study.entity.StudyItem
import cr.ac.una.glifo.study.repository.StudyItemRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Issue #23: verifica contra PostgreSQL real que el esquema que genera Hibernate es el del
 * modelo (21 tablas, 7 columnas JSONB, CHECK y UNIQUE) y que import.sql lo deja con datos.
 * Necesita la base "glifo" igual que GlifoApplicationTests. Cada test corre dentro de una
 * transacción que se revierte, así que no ensucia los datos de import.sql.
 */
@SpringBootTest
@Transactional
class DatabaseSchemaTest {

    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var studyItemRepository: StudyItemRepository
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var topicRepository: SyllabusTopicRepository
    @Autowired lateinit var enrollmentRepository: EnrollmentRepository
    @Autowired lateinit var userRepository: UserRepository

    private val coreTables = listOf(
        "users", "roles", "privileges", "user_roles", "role_privileges",
        "courses", "enrollments", "syllabus_topics", "course_glossary", "glossary_suggestions",
        "notes", "note_pages",
        "topic_coverage", "study_items", "attempts", "review_schedule",
    )
    private val operationsTables = listOf("ai_calls", "sync_queue", "devices", "notifications", "coverage_snapshots")

    @Test
    fun `el esquema tiene las 16 tablas core y las 5 de operaciones`() {
        val tables = jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String::class.java,
        ).toSet()
        assertEquals(16, coreTables.size)
        assertTrue(tables.containsAll(coreTables + operationsTables), "Faltan: ${(coreTables + operationsTables) - tables}")
    }

    @Test
    fun `las columnas documentales son jsonb y ninguna otra`() {
        val jsonbColumns = jdbc.queryForList(
            """
            SELECT table_name || '.' || column_name FROM information_schema.columns
            WHERE table_schema = 'public' AND data_type = 'jsonb'
            """.trimIndent(),
            String::class.java,
        ).toSet()
        assertEquals(
            setOf(
                "notes.content", "note_pages.regions", "note_pages.quality_metrics",
                "study_items.payload", "attempts.response", "sync_queue.payload", "notifications.payload",
            ),
            jsonbColumns,
        )
    }

    @Test
    fun `import sql carga el curso demo con documentos jsonb consultables`() {
        // Operadores JSONB de Postgres sobre los datos sembrados: tres formas distintas en
        // la misma columna, distinguidas por el discriminador.
        val kinds = jdbc.queryForList(
            "SELECT payload ->> 'kind' FROM study_items ORDER BY 1",
            String::class.java,
        )
        assertEquals(listOf("FLASHCARD", "MULTIPLE_CHOICE", "TRUE_FALSE"), kinds)

        val correctOption = jdbc.queryForObject(
            """
            SELECT opt ->> 'id' FROM study_items, jsonb_array_elements(payload -> 'options') opt
            WHERE kind = 'MULTIPLE_CHOICE' AND (opt ->> 'isCorrect')::boolean
            """.trimIndent(),
            String::class.java,
        )
        assertEquals("b", correctOption)

        val uncertainRegions = jdbc.queryForObject(
            "SELECT count(*) FROM note_pages, jsonb_array_elements(regions) r WHERE (r ->> 'isUncertain')::boolean",
            Int::class.java,
        )
        assertEquals(1, uncertainRegions)

        val summary = jdbc.queryForObject(
            "SELECT content -> 'summary' ->> 'verified' FROM notes WHERE status = 'READY'",
            String::class.java,
        )
        assertEquals("1", summary)
    }

    @Test
    fun `un payload guardado desde JPA se lee de vuelta como jsonb`() {
        val course = courseRepository.findByJoinCode("GLF-CALC1")!!
        val topic = topicRepository.findByCourseIdOrderByOrderIndex(course.id).first()
        val saved = studyItemRepository.saveAndFlush(StudyItem().apply {
            this.course = course
            this.syllabusTopic = topic
            this.kind = "TRUE_FALSE"
            this.payload = """{"schemaVersion": 1, "kind": "TRUE_FALSE", "statement": "0 es par", "answer": true}"""
        })

        val stored = jdbc.queryForObject(
            "SELECT payload ->> 'statement' FROM study_items WHERE id = ?",
            String::class.java,
            saved.id,
        )
        assertEquals("0 es par", stored)
    }

    @Test
    fun `el CHECK rechaza un payload cuyo kind no coincide con la columna`() {
        val course = courseRepository.findByJoinCode("GLF-CALC1")!!
        val topic = topicRepository.findByCourseIdOrderByOrderIndex(course.id).first()
        assertThrows<DataIntegrityViolationException> {
            studyItemRepository.saveAndFlush(StudyItem().apply {
                this.course = course
                this.syllabusTopic = topic
                this.kind = "FLASHCARD"
                this.payload = """{"schemaVersion": 1, "kind": "TRUE_FALSE", "statement": "x", "answer": false}"""
            })
        }
    }

    @Test
    fun `el CHECK rechaza un estado fuera del enum`() {
        assertThrows<DataIntegrityViolationException> {
            jdbc.update("UPDATE notes SET status = 'DELETED' WHERE status = 'DRAFT'")
        }
    }

    @Test
    fun `borrar un apunte borra sus paginas en cascada`() {
        val noteId = jdbc.queryForObject(
            "SELECT n.id FROM notes n JOIN note_pages p ON p.note_id = n.id LIMIT 1",
            Long::class.java,
        )
        jdbc.update("DELETE FROM notes WHERE id = ?", noteId)
        val pages = jdbc.queryForObject("SELECT count(*) FROM note_pages WHERE note_id = ?", Int::class.java, noteId)
        assertEquals(0, pages)
    }

    @Test
    fun `el UNIQUE impide matricular dos veces al mismo estudiante`() {
        val course = courseRepository.findByJoinCode("GLF-CALC1")!!
        val student = userRepository.findByEmail("estudiante@est.una.ac.cr")!!
        assertThrows<DataIntegrityViolationException> {
            enrollmentRepository.saveAndFlush(Enrollment().apply {
                this.user = student
                this.course = course
            })
        }
    }
}

package cr.ac.una.glifo.repository

import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Insert, find, update y delete de apuntes con NoteRepository contra PostgreSQL (glifo_test).
 * El apunte es del estudiante demo y del curso demo de import.sql (dos relaciones N a 1),
 * y su contenido va en una columna JSONB.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteRepositoryTest {

    @Autowired lateinit var noteRepository: NoteRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var entityManager: EntityManager
    @Autowired lateinit var jdbc: JdbcTemplate

    private fun newNote() = Note().apply {
        user = userRepository.findByEmail("estudiante@est.una.ac.cr")!!
        course = courseRepository.findByJoinCode("GLF-CALC1")!!
        title = "Derivadas parciales"
    }

    /** Manda los cambios a PostgreSQL y vacía la memoria de Hibernate: el siguiente find lee de la base. */
    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    fun `insert guarda el apunte en estado DRAFT`() {
        val saved = noteRepository.save(newNote())
        flushAndClear()

        assertTrue(saved.id > 0)
        assertEquals("DRAFT", noteRepository.findById(saved.id).orElseThrow().status)
    }

    @Test
    fun `find por curso incluye el apunte nuevo`() {
        val courseId = courseRepository.findByJoinCode("GLF-CALC1")!!.id
        noteRepository.save(newNote())
        flushAndClear()

        val notes = noteRepository.findByCourseId(courseId)

        // Los 2 apuntes demo de import.sql más el nuevo.
        assertEquals(3, notes.size)
        assertTrue(notes.any { it.title == "Derivadas parciales" })
    }

    @Test
    fun `update cambia el estado y guarda el contenido JSONB`() {
        val saved = noteRepository.save(newNote())
        flushAndClear()

        val note = noteRepository.findById(saved.id).orElseThrow()
        note.status = "READY"
        note.content = """{"summary": "Regla de la cadena"}"""
        noteRepository.save(note)
        flushAndClear()

        assertEquals("READY", noteRepository.findById(saved.id).orElseThrow().status)
        // PostgreSQL guardó el contenido como JSON: se puede consultar un campo con ->>
        val summary = jdbc.queryForObject(
            "SELECT content ->> 'summary' FROM notes WHERE id = ?", String::class.java, saved.id
        )
        assertEquals("Regla de la cadena", summary)
    }

    @Test
    fun `delete elimina el apunte`() {
        val saved = noteRepository.save(newNote())
        flushAndClear()

        noteRepository.deleteById(saved.id)
        flushAndClear()

        assertFalse(noteRepository.findById(saved.id).isPresent)
    }
}

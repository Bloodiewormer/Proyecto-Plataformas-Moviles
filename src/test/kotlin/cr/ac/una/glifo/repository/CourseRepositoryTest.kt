package cr.ac.una.glifo.repository

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Insert, find, update y delete de cursos con CourseRepository contra PostgreSQL (glifo_test).
 * El dueño del curso es el docente demo que carga import.sql (relación N a 1).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseRepositoryTest {

    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var entityManager: EntityManager

    private fun newCourse() = Course().apply {
        owner = userRepository.findByEmail("docente@una.ac.cr")
        code = "EIF411"
        name = "Plataformas Móviles"
        term = "2026-II"
        joinCode = "GLF-TEST1"
    }

    /** Manda los cambios a PostgreSQL y vacía la memoria de Hibernate: el siguiente find lee de la base. */
    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    fun `insert guarda el curso y le asigna id`() {
        val saved = courseRepository.save(newCourse())
        flushAndClear()

        assertTrue(saved.id > 0)
        assertTrue(courseRepository.existsById(saved.id))
    }

    @Test
    fun `find por codigo de union trae el curso guardado`() {
        courseRepository.save(newCourse())
        flushAndClear()

        val found = courseRepository.findByJoinCode("GLF-TEST1")

        assertNotNull(found)
        assertEquals("Plataformas Móviles", found.name)
    }

    @Test
    fun `find por docente trae sus cursos`() {
        val teacher = userRepository.findByEmail("docente@una.ac.cr")!!
        courseRepository.save(newCourse())
        flushAndClear()

        val courses = courseRepository.findByOwnerId(teacher.id)

        // El curso demo de import.sql más el que se acaba de guardar.
        assertEquals(2, courses.size)
    }

    @Test
    fun `update cambia el nombre del curso`() {
        val saved = courseRepository.save(newCourse())
        flushAndClear()

        val course = courseRepository.findById(saved.id).orElseThrow()
        course.name = "Diseño de Plataformas Móviles"
        courseRepository.save(course)
        flushAndClear()

        assertEquals("Diseño de Plataformas Móviles", courseRepository.findById(saved.id).orElseThrow().name)
    }

    @Test
    fun `delete elimina el curso`() {
        val saved = courseRepository.save(newCourse())
        flushAndClear()

        courseRepository.deleteById(saved.id)
        flushAndClear()

        assertFalse(courseRepository.findById(saved.id).isPresent)
    }
}

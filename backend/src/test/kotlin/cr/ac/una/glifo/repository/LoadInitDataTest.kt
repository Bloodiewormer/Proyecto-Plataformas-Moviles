package cr.ac.una.glifo.repository

import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Datos iniciales: al crear las tablas, Hibernate ejecuta src/main/resources/import.sql.
 * Esta prueba confirma, a través de los repositorios, que esos datos quedaron cargados.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoadInitDataTest {

    @Autowired lateinit var roleRepository: RoleRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var noteRepository: NoteRepository

    @Test
    fun `import sql carga los 3 roles`() {
        val roles = roleRepository.findAll()

        assertEquals(3, roles.size)
    }

    @Test
    fun `import sql carga las 3 cuentas demo`() {
        val users = userRepository.findAll()

        assertEquals(3, users.size)
        assertNotNull(userRepository.findByEmail("estudiante@est.una.ac.cr"))
    }

    @Test
    fun `import sql carga el curso demo con sus 2 apuntes`() {
        val course = courseRepository.findByJoinCode("GLF-CALC1")

        assertNotNull(course)
        assertEquals("MAT-101", course.code)
        assertEquals(2, noteRepository.findByCourseId(course.id).size)
    }
}

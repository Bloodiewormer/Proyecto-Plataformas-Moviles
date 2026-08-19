package cr.ac.una.glifo

import cr.ac.una.glifo.feature.home.domain.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class UserRoleTest {

    @Test
    fun `ROLE_STUDENT maps to UserRole STUDENT`() {
        val role = UserRole.fromRoles(listOf("ROLE_STUDENT"))
        assertEquals(UserRole.STUDENT, role)
    }

    @Test
    fun `ROLE_TEACHER maps to UserRole TEACHER`() {
        val role = UserRole.fromRoles(listOf("ROLE_TEACHER"))
        assertEquals(UserRole.TEACHER, role)
    }

    @Test
    fun `Teacher takes precedence over Student`() {
        val role = UserRole.fromRoles(listOf("ROLE_STUDENT", "ROLE_TEACHER"))
        assertEquals(UserRole.TEACHER, role)
    }

    @Test
    fun `Spanish synonyms map correctly`() {
        assertEquals(UserRole.TEACHER, UserRole.fromRoles(listOf("DOCENTE")))
        assertEquals(UserRole.TEACHER, UserRole.fromRoles(listOf("Profesor")))
        assertEquals(UserRole.STUDENT, UserRole.fromRoles(listOf("Estudiante")))
        assertEquals(UserRole.STUDENT, UserRole.fromRoles(listOf("Alumno")))
    }

    @Test
    fun `Empty roles list falls back safely to STUDENT without crash`() {
        val role = UserRole.fromRoles(emptyList())
        assertEquals(UserRole.STUDENT, role)
    }

    @Test
    fun `Null roles collection falls back safely to STUDENT without crash`() {
        val role = UserRole.fromRoles(null)
        assertEquals(UserRole.STUDENT, role)
    }

    @Test
    fun `Unknown role falls back safely to STUDENT without crash`() {
        val role = UserRole.fromRoles(listOf("ROLE_GUEST", "UNKNOWN_CUSTOM_ROLE"))
        assertEquals(UserRole.STUDENT, role)
    }
}

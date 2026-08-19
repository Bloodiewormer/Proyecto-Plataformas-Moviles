package cr.ac.una.glifo

import cr.ac.una.glifo.core.network.dto.UpdateRolesRequest
import cr.ac.una.glifo.core.network.dto.UpdateStatusRequest
import cr.ac.una.glifo.core.network.dto.UserAdminDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminModelsTest {

    @Test
    fun `test user admin dto holds correct role data`() {
        val user = UserAdminDto(
            id = 1L,
            email = "admin@glifo.ac.cr",
            firstName = "Admin",
            lastName = "Glifo",
            isActive = true,
            roles = listOf("ROLE_ADMIN")
        )

        assertEquals(1L, user.id)
        assertEquals("admin@glifo.ac.cr", user.email)
        assertTrue(user.isActive)
        assertTrue(user.roles.contains("ROLE_ADMIN"))
    }

    @Test
    fun `test update role request carries role names`() {
        val request = UpdateRolesRequest(roles = listOf("ROLE_ADMIN", "ROLE_TEACHER"))
        assertEquals(2, request.roles.size)
        assertTrue(request.roles.contains("ROLE_ADMIN"))
    }

    @Test
    fun `test update status request carries boolean flag`() {
        val request = UpdateStatusRequest(isActive = false)
        assertEquals(false, request.isActive)
    }
}

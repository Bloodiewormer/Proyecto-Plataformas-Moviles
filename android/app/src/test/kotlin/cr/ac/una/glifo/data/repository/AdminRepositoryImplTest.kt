package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.AdminChangeRoleRequest
import cr.ac.una.glifo.data.dto.AdminResetPasswordRequest
import cr.ac.una.glifo.data.dto.UserDto
import cr.ac.una.glifo.domain.model.AdminUser
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.data.dto.AdminResetPasswordResponse
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.HealthResponseDto
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ApiHealth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AdminRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val repository = AdminRepositoryImpl(api)

    @Test
    fun `checkHealth maps health response dto to domain model`() = runTest {
        coEvery { api.getHealth() } returns ApiResponse(
            HealthResponseDto(
                status = "ok",
                service = "glifo-api",
                timestamp = "2026-11-17T00:00:00Z"
            )
        )

        val result = repository.checkHealth()

        assertEquals(
            Result.Success(ApiHealth("ok", "glifo-api", "2026-11-17T00:00:00Z")),
            result
        )
    }

    @Test
    fun `checkHealth maps IOException to NoConnection`() = runTest {
        coEvery { api.getHealth() } throws IOException("timeout")

        val result = repository.checkHealth()

        assertEquals(Result.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `resetUserPassword trims email and returns temporary password`() = runTest {
        coEvery { api.adminResetPassword(any()) } returns ApiResponse(
            AdminResetPasswordResponse(email = "user@est.una.ac.cr", temporaryPassword = "tmp-9911")
        )

        val result = repository.resetUserPassword("  user@est.una.ac.cr  ")

        assertEquals(Result.Success("tmp-9911"), result)
        coVerify { api.adminResetPassword(AdminResetPasswordRequest("user@est.una.ac.cr")) }
    }

    @Test
    fun `resetUserPassword maps HTTP 404 to Server error`() = runTest {
        val body = "{\"message\":\"Usuario no encontrado\"}".toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { api.adminResetPassword(any()) } throws HttpException(Response.error<Any>(404, body))

        val result = repository.resetUserPassword("missing@una.ac.cr")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error as AppError.Server
        assertEquals("Usuario no encontrado", error.message)
    }

    @Test
    fun `getUsers maps the role and falls back to the email when there is no name`() = runTest {
        coEvery { api.getAdminUsers() } returns ApiResponse(
            listOf(
                UserDto(id = 1L, email = "ana@una.ac.cr", firstName = "Ana", lastName = "Mora", role = "Teacher"),
                UserDto(id = 2L, email = "x@est.una.ac.cr", firstName = null, lastName = null, role = "Unknown")
            )
        )

        val users = (repository.getUsers() as Result.Success).value

        assertEquals(AdminUser(1L, "ana@una.ac.cr", "Ana Mora", GlifoRole.Teacher), users[0])
        assertEquals(AdminUser(2L, "x@est.una.ac.cr", "x@est.una.ac.cr", GlifoRole.Student), users[1])
    }

    @Test
    fun `changeUserRole sends the role name the backend expects`() = runTest {
        coEvery { api.changeUserRole(5L, any()) } returns ApiResponse(
            UserDto(id = 5L, email = "e@est.una.ac.cr", firstName = "Eva", lastName = null, role = "Admin")
        )

        val result = repository.changeUserRole(5L, GlifoRole.Admin)

        assertEquals(GlifoRole.Admin, (result as Result.Success).value.role)
        coVerify { api.changeUserRole(5L, AdminChangeRoleRequest("Admin")) }
    }
}

package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.NoteDetailResponse
import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.domain.error.Result
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

class NoteRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val repository = NoteRepositoryImpl(api)

    private fun httpException(code: Int, body: String = ""): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    @Test
    fun `getNotesForCourse should return list of notes from api`() = runTest {
        val mockData = listOf(
            NoteResponse(id = 1L, courseId = 1L, title = "Derivadas", classDate = "2026-08-20"),
            NoteResponse(id = 2L, courseId = 1L, title = "Integrales", classDate = "2026-08-27")
        )
        coEvery { api.getNotes(1L) } returns ApiResponse(data = mockData)

        val result = repository.getNotesForCourse(1L)

        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).value.size)
        assertEquals("Derivadas", result.value[0].title)
    }

    @Test
    fun `getNotesForCourse should map IOException to NoConnection`() = runTest {
        coEvery { api.getNotes(any()) } throws IOException("Mock server unreachable")

        val result = repository.getNotesForCourse(1L)

        assertEquals(Result.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `getNote should return note on success`() = runTest {
        val noteDetail = NoteDetailResponse(
            id = 1L,
            courseId = 1L,
            title = "Derivadas",
            classDate = "2026-08-20",
            content = null
        )
        coEvery { api.getNote(1L) } returns ApiResponse(data = noteDetail)

        val result = repository.getNote(1L)

        assertTrue(result is Result.Success)
        assertEquals("Derivadas", (result as Result.Success).value.title)
    }

    @Test
    fun `getNote should map 404 to Server error`() = runTest {
        coEvery { api.getNote(999L) } throws httpException(404, "{\"message\":\"Apunte no encontrado\"}")

        val result = repository.getNote(999L)

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Server)
        assertEquals("Apunte no encontrado", (error as AppError.Server).message)
    }

    @Test
    fun `createNote should validate empty title`() = runTest {
        val result = repository.createNote(1L, "", "2026-08-20")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { api.createNote(any()) }
    }

    @Test
    fun `createNote should call api and return created note on success`() = runTest {
        val created = NoteResponse(id = 3L, courseId = 1L, title = "Límites", classDate = "2026-08-15")
        coEvery { api.createNote(any()) } returns ApiResponse(data = created)

        val result = repository.createNote(1L, "Límites", "2026-08-15")

        assertTrue(result is Result.Success)
        assertEquals(3L, (result as Result.Success).value.id)
        assertEquals("Límites", result.value.title)
    }

    @Test
    fun `deleteNote should return success when api returns successful response`() = runTest {
        coEvery { api.deleteNote(1L) } returns Response.success(Unit)

        val result = repository.deleteNote(1L)

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `deleteNote should map 404 when note does not exist`() = runTest {
        val responseBody = "{\"message\":\"Apunte no encontrado\"}".toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { api.deleteNote(999L) } returns Response.error(404, responseBody)

        val result = repository.deleteNote(999L)

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Server)
        assertEquals("Apunte no encontrado", (error as AppError.Server).message)
    }
}

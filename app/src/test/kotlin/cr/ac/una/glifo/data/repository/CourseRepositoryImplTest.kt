package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.CourseDetailResponse
import cr.ac.una.glifo.data.dto.CourseResponse
import cr.ac.una.glifo.data.dto.EnrollmentResponse
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

class CourseRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val repository = CourseRepositoryImpl(api)

    private fun httpException(code: Int, body: String = ""): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    @Test
    fun `getStudentCourses should return list of courses on success`() = runTest {
        val mockData = listOf(
            CourseResponse(id = 1L, name = "Cálculo I", code = "MAT-135", joinCode = "GLF-CALC1", term = "II-2026"),
            CourseResponse(id = 2L, name = "Física General", code = "FIS-100", joinCode = "GLF-FIS100")
        )
        coEvery { api.getCourses() } returns ApiResponse(data = mockData)

        val result = repository.getStudentCourses()

        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).value.size)
        assertEquals("Cálculo I", result.value[0].name)
        assertEquals("II-2026", result.value[0].term)
        assertEquals("", result.value[1].term)
    }

    @Test
    fun `getStudentCourses should map IOException to NoConnection`() = runTest {
        coEvery { api.getCourses() } throws IOException("No connection")

        val result = repository.getStudentCourses()

        assertEquals(Result.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `getStudentCourses should map HTTP 401 to Unauthorized`() = runTest {
        coEvery { api.getCourses() } throws httpException(401)

        val result = repository.getStudentCourses()

        assertEquals(Result.Failure(AppError.Unauthorized), result)
    }

    @Test
    fun `getCourseDetail should return course detail on success`() = runTest {
        val detail = CourseDetailResponse(
            id = 1L,
            name = "Cálculo I",
            code = "MAT-135",
            joinCode = "GLF-CALC1",
            syllabusStatus = "PUBLISHED"
        )
        coEvery { api.getCourse(1L) } returns ApiResponse(data = detail)

        val result = repository.getCourseDetail(1L)

        assertTrue(result is Result.Success)
        assertEquals("PUBLISHED", (result as Result.Success).value.syllabusStatus)
    }

    @Test
    fun `getCourseDetail should map HTTP 404 to Server error`() = runTest {
        coEvery { api.getCourse(999L) } throws httpException(404, "{\"message\":\"Curso no encontrado\"}")

        val result = repository.getCourseDetail(999L)

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Server)
        assertEquals("Curso no encontrado", (error as AppError.Server).message)
    }

    @Test
    fun `createCourse should validate empty fields before calling api`() = runTest {
        val result = repository.createCourse("", "MAT-101", "I")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { api.createCourse(any()) }
    }

    @Test
    fun `createCourse should call api and return created course`() = runTest {
        val created = CourseResponse(id = 10L, name = "Química", code = "QUI-101", joinCode = "GLF-QUI101")
        coEvery { api.createCourse(any()) } returns ApiResponse(data = created)

        val result = repository.createCourse("Química", "QUI-101", "I")

        assertTrue(result is Result.Success)
        assertEquals("GLF-QUI101", (result as Result.Success).value.joinCode)
    }

    @Test
    fun `joinCourse should return success on HTTP 201 from api`() = runTest {
        coEvery { api.joinCourse(any()) } returns ApiResponse(
            data = EnrollmentResponse(id = 10L, courseId = 1L, status = "ACTIVE")
        )

        val result = repository.joinCourse("GLF-CALC1")

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `joinCourse should map HTTP 409 conflict with server message`() = runTest {
        coEvery { api.joinCourse(any()) } throws httpException(409, "{\"message\":\"Ya estás matriculado en este curso\"}")

        val result = repository.joinCourse("ALREADY")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Server)
        assertEquals("Ya estás matriculado en este curso", (error as AppError.Server).message)
    }

    @Test
    fun `joinCourse should map HTTP 404 invalid code with server message`() = runTest {
        coEvery { api.joinCourse(any()) } throws httpException(404, "{\"message\":\"Código de curso inválido\"}")

        val result = repository.joinCourse("INVALID")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Server)
        assertEquals("Código de curso inválido", (error as AppError.Server).message)
    }

    @Test
    fun `findCourseByJoinCode should return matched course from api`() = runTest {
        val mockData = listOf(
            CourseResponse(id = 1L, name = "Cálculo I", code = "MAT-135", joinCode = "GLF-CALC1")
        )
        coEvery { api.getCourses() } returns ApiResponse(data = mockData)

        val result = repository.findCourseByJoinCode("GLF-CALC1")

        assertTrue(result is Result.Success)
        assertEquals("MAT-135", (result as Result.Success).value.code)
    }

    @Test
    fun `findCourseByJoinCode should return validation failure when code does not exist`() = runTest {
        coEvery { api.getCourses() } returns ApiResponse(data = emptyList())

        val result = repository.findCourseByJoinCode("NOT_FOUND")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Validation)
    }
}

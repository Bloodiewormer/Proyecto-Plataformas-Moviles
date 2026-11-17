package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.CoverageDeltaResponseDto
import cr.ac.una.glifo.data.dto.CoverageResponseDto
import cr.ac.una.glifo.data.dto.TopicCoverageResponseDto
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
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

class CoverageRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val repository = CoverageRepositoryImpl(api = api)

    private fun httpException(code: Int, body: String = ""): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    private val sampleCoverageResponse = CoverageResponseDto(
        courseId = 1L,
        coveragePct = 50.0f,
        stateCounts = mapOf("SOLID" to 1, "PARTIAL" to 1, "ABSENT" to 1, "UNCERTAIN" to 1),
        topics = listOf(
            TopicCoverageResponseDto(101L, 100L, "U1.1", "Límites", "SOLID", 0.90f, "2026-10-28T10:00:00Z"),
            TopicCoverageResponseDto(102L, 100L, "U1.2", "Continuidad", "PARTIAL", 0.70f, "2026-10-28T10:00:00Z"),
            TopicCoverageResponseDto(103L, 100L, "U1.3", "Derivadas", "ABSENT", 0.0f, null),
            TopicCoverageResponseDto(104L, 100L, "U1.4", "Optimización", "UNCERTAIN", 0.25f, "2026-10-28T10:00:00Z")
        )
    )

    @Test
    fun `getSummary and getTopics return mapped coverage from backend on success`() = runTest {
        coEvery { api.getCoverage(1L) } returns ApiResponse(data = sampleCoverageResponse)

        val summaryResult = repository.getSummary(1L)
        val topicsResult = repository.getTopics(1L)

        assertTrue(summaryResult is Result.Success)
        val summary = (summaryResult as Result.Success).value
        assertEquals(50, summary.coveragePercent)
        assertEquals(1, summary.solidCount)
        assertEquals(1, summary.partialCount)
        assertEquals(1, summary.absentCount)
        assertEquals(1, summary.doubtfulCount)

        assertTrue(topicsResult is Result.Success)
        val topics = (topicsResult as Result.Success).value
        assertEquals(4, topics.size)
        assertEquals(ConfidenceState.Verified, topics[0].state)
        assertEquals(ConfidenceState.Repaired, topics[1].state)
        assertEquals(ConfidenceState.Escalated, topics[2].state)
        assertEquals(ConfidenceState.Uncertain, topics[3].state)
    }

    @Test
    fun `evaluateCoverage calls POST evaluate endpoint and updates summary`() = runTest {
        val evaluated = sampleCoverageResponse.copy(
            coveragePct = 75.0f,
            stateCounts = mapOf("SOLID" to 3, "PARTIAL" to 0, "ABSENT" to 1, "UNCERTAIN" to 0)
        )
        coEvery { api.evaluateCoverage(1L) } returns ApiResponse(data = evaluated)

        val result = repository.evaluateCoverage(1L)

        assertTrue(result is Result.Success)
        assertEquals(75, (result as Result.Success).value.coveragePercent)
        assertEquals(3, result.value.solidCount)
        coVerify(exactly = 1) { api.evaluateCoverage(1L) }
    }

    @Test
    fun `getDelta calls GET coverage delta endpoint and maps changed topics`() = runTest {
        val deltaDto = CoverageDeltaResponseDto(
            courseId = 1L,
            since = "2026-10-21T00:00:00Z",
            changedCounts = mapOf("SOLID" to 1, "PARTIAL" to 0, "ABSENT" to 0, "UNCERTAIN" to 0),
            changedTopics = listOf(
                TopicCoverageResponseDto(101L, 100L, "U1.1", "Límites", "SOLID", 0.90f, "2026-10-28T10:00:00Z")
            )
        )
        coEvery { api.getCoverageDelta(1L, "2026-10-21T00:00:00Z") } returns ApiResponse(data = deltaDto)

        val result = repository.getDelta(courseId = 1L, since = "2026-10-21T00:00:00Z")

        assertTrue(result is Result.Success)
        val delta = (result as Result.Success).value
        assertTrue(delta.changedTopics.isNotEmpty())
        assertEquals(1, delta.changedTopics.size)
        assertEquals("U1.1 Límites", delta.changedTopics.single().name)
    }

    @Test
    fun `network failure maps IOException to AppError NoConnection`() = runTest {
        coEvery { api.getCoverage(1L) } throws IOException("Sin conexión")

        val result = repository.getSummary(1L)

        assertEquals(Result.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `HTTP 404 and HTTP 502 map to AppError Server`() = runTest {
        coEvery { api.getCoverage(999L) } throws httpException(404, """{"message":"Curso no encontrado"}""")

        val notFound = repository.getSummary(999L)
        assertTrue(notFound is Result.Failure)
        val notFoundError = (notFound as Result.Failure).error
        assertTrue(notFoundError is AppError.Server)
        assertEquals("404", (notFoundError as AppError.Server).code)
        assertEquals("Curso no encontrado", notFoundError.message)

        coEvery { api.evaluateCoverage(1L) } throws httpException(
            502,
            """{"code":"AI_PROVIDER_ERROR","message":"Error en el juez semántico IA-04"}"""
        )

        val badGateway = repository.evaluateCoverage(1L)
        assertTrue(badGateway is Result.Failure)
        val badGatewayError = (badGateway as Result.Failure).error
        assertTrue(badGatewayError is AppError.Server)
        assertEquals("502", (badGatewayError as AppError.Server).code)
        assertEquals("Error en el juez semántico IA-04", badGatewayError.message)
    }

    @Test
    fun `getTopicDetail and getDelta resolve localized no-active-course message from Context`() = runTest {
        val context = mockk<android.content.Context> {
            io.mockk.every { getString(cr.ac.una.glifo.R.string.coverage_error_no_active_course) } returns "Select an active course first."
        }
        val repoWithContext = CoverageRepositoryImpl(api = api, activeCourseStore = null, appContext = context)

        val topicResult = repoWithContext.getTopicDetail("Limits")
        val deltaResult = repoWithContext.getDelta(courseId = null, since = null)

        assertEquals(
            Result.Failure(AppError.Validation("courseId", "Select an active course first.")),
            topicResult
        )
        assertEquals(
            Result.Failure(AppError.Validation("courseId", "Select an active course first.")),
            deltaResult
        )
    }
}

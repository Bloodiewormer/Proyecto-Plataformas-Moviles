package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.datasource.local.entity.ProcessedPageEntity
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.AiUsageByCallTypeDto
import cr.ac.una.glifo.data.dto.AiUsageResponseDto
import cr.ac.una.glifo.data.dto.AiUsageTotalsDto
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal

class AiCostRepositoryImplTest {

    private val processedPageDao = mockk<ProcessedPageDao>(relaxed = true)
    private val api = mockk<GlifoApi>()
    private val repository = AiCostRepositoryImpl(processedPageDao, api)

    private fun httpException(code: Int, body: String = ""): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    private val sampleLocalPage = ProcessedPageEntity(
        id = 1L,
        courseId = 1L,
        perceptualHash = "abc1234567890def",
        photoPath = "/p.jpg",
        analysisJson = PageAnalysis(
            regions = listOf(
                RecognizedRegion(
                    index = 0,
                    bounds = RegionBounds(0, 0, 100, 50),
                    text = "Definición de límite",
                    lineCount = 1,
                    ocrConfidence = 0.92f,
                    kind = RegionKind.Text,
                    kindConfidence = 0.95f,
                    route = "N1 · ML Kit",
                    confidenceState = ConfidenceState.Verified,
                    metrics = RegionInkMetrics(0.2f, 10, 2.0f)
                )
            ),
            pageWidth = 1000,
            pageHeight = 1400,
            ocrMillis = 50L,
            totalMillis = 120L
        ).toSnapshotJson(),
        processedAt = 1000L
    )

    private val sampleUsageDto = AiUsageResponseDto(
        scope = "USER",
        courseId = 1L,
        totals = AiUsageTotalsDto(
            totalCalls = 3L,
            successCalls = 3L,
            failedCalls = 0L,
            avoidedCalls = 2L,
            inputTokens = 1200L,
            outputTokens = 300L,
            estimatedCostUsd = BigDecimal("0.00036000"),
            unknownCostCalls = 0L
        ),
        byCallType = listOf(
            AiUsageByCallTypeDto(
                callType = "IA_01",
                totalCalls = 3L,
                successCalls = 3L,
                inputTokens = 1200L,
                outputTokens = 300L,
                estimatedCostUsd = BigDecimal("0.00036000")
            )
        )
    )

    @Test
    fun `getStats for student combines local metrics with getMyAiUsage on success`() = runTest {
        coEvery { processedPageDao.all() } returns listOf(sampleLocalPage)
        coEvery { api.getMyAiUsage(1L) } returns ApiResponse(data = sampleUsageDto)

        val result = repository.getStats(GlifoRole.Student, courseId = 1L)

        assertTrue(result is Result.Success)
        val stats = (result as Result.Success).value
        assertTrue(stats.serverDataAvailable)
        assertEquals(100, stats.headlinePercent)
        assertEquals(3L, stats.totalCalls)
        assertEquals(2L, stats.avoidedCallsCount)
        assertEquals(1500L, stats.totalTokens)
        assertEquals("$0.00036 USD", stats.estimatedCostLabel)
        coVerify(exactly = 1) { api.getMyAiUsage(1L) }
    }

    @Test
    fun `getStats for teacher calls getCourseAiUsage when courseId is provided`() = runTest {
        coEvery { processedPageDao.all() } returns emptyList()
        coEvery { api.getCourseAiUsage(1L) } returns ApiResponse(data = sampleUsageDto.copy(scope = "COURSE"))

        val result = repository.getStats(GlifoRole.Teacher, courseId = 1L)

        assertTrue(result is Result.Success)
        val stats = (result as Result.Success).value
        assertTrue(stats.serverDataAvailable)
        assertEquals(3L, stats.totalCalls)
        coVerify(exactly = 1) { api.getCourseAiUsage(1L) }
    }

    @Test
    fun `getStats without network returns local metrics with serverDataAvailable false`() = runTest {
        coEvery { processedPageDao.all() } returns listOf(sampleLocalPage)
        coEvery { api.getMyAiUsage(null) } throws IOException("Sin red")

        val result = repository.getStats(GlifoRole.Student, courseId = null)

        assertTrue(result is Result.Success)
        val stats = (result as Result.Success).value
        assertFalse(stats.serverDataAvailable)
        assertEquals(100, stats.headlinePercent)
        assertTrue(stats.paidCalls.isEmpty())
        assertEquals(
            listOf(
                cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_cost_breakdown_n1),
                cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_cost_breakdown_n15),
                cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_cost_breakdown_n2),
                cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_cost_breakdown_uncertain)
            ),
            stats.breakdown.map { it.labelText }
        )
        assertEquals(
            cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_cost_period_this_phone, 1, 1),
            stats.periodText
        )
        assertEquals(
            cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_cost_offline_footer),
            stats.footerText
        )
    }

    @Test
    fun `getStats maps HTTP 404 and HTTP 502 to AppError Server`() = runTest {
        coEvery { processedPageDao.all() } returns emptyList()
        coEvery { api.getCourseAiUsage(99L) } throws httpException(404, """{"message":"Curso no encontrado"}""")

        val notFoundResult = repository.getStats(GlifoRole.Teacher, courseId = 99L)
        assertTrue(notFoundResult is Result.Failure)
        val notFoundError = (notFoundResult as Result.Failure).error
        assertTrue(notFoundError is AppError.Server)
        assertEquals("404", (notFoundError as AppError.Server).code)
        assertEquals("Curso no encontrado", notFoundError.message)

        coEvery { api.getMyAiUsage(1L) } throws httpException(502, """{"message":"Proveedor de IA no disponible"}""")
        val badGatewayResult = repository.getStats(GlifoRole.Student, courseId = 1L)
        assertTrue(badGatewayResult is Result.Failure)
        val badGatewayError = (badGatewayResult as Result.Failure).error
        assertTrue(badGatewayError is AppError.Server)
        assertEquals("502", (badGatewayError as AppError.Server).code)
    }
}

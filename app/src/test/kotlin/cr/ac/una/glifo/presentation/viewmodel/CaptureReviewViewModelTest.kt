package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureQualityIssue
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.repository.CaptureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureReviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeCaptureRepository(
        private val checks: List<CaptureQualityCheck>,
        private val warning: Boolean
    ) : CaptureRepository {
        override suspend fun getQualityChecks(photoPath: String?): Result<List<CaptureQualityCheck>> =
            Result.Success(checks)

        override suspend fun hasQualityWarning(photoPath: String?): Result<Boolean> =
            Result.Success(warning)

        override suspend fun analyzePage(photoPath: String?, courseId: Long?): Result<PageAnalysis> =
            throw UnsupportedOperationException()

        override suspend fun getProcessingSteps(photoPath: String?, courseId: Long?): Result<List<CaptureStep>> =
            throw UnsupportedOperationException()

        override suspend fun saveAsNote(photoPath: String, courseId: Long): Result<Long> =
            throw UnsupportedOperationException()
    }

    @Test
    fun `sharp overexposed photo selects overexposed warning`() = runTest(dispatcher) {
        val checks = listOf(
            CaptureQualityCheck(
                label = "Enfoque y Nitidez",
                scoreLabel = "Enfoque óptimo",
                percent = 0.85f,
                isWarning = false
            ),
            CaptureQualityCheck(
                label = "Iluminación",
                scoreLabel = "Sobreexpuesto. Exceso de luz.",
                percent = 0.25f,
                isWarning = true
            )
        )
        val vm = CaptureReviewViewModel(FakeCaptureRepository(checks, warning = true))
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals(CaptureQualityWarning.Overexposed, data.warning)
    }

    @Test
    fun `sharp underexposed photo selects underexposed warning`() {
        val checks = listOf(
            CaptureQualityCheck("Focus & sharpness", "Sharp focus", 0.9f, isWarning = false),
            CaptureQualityCheck("Lighting", "Muy oscuro. Falta iluminación.", 0.2f, isWarning = true)
        )

        assertEquals(
            CaptureQualityWarning.Underexposed,
            selectQualityWarning(checks, hasWarning = true)
        )
    }

    @Test
    fun `blurry well-lit photo selects blurry warning`() {
        val checks = listOf(
            CaptureQualityCheck("Enfoque y Nitidez", "Imagen borrosa", 0.15f, isWarning = true),
            CaptureQualityCheck("Iluminación", "Iluminación correcta", 0.8f, isWarning = false)
        )

        assertEquals(
            CaptureQualityWarning.Blurry,
            selectQualityWarning(checks, hasWarning = true)
        )
    }

    @Test
    fun `multiple failing checks select multiple warning`() {
        val checks = listOf(
            CaptureQualityCheck("Enfoque y Nitidez", "Imagen borrosa", 0.15f, isWarning = true),
            CaptureQualityCheck("Iluminación", "Sobreexpuesto. Exceso de luz.", 0.2f, isWarning = true)
        )

        assertEquals(
            CaptureQualityWarning.Multiple,
            selectQualityWarning(checks, hasWarning = true)
        )
    }

    @Test
    fun `explicit issue field takes precedence over localized labels`() {
        val checks = listOf(
            CaptureQualityCheck(
                label = "Custom check",
                scoreLabel = "Custom label",
                percent = 0.3f,
                isWarning = true,
                issue = CaptureQualityIssue.Overexposed
            )
        )

        assertEquals(
            CaptureQualityWarning.Overexposed,
            selectQualityWarning(checks, hasWarning = true)
        )
    }

    @Test
    fun `passing photo selects null warning`() {
        val checks = listOf(
            CaptureQualityCheck("Enfoque y Nitidez", "Enfoque óptimo", 0.9f, isWarning = false),
            CaptureQualityCheck("Iluminación", "Iluminación correcta", 0.9f, isWarning = false)
        )

        assertNull(selectQualityWarning(checks, hasWarning = false))
    }

    @Test
    fun `unknown failing check falls back to generic warning`() {
        val checks = listOf(
            CaptureQualityCheck("Framing", "Cropped edge", 0.2f, isWarning = true)
        )

        assertEquals(
            CaptureQualityWarning.Generic,
            selectQualityWarning(checks, hasWarning = true)
        )
    }
}

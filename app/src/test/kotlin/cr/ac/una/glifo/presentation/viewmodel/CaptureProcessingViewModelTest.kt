package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.PageReuse
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureProcessingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeCaptureRepository(
        private val reusedAnalysis: PageAnalysis,
        private val freshAnalysis: PageAnalysis,
        private val reusedSteps: List<CaptureStep>,
        private val freshSteps: List<CaptureStep>
    ) : CaptureRepository {
        var lastAnalyzeSkipDedup: Boolean? = null
        var lastStepsSkipDedup: Boolean? = null
        var analyzeCalls = 0
        var stepsCalls = 0
        var savedNoteCourseId: Long? = null

        override suspend fun getQualityChecks(photoPath: String?): Result<List<CaptureQualityCheck>> =
            Result.Success(emptyList())

        override suspend fun hasQualityWarning(photoPath: String?): Result<Boolean> =
            Result.Success(false)

        override suspend fun analyzePage(
            photoPath: String?,
            courseId: Long?,
            skipDedup: Boolean
        ): Result<PageAnalysis> {
            analyzeCalls++
            lastAnalyzeSkipDedup = skipDedup
            return Result.Success(if (skipDedup) freshAnalysis else reusedAnalysis)
        }

        override suspend fun getProcessingSteps(
            photoPath: String?,
            courseId: Long?,
            skipDedup: Boolean
        ): Result<List<CaptureStep>> {
            stepsCalls++
            lastStepsSkipDedup = skipDedup
            return Result.Success(if (skipDedup) freshSteps else reusedSteps)
        }

        override suspend fun saveAsNote(photoPath: String, courseId: Long): Result<Long> {
            savedNoteCourseId = courseId
            return Result.Success(101L)
        }
    }

    private val reusedAnalysis = PageAnalysis(
        regions = emptyList(),
        pageWidth = 1200,
        pageHeight = 1600,
        ocrMillis = 0,
        totalMillis = 150,
        reusedFrom = PageReuse("c3a1f0e98b7d6245", 4, 1_000L)
    )

    private val freshAnalysis = PageAnalysis(
        regions = emptyList(),
        pageWidth = 1200,
        pageHeight = 1600,
        ocrMillis = 800,
        totalMillis = 1500,
        reusedFrom = null
    )

    private val reusedSteps = listOf(
        CaptureStep(state = "done", name = "Deduplicación", description = "Página ya procesada")
    )

    private val freshSteps = listOf(
        CaptureStep(state = "done", name = "Deduplicación", description = "Página nueva")
    )

    @Test
    fun `load invokes repository with skipDedup false by default and publishes success`() = runTest(dispatcher) {
        val repository = FakeCaptureRepository(reusedAnalysis, freshAnalysis, reusedSteps, freshSteps)
        val viewModel = CaptureProcessingViewModel(repository)

        viewModel.load("/path/to/photo.jpg", 1L)
        advanceUntilIdle()

        assertEquals(false, repository.lastAnalyzeSkipDedup)
        assertEquals(false, repository.lastStepsSkipDedup)
        assertEquals(1, repository.analyzeCalls)
        assertEquals(1, repository.stepsCalls)

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertNotNull(data.analysis?.reusedFrom)
        assertEquals(reusedSteps, data.steps)
    }

    @Test
    fun `processAsNewPage re-runs pipeline with skipDedup true`() = runTest(dispatcher) {
        val repository = FakeCaptureRepository(reusedAnalysis, freshAnalysis, reusedSteps, freshSteps)
        val viewModel = CaptureProcessingViewModel(repository)

        viewModel.load("/path/to/photo.jpg", 1L)
        advanceUntilIdle()

        val initialState = viewModel.state.value as UiState.Success
        assertNotNull(initialState.data.analysis?.reusedFrom)

        viewModel.processAsNewPage()
        advanceUntilIdle()

        assertEquals(true, repository.lastAnalyzeSkipDedup)
        assertEquals(true, repository.lastStepsSkipDedup)
        assertEquals(2, repository.analyzeCalls)
        assertEquals(2, repository.stepsCalls)

        val finalState = viewModel.state.value as UiState.Success
        assertNull(finalState.data.analysis?.reusedFrom)
        assertEquals(freshSteps, finalState.data.steps)
    }

    @Test
    fun `saveAsNote saves note through repository`() = runTest(dispatcher) {
        val repository = FakeCaptureRepository(reusedAnalysis, freshAnalysis, reusedSteps, freshSteps)
        val viewModel = CaptureProcessingViewModel(repository)

        var savedId: Long? = null
        viewModel.saveAsNote("/path/to/photo.jpg", 1L) { id -> savedId = id }
        advanceUntilIdle()

        assertEquals(1L, repository.savedNoteCourseId)
        assertEquals(101L, savedId)
    }
}

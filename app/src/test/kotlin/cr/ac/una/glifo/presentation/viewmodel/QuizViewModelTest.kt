package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.model.QuizResultRow
import cr.ac.una.glifo.domain.repository.StudyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class QuizViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val activeCourseId = MutableStateFlow<Long?>(1L)
    private val store = mockk<ActiveCourseStore>(relaxed = true) {
        every { activeCourseId } returns this@QuizViewModelTest.activeCourseId
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun q(
        topicTag: String,
        topicLabel: String,
        correctIndex: Int = 1
    ) = QuizQuestion(
        topicTag = topicTag,
        topicLabel = topicLabel,
        prompt = "Pregunta sobre $topicLabel",
        options = listOf("Opción 0", "Opción 1", "Opción 2"),
        correctIndex = correctIndex,
        explanation = "Explicación"
    )

    @Test
    fun `all correct answers produce score equal to total and Todo correcto headline`() = runTest(dispatcher) {
        val savedSlot = slot<QuizResultInfo>()
        val studyRepository = mockk<StudyRepository>(relaxed = true) {
            coEvery { getQuizQuestions(1L) } returns Result.Success(
                listOf(
                    q("2.1", "2.1 · Definición de límite", correctIndex = 0),
                    q("2.2", "2.2 · Continuidad", correctIndex = 2),
                    q("3.4", "3.4 · Series de potencias", correctIndex = 1)
                )
            )
            coEvery { saveQuizResult(1L, capture(savedSlot)) } returns Unit
        }

        val vm = QuizViewModel(studyRepository, store)
        advanceUntilIdle()

        vm.submitAnswer(0, questionIndex = 0)
        vm.nextQuestion()
        vm.submitAnswer(2, questionIndex = 1)
        vm.nextQuestion()
        vm.submitAnswer(1, questionIndex = 2)
        val result = vm.finishQuiz()!!
        advanceUntilIdle()

        assertEquals(3, result.score)
        assertEquals(3, result.total)
        assertEquals("¡Todo correcto!", result.headline)
        assertEquals("Fallaste 0 de 3.", result.subtext)
        assertEquals(
            listOf(
                QuizResultRow(true, "1 · 2.1 · Definición de límite"),
                QuizResultRow(true, "2 · 2.2 · Continuidad"),
                QuizResultRow(true, "3 · 3.4 · Series de potencias")
            ),
            result.review
        )
        assertNull(result.rescheduledCount)
        assertNull(result.daysUntilReturn)
        assertEquals(result, savedSlot.captured)
    }

    @Test
    fun `failures in a single topic produce Repasa headline and same-topic subtext`() = runTest(dispatcher) {
        val studyRepository = mockk<StudyRepository>(relaxed = true) {
            coEvery { getQuizQuestions(1L) } returns Result.Success(
                listOf(
                    q("2.1", "2.1 · Definición de límite", correctIndex = 0),
                    q("3.4", "3.4 · Series de potencias", correctIndex = 1),
                    q("3.4", "3.4 · Series de potencias", correctIndex = 2)
                )
            )
        }

        val vm = QuizViewModel(studyRepository, store)
        advanceUntilIdle()

        // Acierto en 2.1, fallos en las dos de 3.4
        vm.submitAnswer(0, questionIndex = 0)
        vm.submitAnswer(0, questionIndex = 1)
        vm.submitAnswer(0, questionIndex = 2)
        val result = vm.finishQuiz()!!
        advanceUntilIdle()

        assertEquals(1, result.score)
        assertEquals(3, result.total)
        assertEquals("Repasá: 3.4 · Series de potencias", result.headline)
        assertEquals("Fallaste 2 de 3. Los 2 fallos son del mismo tema.", result.subtext)
        coVerify { studyRepository.saveQuizResult(1L, result) }
    }

    @Test
    fun `failures across multiple topics highlight topic with most failures in headline`() = runTest(dispatcher) {
        val studyRepository = mockk<StudyRepository>(relaxed = true) {
            coEvery { getQuizQuestions(1L) } returns Result.Success(
                listOf(
                    q("2.1", "2.1 · Definición de límite", correctIndex = 0),
                    q("2.2", "2.2 · Continuidad", correctIndex = 1),
                    q("3.4", "3.4 · Series de potencias", correctIndex = 2),
                    q("3.4", "3.4 · Series de potencias", correctIndex = 1)
                )
            )
        }

        val vm = QuizViewModel(studyRepository, store)
        advanceUntilIdle()

        // Acierto en la 1 (2.1), fallo en la 2 (2.2), fallos en la 3 y 4 (ambas de 3.4)
        vm.submitAnswer(0, questionIndex = 0)
        vm.submitAnswer(0, questionIndex = 1)
        vm.submitAnswer(0, questionIndex = 2)
        vm.submitAnswer(0, questionIndex = 3)
        val result = vm.finishQuiz()!!
        advanceUntilIdle()

        assertEquals(1, result.score)
        assertEquals(4, result.total)
        assertEquals("Repasá: 3.4 · Series de potencias", result.headline)
        assertEquals("Fallaste 3 de 4.", result.subtext)
    }

    @Test
    fun `QuizResultViewModel shows Empty when no quiz result is saved for the course`() = runTest(dispatcher) {
        val studyRepository = mockk<StudyRepository> {
            coEvery { getQuizResult(1L) } returns Result.Success(null)
        }

        val resultVm = QuizResultViewModel(studyRepository, store)
        advanceUntilIdle()

        assertEquals(UiState.Empty, resultVm.state.value)
    }
}

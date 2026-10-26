package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Las pantallas por curso siguen al curso activo y nunca muestran datos de otro curso. */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveCourseFollowTest {

    private val dispatcher = StandardTestDispatcher()
    private val activeId = MutableStateFlow<Long?>(null)
    private val store = mockk<ActiveCourseStore>(relaxed = true) {
        every { activeCourseId } returns activeId
        every { set(any(), any()) } answers { activeId.value = firstArg() }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun quiz(topic: String) = QuizQuestion(topic, topic, "?", listOf("a", "b"), 0, "")

    @Test
    fun `quiz without an active course is empty, not another course's quiz`() = runTest(dispatcher) {
        val study = mockk<StudyRepository>()
        val vm = QuizViewModel(study, store)
        advanceUntilIdle()

        assertEquals(UiState.Empty, vm.state.value)
        verify(exactly = 0) { store.set(any(), any()) }
    }

    @Test
    fun `quiz reloads for the new course after a switch`() = runTest(dispatcher) {
        val study = mockk<StudyRepository> {
            coEvery { getQuizQuestions(1L) } returns Result.Success(listOf(quiz("calculo")))
            coEvery { getQuizQuestions(2L) } returns Result.Success(listOf(quiz("moviles")))
        }
        activeId.value = 1L
        val vm = QuizViewModel(study, store)
        advanceUntilIdle()
        assertEquals("calculo", (vm.state.value as UiState.Success).data.single().topicTag)

        activeId.value = 2L
        advanceUntilIdle()
        assertEquals("moviles", (vm.state.value as UiState.Success).data.single().topicTag)
    }

    @Test
    fun `flashcards of a slow old course never overwrite the new course`() = runTest(dispatcher) {
        val study = mockk<StudyRepository> {
            coEvery { getFlashcards(1L) } coAnswers { delay(1_000); Result.Success(listOf(Flashcard("calculo", "", ""))) }
            coEvery { getFlashcards(2L) } returns Result.Success(listOf(Flashcard("moviles", "", "")))
        }
        activeId.value = 1L
        val vm = FlashcardViewModel(study, store)
        advanceTimeBy(100)
        activeId.value = 2L
        advanceUntilIdle()

        assertEquals("moviles", (vm.state.value as UiState.Success).data.single().question)
    }

    @Test
    fun `home never shows the previous course when the switch lands mid-load`() = runTest(dispatcher) {
        val courses = mockk<CourseRepository> {
            coEvery { getCourseDetail(1L) } coAnswers { delay(1_000); Result.Success(CourseDetail(1L, "Calculo", "MAT-101", "C1", null)) }
            coEvery { getCourseDetail(2L) } returns Result.Success(CourseDetail(2L, "Moviles", "EIF411", "M1", null))
            coEvery { getCourseStats(any()) } returns Result.Success(mockk<CourseStats>(relaxed = true))
        }
        val notes = mockk<NoteRepository> { coEvery { getNoteMetaList(any()) } returns Result.Success(emptyList()) }
        val sync = mockk<SyncRepository> { coEvery { getPendingCount() } returns Result.Success(0) }
        val study = mockk<StudyRepository> {
            coEvery { getFlashcards(any()) } returns Result.Success(emptyList())
            coEvery { getQuizQuestion(any()) } returns Result.Success(quiz("x"))
        }
        activeId.value = 1L
        val vm = HomeViewModel(courses, notes, sync, study, store)
        advanceTimeBy(100)
        activeId.value = 2L
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals(2L, data.course?.id)
        assertEquals(1, data.quizCount)
    }

    @Test
    fun `joining a course makes it the active course`() = runTest(dispatcher) {
        val found = Course(7L, "Fisica", "FIS-100", "GLF-FIS1")
        val courses = mockk<CourseRepository> {
            coEvery { findCourseByJoinCode("GLF-FIS1") } returns Result.Success(found)
            coEvery { getCourseStats(7L) } returns Result.Success(mockk<CourseStats>(relaxed = true))
            coEvery { joinCourse("GLF-FIS1") } returns Result.Success(Unit)
        }
        val vm = JoinCourseViewModel(courses, store)
        vm.search("GLF-FIS1")
        advanceUntilIdle()
        vm.joinCourse("GLF-FIS1")
        advanceUntilIdle()

        assertTrue(vm.joinState.value is UiState.Success)
        verify { store.set(7L, "Fisica") }
        assertEquals(7L, activeId.value)
    }
}

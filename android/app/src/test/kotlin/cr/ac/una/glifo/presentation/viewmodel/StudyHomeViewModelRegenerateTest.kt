package cr.ac.una.glifo.presentation.viewmodel

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourse
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.SyllabusTopic
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudyHomeViewModelRegenerateTest {

    private val dispatcher = StandardTestDispatcher()
    private val activeCourse = MutableStateFlow<ActiveCourse?>(ActiveCourse(id = 1L, name = "Calculo I"))
    private val store = mockk<ActiveCourseStore>(relaxed = true) {
        every { activeCourse } returns this@StudyHomeViewModelRegenerateTest.activeCourse
    }

    private val context = mockk<Context> {
        every { getString(R.string.study_notice_no_linked_notes) } returns
            "El material sale de tus apuntes de los temas del curso."
    }

    private val topic1 = SyllabusTopic(id = 101L, courseId = 1L, code = "U1.1", title = "Limites", orderIndex = 1)
    private val topic2 = SyllabusTopic(id = 102L, courseId = 1L, code = "U1.2", title = "Continuidad", orderIndex = 2)

    private val courseDetail = CourseDetail(
        id = 1L,
        name = "Calculo I",
        code = "MAT-101",
        joinCode = "GLF-CALC1",
        syllabusStatus = "READY"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `primera generacion manda regenerate false cuando no hay material previo`() = runTest(dispatcher) {
        val courseRepo = mockk<CourseRepository> {
            coEvery { getCourseDetail(1L) } returns Result.Success(courseDetail)
            coEvery { getCourseTopics(1L) } returns Result.Success(listOf(topic1, topic2))
        }

        val regenerateSlot = slot<Boolean>()
        val studyRepo = mockk<StudyRepository> {
            coEvery { getFlashcards(1L) } returns Result.Success(emptyList())
            coEvery { getQuizQuestions(1L) } returns Result.Success(emptyList())
            coEvery {
                generateStudyItems(1L, any(), any(), capture(regenerateSlot))
            } returns Result.Success(Unit)
        }

        val noteRepo = mockk<NoteRepository> {
            coEvery { getNotesForCourse(1L) } returns Result.Success(
                listOf(
                    Note(id = 5L, courseId = 1L, syllabusTopicId = 101L, classDate = "2026-09-16", title = "Apunte 1")
                )
            )
        }

        val vm = StudyHomeViewModel(courseRepo, studyRepo, store, context, noteRepo)
        vm.load(1L)
        advanceUntilIdle()

        val initialData = (vm.state.value as UiState.Success).data
        assertFalse(initialData.hasMaterial)

        vm.generateStudyMaterial(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { studyRepo.generateStudyItems(1L, listOf(101L, 102L), any(), false) }
        assertFalse(regenerateSlot.captured)
    }

    @Test
    fun `regeneracion manda regenerate true cuando ya existe material`() = runTest(dispatcher) {
        val courseRepo = mockk<CourseRepository> {
            coEvery { getCourseDetail(1L) } returns Result.Success(courseDetail)
            coEvery { getCourseTopics(1L) } returns Result.Success(listOf(topic1, topic2))
        }

        val existingFlashcard = Flashcard("Prompt", "Answer", "U1.1")
        val regenerateSlot = slot<Boolean>()
        val studyRepo = mockk<StudyRepository> {
            coEvery { getFlashcards(1L) } returns Result.Success(listOf(existingFlashcard))
            coEvery { getQuizQuestions(1L) } returns Result.Success(emptyList())
            coEvery {
                generateStudyItems(1L, any(), any(), capture(regenerateSlot))
            } returns Result.Success(Unit)
        }

        val noteRepo = mockk<NoteRepository> {
            coEvery { getNotesForCourse(1L) } returns Result.Success(
                listOf(
                    Note(id = 5L, courseId = 1L, syllabusTopicId = 101L, classDate = "2026-09-16", title = "Apunte 1")
                )
            )
        }

        val vm = StudyHomeViewModel(courseRepo, studyRepo, store, context, noteRepo)
        vm.load(1L)
        advanceUntilIdle()

        val loadedData = (vm.state.value as UiState.Success).data
        assertTrue(loadedData.hasMaterial)

        vm.generateStudyMaterial(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { studyRepo.generateStudyItems(1L, listOf(101L, 102L), any(), true) }
        assertTrue(regenerateSlot.captured)
        assertNull((vm.state.value as UiState.Success).data.noticeMessage)
    }

    @Test
    fun `si no hay apuntes ligados al temario muestra noticeMessage explicativo`() = runTest(dispatcher) {
        val courseRepo = mockk<CourseRepository> {
            coEvery { getCourseDetail(1L) } returns Result.Success(courseDetail)
            coEvery { getCourseTopics(1L) } returns Result.Success(listOf(topic1, topic2))
        }

        val studyRepo = mockk<StudyRepository> {
            coEvery { getFlashcards(1L) } returns Result.Success(emptyList())
            coEvery { getQuizQuestions(1L) } returns Result.Success(emptyList())
            coEvery { generateStudyItems(1L, any(), any(), any()) } returns Result.Success(Unit)
        }

        // Sin apuntes o apuntes sin tema asignado (syllabusTopicId == null)
        val noteRepo = mockk<NoteRepository> {
            coEvery { getNotesForCourse(1L) } returns Result.Success(
                listOf(
                    Note(id = 8L, courseId = 1L, syllabusTopicId = null, classDate = "2026-09-20", title = "Borrador suelto")
                )
            )
        }

        val vm = StudyHomeViewModel(courseRepo, studyRepo, store, context, noteRepo)
        vm.load(1L)
        advanceUntilIdle()

        vm.generateStudyMaterial(1L)
        advanceUntilIdle()

        val finalData = (vm.state.value as UiState.Success).data
        assertEquals(
            "El material sale de tus apuntes de los temas del curso.",
            finalData.noticeMessage
        )
    }
}

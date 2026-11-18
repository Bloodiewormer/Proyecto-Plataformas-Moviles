package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val activeCourseIdFlow = MutableStateFlow<Long?>(1L)
    private val pendingCountFlow = MutableSharedFlow<Int>(replay = 1)

    private val courseRepository = mockk<CourseRepository>()
    private val noteRepository = mockk<NoteRepository>()
    private val syncRepository = mockk<SyncRepository>()
    private val studyRepository = mockk<StudyRepository>()
    private val activeCourseStore = mockk<ActiveCourseStore>(relaxed = true) {
        every { activeCourseId } returns activeCourseIdFlow
    }

    private val note1 = NoteMeta(
        note = Note(id = 101L, courseId = 1L, title = "Clase 1", classDate = "2026-11-01"),
        chips = emptyList(),
        pageCount = 2,
        displayDate = "1 nov"
    )
    private val note2 = NoteMeta(
        note = Note(id = 102L, courseId = 1L, title = "Clase 2", classDate = "2026-11-03"),
        chips = emptyList(),
        pageCount = 3,
        displayDate = "3 nov"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        pendingCountFlow.tryEmit(0)
        every { syncRepository.observePendingCount() } returns pendingCountFlow
        coEvery { syncRepository.getPendingCount() } returns Result.Success(0)
        coEvery { courseRepository.getCourseDetail(1L) } returns Result.Success(CourseDetail(1L, "Calculo", "MAT-101", "C1", null))
        coEvery { courseRepository.getCourseStats(1L) } returns Result.Success(mockk<CourseStats>(relaxed = true))
        coEvery { noteRepository.getNoteMetaList(1L) } returns Result.Success(listOf(note1))
        coEvery { studyRepository.getFlashcards(1L) } returns Result.Success(emptyList())
        coEvery { studyRepository.getQuizQuestion(1L) } returns Result.Failure(mockk(relaxed = true))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load populates HomeUiData with notes, course detail, and pending sync count`() = runTest(dispatcher) {
        val vm = HomeViewModel(courseRepository, noteRepository, syncRepository, studyRepository, activeCourseStore)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("Calculo", data.course?.name)
        assertEquals(listOf(note1), data.recentNotes)
        assertEquals(0, data.pendingSyncCount)
    }

    @Test
    fun `pending count increase updates live state without triggering note reload`() = runTest(dispatcher) {
        val vm = HomeViewModel(courseRepository, noteRepository, syncRepository, studyRepository, activeCourseStore)
        advanceUntilIdle()

        pendingCountFlow.emit(2)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals(2, data.pendingSyncCount)
        coVerify(exactly = 1) { noteRepository.getNoteMetaList(1L) }
    }

    @Test
    fun `pending count decrease reloads notes when captures finish syncing`() = runTest(dispatcher) {
        pendingCountFlow.tryEmit(2)
        val vm = HomeViewModel(courseRepository, noteRepository, syncRepository, studyRepository, activeCourseStore)
        advanceUntilIdle()

        coEvery { noteRepository.getNoteMetaList(1L) } returns Result.Success(listOf(note1, note2))

        pendingCountFlow.emit(1)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals(1, data.pendingSyncCount)
        assertEquals(listOf(note1, note2), data.recentNotes)
        coVerify(exactly = 2) { noteRepository.getNoteMetaList(1L) }
    }

    @Test
    fun `pending count transition from greater than 0 to 0 emits syncCompletedEvent and reloads notes`() = runTest(dispatcher) {
        pendingCountFlow.tryEmit(1)
        val vm = HomeViewModel(courseRepository, noteRepository, syncRepository, studyRepository, activeCourseStore)
        advanceUntilIdle()

        val emittedEvents = mutableListOf<Unit>()
        val eventsJob = launch { vm.syncCompletedEvent.toList(emittedEvents) }

        pendingCountFlow.emit(0)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals(0, data.pendingSyncCount)
        assertEquals(1, emittedEvents.size)

        eventsJob.cancel()
    }
}

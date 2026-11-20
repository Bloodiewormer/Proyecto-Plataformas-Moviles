package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.repository.NoteRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
class NotesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val pendingCountFlow = MutableSharedFlow<Int>(replay = 1)

    private val noteRepository = mockk<NoteRepository>()
    private val syncRepository = mockk<SyncRepository>()

    private val note1 = NoteMeta(
        note = Note(id = 201L, courseId = 5L, title = "Apunte 1", classDate = "2026-11-01"),
        chips = emptyList(),
        pageCount = 1,
        displayDate = "1 nov"
    )
    private val note2 = NoteMeta(
        note = Note(id = 202L, courseId = 5L, title = "Apunte 2", classDate = "2026-11-02"),
        chips = emptyList(),
        pageCount = 4,
        displayDate = "2 nov"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        pendingCountFlow.tryEmit(0)
        every { syncRepository.observePendingCount() } returns pendingCountFlow
        coEvery { syncRepository.getPendingCount() } returns Result.Success(0)
        coEvery { noteRepository.getNoteMetaList(5L) } returns Result.Success(listOf(note1))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load populates notes and pending sync count`() = runTest(dispatcher) {
        val vm = NotesViewModel(noteRepository, syncRepository)
        vm.load(5L)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(listOf(note1), data.notes)
        assertEquals(0, data.pendingSyncCount)
    }

    @Test
    fun `live count increase updates pending sync count without reloading notes`() = runTest(dispatcher) {
        val vm = NotesViewModel(noteRepository, syncRepository)
        vm.load(5L)
        advanceUntilIdle()

        pendingCountFlow.emit(3)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals(3, data.pendingSyncCount)
        coVerify(exactly = 1) { noteRepository.getNoteMetaList(5L) }
    }

    @Test
    fun `live count decrease automatically reloads notes for current course`() = runTest(dispatcher) {
        pendingCountFlow.tryEmit(2)
        val vm = NotesViewModel(noteRepository, syncRepository)
        vm.load(5L)
        advanceUntilIdle()

        coEvery { noteRepository.getNoteMetaList(5L) } returns Result.Success(listOf(note1, note2))

        pendingCountFlow.emit(1)
        advanceUntilIdle()

        val data = (vm.state.value as UiState.Success).data
        assertEquals(1, data.pendingSyncCount)
        assertEquals(listOf(note1, note2), data.notes)
        coVerify(exactly = 2) { noteRepository.getNoteMetaList(5L) }
    }

    @Test
    fun `error during notes loading surfaces UiState Error`() = runTest(dispatcher) {
        coEvery { noteRepository.getNoteMetaList(5L) } returns Result.Failure(AppError.NoConnection)
        val vm = NotesViewModel(noteRepository, syncRepository)
        vm.load(5L)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UiState.Error)
        assertEquals(AppError.NoConnection, (state as UiState.Error).error)
    }
}

package cr.ac.una.glifo.data.repository

import com.google.gson.JsonParser
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.sync.SyncScheduler
import cr.ac.una.glifo.data.datasource.local.GlifoDatabase
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.FragmentCorrectionEntity
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.NoteDetailResponse
import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.NoteContentSource
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import cr.ac.una.glifo.domain.repository.CourseContextRepository
import cr.ac.una.glifo.presentation.viewmodel.NoteContentViewModel
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class NoteRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val noteDao = mockk<NoteDao>(relaxed = true)
    private val syncDao = mockk<SyncOperationDao>(relaxed = true)
    private val repository = NoteRepositoryImpl(
        api = api,
        database = mockk<GlifoDatabase>(),
        noteDao = noteDao,
        syncDao = syncDao,
        syncScheduler = mockk<SyncScheduler>(relaxed = true)
    )

    private val localNote = NoteEntity(id = 7L, serverId = null, courseId = 1L, title = "Apunte 30/09", classDate = "2026-09-30", status = "PROCESSING", createdAt = 0L)
    private val remoteNote = NoteEntity(id = 8L, serverId = 42L, courseId = 1L, title = "Derivadas", classDate = "2026-08-20", status = "READY", createdAt = 0L)

    private val analysis = PageAnalysis(
        regions = listOf(
            RecognizedRegion(0, RegionBounds(0, 0, 100, 20), "Límite de una función", 1, 0.93f, RegionKind.Text, 0.9f, "N1 · verificado", ConfidenceState.Verified, RegionInkMetrics(0.1f, 20, 5f)),
            RecognizedRegion(1, RegionBounds(0, 30, 100, 20), "converjencia", 1, 0.41f, RegionKind.Text, 0.8f, "N2 · reparación", ConfidenceState.Uncertain, RegionInkMetrics(0.1f, 12, 5f)),
            RecognizedRegion(2, RegionBounds(0, 60, 80, 30), "lim x→a", 1, 0.5f, RegionKind.Math, 0.7f, "N1.5 · OCR matemático", ConfidenceState.Repaired, RegionInkMetrics(0.2f, 9, 2.6f))
        ),
        pageWidth = 1200, pageHeight = 1600, ocrMillis = 800, totalMillis = 1500
    )
    private val page = NotePageEntity(id = 3L, noteId = 7L, pageIndex = 0, perceptualHash = "c3a1f0e98b7d6245", photoPath = "/p.jpg", levelReached = "N1", overallConfidence = 0.67f, analysisJson = analysis.toSnapshotJson(), processedAt = 0L)

    @Test
    fun `remote notes are cached in Room and read back from there`() = runTest {
        val cached = slot<List<NoteEntity>>()
        coEvery { api.getNotes(1L) } returns ApiResponse(listOf(NoteResponse(42L, 1L, "Derivadas", "2026-08-20", "READY")))
        coEvery { noteDao.cacheRemoteNotes(1L, capture(cached)) } returns Unit
        coEvery { noteDao.notesForCourse(1L) } returns listOf(remoteNote, localNote)

        val result = repository.getNotesForCourse(1L)

        assertEquals(42L, cached.captured.single().serverId)
        // La lista incluye también la captura local que el servidor todavía no tiene.
        assertEquals(listOf(8L, 7L), (result as Result.Success).value.map { it.id })
    }

    @Test
    fun `offline with cached notes returns the Room copy instead of an error`() = runTest {
        coEvery { api.getNotes(1L) } throws IOException("sin red")
        coEvery { noteDao.notesForCourse(1L) } returns listOf(remoteNote)

        val result = repository.getNotesForCourse(1L)

        assertEquals(listOf("Derivadas"), (result as Result.Success).value.map { it.title })
    }

    @Test
    fun `offline with nothing cached reports NoConnection`() = runTest {
        coEvery { api.getNotes(1L) } throws IOException("sin red")
        coEvery { noteDao.notesForCourse(1L) } returns emptyList()

        assertEquals(Result.Failure(AppError.NoConnection), repository.getNotesForCourse(1L))
    }

    @Test
    fun `content of a captured note comes from the stored pipeline analysis`() = runTest {
        coEvery { noteDao.noteById(7L) } returns localNote
        coEvery { noteDao.pagesForNote(7L) } returns listOf(page)
        coEvery { noteDao.correctionsForNote(7L) } returns emptyList()
        coEvery { syncDao.pendingForNote(7L) } returns 2

        val content = (repository.getNoteContent(7L) as Result.Success).value

        assertEquals(NoteContentSource.LocalPipeline, content.source)
        assertEquals(listOf("p0-r0", "p0-r1", "p0-r2"), content.fragments.map { it.id })
        assertEquals(1, content.uncertainCount)
        assertEquals("p0-r1", content.weakestFragment?.id)
        assertEquals("p0-r2", content.firstFormula?.id)
        assertEquals(1200, content.page(0)?.analyzedWidth)
        assertTrue(content.hasPendingSync)
    }

    @Test
    fun `a saved correction replaces the text and marks the fragment verified`() = runTest {
        coEvery { noteDao.noteById(7L) } returns localNote
        coEvery { noteDao.pagesForNote(7L) } returns listOf(page)
        coEvery { noteDao.correctionsForNote(7L) } returns listOf(
            FragmentCorrectionEntity(7L, "p0-r1", "convergencia", "converjencia", 0L)
        )

        val fragment = (repository.getNoteContent(7L) as Result.Success).value.fragment("p0-r1")!!

        assertEquals("convergencia", fragment.displayText)
        assertEquals(ConfidenceState.Verified, fragment.state)
    }

    @Test
    fun `saving a correction stores the original text next to the new one`() = runTest {
        val saved = slot<FragmentCorrectionEntity>()
        coEvery { noteDao.noteById(7L) } returns localNote
        coEvery { noteDao.pagesForNote(7L) } returns listOf(page)
        coEvery { noteDao.correctionsForNote(7L) } returns emptyList()
        coEvery { noteDao.upsertCorrection(capture(saved)) } returns Unit

        val result = repository.saveCorrection(7L, "p0-r1", "  convergencia ")

        assertEquals(Result.Success(Unit), result)
        assertEquals("convergencia", saved.captured.correctedText)
        assertEquals("converjencia", saved.captured.originalText)
    }

    @Test
    fun `a blank correction is rejected`() = runTest {
        val result = repository.saveCorrection(7L, "p0-r1", "   ")

        assertTrue((result as Result.Failure).error is AppError.Validation)
    }

    @Test
    fun `a server note without local pages shows the stored IA-01 reconstruction`() = runTest {
        val contentJson = """{"schemaVersion":1,"blocks":[{"id":"b1","type":"HEADING","text":"Límites"},{"id":"b2","type":"PARAGRAPH","spans":[{"text":"[ilegible]","confidence":0.2,"resolvedAt":"UNRESOLVED","isUncertain":true}]}]}"""
        coEvery { noteDao.noteById(8L) } returns remoteNote
        coEvery { noteDao.pagesForNote(8L) } returns emptyList()
        coEvery { noteDao.correctionsForNote(8L) } returns emptyList()
        coEvery { api.getNote(42L) } returns ApiResponse(
            NoteDetailResponse(42L, 1L, "Derivadas", "2026-08-20", "READY", JsonParser.parseString(contentJson))
        )

        val content = (repository.getNoteContent(8L) as Result.Success).value

        assertEquals(NoteContentSource.ServerReconstruction, content.source)
        assertEquals(listOf("Límites", "[ilegible]"), content.fragments.map { it.text })
        assertEquals(ConfidenceState.Uncertain, content.fragments[1].state)
        coVerify { noteDao.setServerContent(8L, any(), "READY") }
    }

    @Test
    fun `a note with no pages and no reconstruction says so instead of inventing content`() = runTest {
        coEvery { noteDao.noteById(8L) } returns remoteNote
        coEvery { noteDao.pagesForNote(8L) } returns emptyList()
        coEvery { noteDao.correctionsForNote(8L) } returns emptyList()
        coEvery { api.getNote(42L) } returns ApiResponse(NoteDetailResponse(42L, 1L, "Derivadas", "2026-08-20", "READY", null))

        val content = (repository.getNoteContent(8L) as Result.Success).value

        assertEquals(NoteContentSource.Unavailable, content.source)
        assertTrue(content.fragments.isEmpty())
    }

    @Test
    fun `deleting a note that never reached the server does not call the API`() = runTest {
        coEvery { noteDao.noteById(7L) } returns localNote

        assertEquals(Result.Success(Unit), repository.deleteNote(7L))
        coVerify(exactly = 0) { api.deleteNote(any()) }
        coVerify { noteDao.deleteNote(7L) }
    }

    @Test
    fun `an unknown note id is a 404`() = runTest {
        coEvery { noteDao.noteById(99L) } returns null

        val error = (repository.getNoteContent(99L) as Result.Failure).error

        assertEquals("404", (error as AppError.Server).code)
    }

    @Test
    fun `reconstructNote succeeds and saves content and READY in Room`() = runTest {
        val syncedProcessing = localNote.copy(serverId = 42L)
        val contentJson = """{"schemaVersion":1,"blocks":[{"id":"b1","type":"HEADING","text":"Límites"},{"id":"b2","type":"PARAGRAPH","spans":[{"text":"convergencia","confidence":0.85,"resolvedAt":"N1","isUncertain":false}]}],"summary":"Resumen"}"""
        coEvery { noteDao.noteById(7L) } returns syncedProcessing
        coEvery { noteDao.pagesForNote(7L) } returns listOf(page)
        coEvery { noteDao.correctionsForNote(7L) } returns emptyList()
        coEvery { api.reconstructNote(42L) } returns ApiResponse(
            NoteDetailResponse(42L, 1L, "Apunte 30/09", "2026-09-30", "READY", JsonParser.parseString(contentJson))
        )

        val result = repository.reconstructNote(7L)

        val content = (result as Result.Success).value
        assertEquals(NoteContentSource.ServerReconstruction, content.source)
        assertEquals("READY", content.status)
        assertEquals(listOf("Límites", "convergencia"), content.fragments.map { it.text })
        coVerify(exactly = 1) { api.reconstructNote(42L) }
        coVerify(exactly = 1) { noteDao.setServerContent(7L, match { it.contains("schemaVersion") }, "READY") }
    }

    @Test
    fun `reconstructNote on a note without serverId returns error without calling the API`() = runTest {
        coEvery { noteDao.noteById(7L) } returns localNote

        val result = repository.reconstructNote(7L)

        assertTrue((result as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { api.reconstructNote(any()) }
        coVerify(exactly = 0) { noteDao.setServerContent(any(), any(), any()) }
    }

    @Test
    fun `reconstructNote maps 502 AI error to AppError Server`() = runTest {
        val syncedProcessing = localNote.copy(serverId = 42L)
        val errorBody = """{"error":{"code":"AI_PROVIDER_ERROR","message":"Error del proveedor de IA"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { noteDao.noteById(7L) } returns syncedProcessing
        coEvery { api.reconstructNote(42L) } throws HttpException(Response.error<Any>(502, errorBody))

        val result = repository.reconstructNote(7L)

        val error = (result as Result.Failure).error as AppError.Server
        assertEquals("502", error.code)
        coVerify(exactly = 0) { noteDao.setServerContent(any(), any(), any()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when SyncWorker finishes, NoteContentViewModel clears pendingSync and enables canReconstruct`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val noteFlow = MutableStateFlow<NoteEntity?>(localNote)
            val pendingFlow = MutableStateFlow(2)

            coEvery { noteDao.noteById(7L) } answers { noteFlow.value }
            every { noteDao.observeNoteById(7L) } returns noteFlow
            coEvery { syncDao.pendingForNote(7L) } answers { pendingFlow.value }
            every { syncDao.observePendingForNote(7L) } returns pendingFlow
            coEvery { noteDao.pagesForNote(7L) } returns listOf(page)
            coEvery { noteDao.correctionsForNote(7L) } returns emptyList()

            val viewModel = NoteContentViewModel(
                noteRepository = repository,
                courseContextRepository = mockk<CourseContextRepository>(relaxed = true)
            )

            // Al abrir NoteDetailScreen recién guardada la captura, el worker todavía está corriendo.
            viewModel.load(7L, force = true)
            advanceUntilIdle()

            val initial = (viewModel.state.value as UiState.Success).data.content
            assertTrue(initial.hasPendingSync)
            assertNull(initial.serverId)
            assertFalse(initial.canReconstruct)

            // El SyncWorker termina CREATE_NOTE + UPLOAD_PAGE (serverId = 3) y reconstruct da 502,
            // por lo que la nota queda en PROCESSING en Room y sin operaciones pendientes.
            noteFlow.value = localNote.copy(serverId = 3L, status = "PROCESSING")
            pendingFlow.value = 0
            advanceUntilIdle()

            val afterSync = (viewModel.state.value as UiState.Success).data.content
            assertFalse(afterSync.hasPendingSync)
            assertEquals(3L, afterSync.serverId)
            assertEquals("PROCESSING", afterSync.status)
            assertTrue(afterSync.canReconstruct)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

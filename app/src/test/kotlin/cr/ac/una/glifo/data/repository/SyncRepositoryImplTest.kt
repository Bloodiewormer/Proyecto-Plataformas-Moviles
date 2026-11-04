package cr.ac.una.glifo.data.repository

import com.google.gson.JsonParser
import cr.ac.una.glifo.core.sync.SyncScheduler
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.MathOcrResponseDto
import cr.ac.una.glifo.data.dto.NoteDetailResponse
import cr.ac.una.glifo.data.dto.NotePageResponse
import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.data.dto.PageRepairResponseDto
import cr.ac.una.glifo.data.dto.RepairedRegionDto
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import cr.ac.una.glifo.domain.model.SyncRunOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SyncRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val syncDao = mockk<SyncOperationDao>(relaxed = true)
    private val noteDao = mockk<NoteDao>(relaxed = true)
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val repository = SyncRepositoryImpl(api, syncDao, noteDao, scheduler)

    private val emptyAnalysis = PageAnalysis(emptyList(), 1200, 1600, 0, 0).toSnapshotJson()
    private val page = NotePageEntity(3L, 7L, 0, "c3a1f0e98b7d6245", "/p.jpg", "N1", 0.8f, emptyAnalysis, 0L)
    private val secondPage = NotePageEntity(4L, 7L, 1, "a1b2c3d4e5f60718", "/p2.jpg", "N1", 0.9f, emptyAnalysis, 0L)
    private val unsynced = NoteEntity(7L, null, 1L, "Apunte", "2026-09-30", "PROCESSING", null, 0L)
    private val synced = unsynced.copy(serverId = 42L)

    private fun op(id: Long, type: String, pageId: Long? = null) = SyncOperationEntity(
        id = id, entityType = type, idempotencyKey = "k$id", noteId = 7L, pageId = pageId, createdAt = 0L, updatedAt = 0L
    )

    private val createOp = op(1L, SyncOperationEntity.TYPE_CREATE_NOTE)
    private val uploadOp = op(2L, SyncOperationEntity.TYPE_UPLOAD_PAGE, pageId = 3L)
    private val secondUploadOp = op(3L, SyncOperationEntity.TYPE_UPLOAD_PAGE, pageId = 4L)

    private fun httpException(code: Int) =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaTypeOrNull())))

    @Test
    fun `creates the note first, stores its server id and then uploads the page`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(createOp, uploadOp)
        coEvery { noteDao.noteById(7L) } returnsMany listOf(unsynced, synced)
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { api.createNote(any()) } returns ApiResponse(NoteResponse(42L, 1L, "Apunte", "2026-09-30"))
        coEvery { api.uploadPage(42L, any()) } returns ApiResponse(NotePageResponse(9L, 42L, 0))

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(2, summary.uploaded)
        assertEquals(SyncRunOutcome.Completed, summary.outcome)
        coVerifyOrder {
            api.createNote(any())
            noteDao.setServerId(7L, 42L)
            api.uploadPage(42L, match { it.perceptualHash == "c3a1f0e98b7d6245" && it.levelReached == "N1" })
        }
        coVerify { syncDao.update(1L, SyncOperationEntity.STATUS_DONE, 1, null, any()) }
        coVerify { syncDao.update(2L, SyncOperationEntity.STATUS_DONE, 1, null, any()) }
    }

    @Test
    fun `without network the run stops and asks WorkManager to retry`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(createOp, uploadOp)
        coEvery { noteDao.noteById(7L) } returns unsynced
        coEvery { api.createNote(any()) } throws IOException("sin red")

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(SyncRunOutcome.RetryLater, summary.outcome)
        coVerify { syncDao.update(1L, SyncOperationEntity.STATUS_PENDING, 1, "Sin conexión con el servidor", any()) }
        coVerify(exactly = 0) { api.uploadPage(any(), any()) }
    }

    @Test
    fun `a page the server already has (409) counts as uploaded`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { api.uploadPage(42L, any()) } throws httpException(409)

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(1, summary.uploaded)
        coVerify { syncDao.update(2L, SyncOperationEntity.STATUS_DONE, 1, null, any()) }
    }

    @Test
    fun `a rejected operation is marked failed and the run continues`() = runTest {
        val otherUpload = op(3L, SyncOperationEntity.TYPE_UPLOAD_PAGE, pageId = 3L)
        coEvery { syncDao.runnable() } returns listOf(uploadOp, otherUpload)
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { api.uploadPage(42L, any()) } throws httpException(400) andThen ApiResponse(NotePageResponse(9L, 42L, 0))

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(1, summary.failed)
        assertEquals(1, summary.uploaded)
        assertEquals(SyncRunOutcome.Completed, summary.outcome)
        coVerify { syncDao.update(2L, SyncOperationEntity.STATUS_FAILED, 1, "El servidor rechazó la operación (400)", any()) }
    }

    @Test
    fun `server errors are retried later, not marked failed`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { api.uploadPage(42L, any()) } throws httpException(503)

        assertEquals(SyncRunOutcome.RetryLater, (repository.syncNow() as Result.Success).value.outcome)
        coVerify(exactly = 0) { syncDao.update(any(), SyncOperationEntity.STATUS_FAILED, any(), any(), any()) }
    }

    @Test
    fun `a page waits while its note does not exist on the server`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { noteDao.noteById(7L) } returns unsynced

        repository.syncNow()

        coVerify(exactly = 0) { api.uploadPage(any(), any()) }
        coVerify { syncDao.update(2L, SyncOperationEntity.STATUS_PENDING, 0, null, any()) }
    }

    @Test
    fun `retrying failed operations requeues them and schedules a run`() = runTest {
        coEvery { syncDao.requeueFailed(any()) } returns 2

        assertEquals(Result.Success(2), repository.retryFailed())
        coVerify { scheduler.schedule() }
    }

    @Test
    fun `triggers reconstruction only after the last pending page of a note is uploaded`() = runTest {
        val contentJson = """{"schemaVersion":1,"blocks":[{"id":"b1","type":"HEADING","text":"Límites"}]}"""
        coEvery { syncDao.runnable() } returns listOf(createOp, uploadOp, secondUploadOp)
        coEvery { syncDao.pendingForNote(7L) } returns 0
        coEvery { noteDao.noteById(7L) } returnsMany listOf(unsynced, synced)
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { noteDao.pageById(4L) } returns secondPage
        coEvery { api.createNote(any()) } returns ApiResponse(NoteResponse(42L, 1L, "Apunte", "2026-09-30"))
        coEvery { api.uploadPage(42L, any()) } returns ApiResponse(NotePageResponse(9L, 42L, 0))
        coEvery { api.reconstructNote(42L) } returns ApiResponse(
            NoteDetailResponse(42L, 1L, "Apunte", "2026-09-30", "READY", JsonParser.parseString(contentJson))
        )

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(3, summary.uploaded)
        assertEquals(SyncRunOutcome.Completed, summary.outcome)
        coVerify(exactly = 1) { api.reconstructNote(42L) }
        coVerifyOrder {
            api.uploadPage(42L, match { it.pageIndex == 0 })
            api.uploadPage(42L, match { it.pageIndex == 1 })
            api.reconstructNote(42L)
            noteDao.setServerContent(7L, match { it.contains("schemaVersion") }, "READY")
        }
    }

    @Test
    fun `a failure in reconstruction does not mark the sync as failed`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { syncDao.pendingForNote(7L) } returns 0
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { api.uploadPage(42L, any()) } returns ApiResponse(NotePageResponse(9L, 42L, 0))
        coEvery { api.reconstructNote(42L) } throws httpException(502)

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(1, summary.uploaded)
        assertEquals(0, summary.failed)
        assertEquals(SyncRunOutcome.Completed, summary.outcome)
        coVerify { syncDao.update(2L, SyncOperationEntity.STATUS_DONE, 1, null, any()) }
        coVerify(exactly = 1) { api.reconstructNote(42L) }
        coVerify(exactly = 0) { noteDao.setServerContent(any(), any(), any()) }
    }

    @Test
    fun `calls transcribeMathRegion for MATH regions and repairPage once per page before reconstructNote`() = runTest {
        val metrics = RegionInkMetrics(0.18f, 12, 2.5f)
        val analysisWithRegions = PageAnalysis(
            regions = listOf(
                RecognizedRegion(
                    index = 0,
                    bounds = RegionBounds(10, 20, 200, 60),
                    text = "int x^2 dx",
                    lineCount = 1,
                    ocrConfidence = 0.65f,
                    kind = RegionKind.Math,
                    kindConfidence = 0.90f,
                    route = "N1.5 · OCR matemático",
                    confidenceState = ConfidenceState.Repaired,
                    metrics = metrics
                ),
                RecognizedRegion(
                    index = 1,
                    bounds = RegionBounds(10, 100, 300, 80),
                    text = "texto borroso",
                    lineCount = 1,
                    ocrConfidence = 0.45f,
                    kind = RegionKind.Text,
                    kindConfidence = 0.85f,
                    route = "N2 · reparación por visión",
                    confidenceState = ConfidenceState.Uncertain,
                    metrics = metrics
                ),
                RecognizedRegion(
                    index = 2,
                    bounds = RegionBounds(10, 200, 250, 120),
                    text = "",
                    lineCount = 0,
                    ocrConfidence = 0.0f,
                    kind = RegionKind.Drawing,
                    kindConfidence = 0.88f,
                    route = "N2 · reparación por visión",
                    confidenceState = ConfidenceState.Escalated,
                    metrics = metrics
                )
            ),
            pageWidth = 1200,
            pageHeight = 1600,
            ocrMillis = 40,
            totalMillis = 90
        ).toSnapshotJson()

        val pageWithRegions = page.copy(analysisJson = analysisWithRegions)
        val contentJson = """{"schemaVersion":1,"blocks":[{"id":"b1","type":"FORMULA","latex":"\\int x^2 dx","resolvedAt":"N1_5","confidence":0.94}]}"""

        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { syncDao.pendingForNote(7L) } returns 0
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns pageWithRegions
        coEvery { noteDao.pagesForNote(7L) } returns listOf(pageWithRegions)
        coEvery { api.uploadPage(42L, any()) } returns ApiResponse(NotePageResponse(9L, 42L, 0))
        coEvery { api.transcribeMathRegion(42L, 0, "r0", any()) } returns ApiResponse(
            MathOcrResponseDto(
                pageIndex = 0,
                regionId = "r0",
                latex = "\\int x^2 dx",
                confidence = 0.94f,
                latexCompiles = true,
                status = "KEEP_N1_5",
                resolvedAt = "N1_5"
            )
        )
        coEvery { api.repairPage(42L, 0, any()) } returns ApiResponse(
            PageRepairResponseDto(
                pageIndex = 0,
                levelReached = "N2",
                repaired = 2,
                uncertain = 0,
                regions = listOf(
                    RepairedRegionDto(
                        regionId = "r1",
                        kind = "TEXT",
                        value = "texto nítido",
                        confidence = 0.91f,
                        resolvedAt = "N2",
                        isUncertain = false
                    ),
                    RepairedRegionDto(
                        regionId = "r2",
                        kind = "DRAWING",
                        value = "Gráfica de parábola",
                        confidence = 0.89f,
                        resolvedAt = "N2",
                        isUncertain = false
                    )
                )
            )
        )
        coEvery { api.reconstructNote(42L) } returns ApiResponse(
            NoteDetailResponse(42L, 1L, "Apunte", "2026-09-30", "READY", JsonParser.parseString(contentJson))
        )

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(1, summary.uploaded)
        assertEquals(0, summary.failed)
        assertEquals(SyncRunOutcome.Completed, summary.outcome)

        coVerify(exactly = 1) { api.transcribeMathRegion(42L, 0, "r0", any()) }
        coVerify(exactly = 1) { api.repairPage(42L, 0, match { it.size == 2 }) }
        coVerify(exactly = 1) { api.reconstructNote(42L) }
        coVerifyOrder {
            api.uploadPage(42L, any())
            api.transcribeMathRegion(42L, 0, "r0", any())
            api.repairPage(42L, 0, any())
            noteDao.updatePageAnalysis(3L, any(), "N2")
            api.reconstructNote(42L)
        }
    }

    @Test
    fun `failure in transcribeMathRegion or repairPage does not break sync or prevent reconstructNote`() = runTest {
        val metrics = RegionInkMetrics(0.18f, 12, 2.5f)
        val analysisWithRegions = PageAnalysis(
            regions = listOf(
                RecognizedRegion(
                    index = 0,
                    bounds = RegionBounds(10, 20, 200, 60),
                    text = "x^2",
                    lineCount = 1,
                    ocrConfidence = 0.50f,
                    kind = RegionKind.Math,
                    kindConfidence = 0.90f,
                    route = "N1.5 · OCR matemático",
                    confidenceState = ConfidenceState.Repaired,
                    metrics = metrics
                ),
                RecognizedRegion(
                    index = 1,
                    bounds = RegionBounds(10, 100, 300, 80),
                    text = "dudoso",
                    lineCount = 1,
                    ocrConfidence = 0.40f,
                    kind = RegionKind.Text,
                    kindConfidence = 0.85f,
                    route = "N2 · reparación por visión",
                    confidenceState = ConfidenceState.Uncertain,
                    metrics = metrics
                )
            ),
            pageWidth = 1200,
            pageHeight = 1600,
            ocrMillis = 40,
            totalMillis = 90
        ).toSnapshotJson()

        val pageWithRegions = page.copy(analysisJson = analysisWithRegions)
        val contentJson = """{"schemaVersion":1,"blocks":[{"id":"b1","type":"HEADING","text":"Apunte"}]}"""

        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { syncDao.pendingForNote(7L) } returns 0
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns pageWithRegions
        coEvery { noteDao.pagesForNote(7L) } returns listOf(pageWithRegions)
        coEvery { api.uploadPage(42L, any()) } returns ApiResponse(NotePageResponse(9L, 42L, 0))
        coEvery { api.transcribeMathRegion(42L, 0, "r0", any()) } throws httpException(502)
        coEvery { api.repairPage(42L, 0, any()) } throws httpException(503)
        coEvery { api.reconstructNote(42L) } returns ApiResponse(
            NoteDetailResponse(42L, 1L, "Apunte", "2026-09-30", "READY", JsonParser.parseString(contentJson))
        )

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(1, summary.uploaded)
        assertEquals(0, summary.failed)
        assertEquals(SyncRunOutcome.Completed, summary.outcome)
        coVerify(exactly = 1) { api.transcribeMathRegion(42L, 0, "r0", any()) }
        coVerify(exactly = 1) { api.repairPage(42L, 0, any()) }
        coVerify(exactly = 1) { api.reconstructNote(42L) }
        coVerify { noteDao.setServerContent(7L, any(), "READY") }
    }
}

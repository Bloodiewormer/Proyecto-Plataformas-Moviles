package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.sync.SyncScheduler
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.NotePageResponse
import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.PageAnalysis
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
    private val unsynced = NoteEntity(7L, null, 1L, "Apunte", "2026-09-30", "PROCESSING", null, 0L)
    private val synced = unsynced.copy(serverId = 42L)

    private fun op(id: Long, type: String, pageId: Long? = null) = SyncOperationEntity(
        id = id, entityType = type, idempotencyKey = "k$id", noteId = 7L, pageId = pageId, createdAt = 0L, updatedAt = 0L
    )

    private val createOp = op(1L, SyncOperationEntity.TYPE_CREATE_NOTE)
    private val uploadOp = op(2L, SyncOperationEntity.TYPE_UPLOAD_PAGE, pageId = 3L)

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
}

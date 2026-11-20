package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.sync.SyncScheduler
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.STATUS_DONE
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.STATUS_FAILED
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.STATUS_IN_PROGRESS
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.STATUS_PENDING
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.TYPE_CREATE_NOTE
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity.Companion.TYPE_UPLOAD_PAGE
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.NotePageResponse
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

/**
 * Máquina de estados de una operación de la cola offline (issue #36). Complementa a
 * [SyncRepositoryImplTest], que cubre el camino feliz:
 *
 *   PENDING → IN_PROGRESS → DONE      2xx, o 409 en una página (ya estaba)
 *                         → PENDING   reintentar: sin red, 5xx, 401, 408, 429 (attempts + 1)
 *                         → PENDING   en espera: el apunte aún no existe en el servidor (attempts igual)
 *                         → FAILED    otro 4xx, datos corruptos u operación desconocida
 *   FAILED  → PENDING                 cuando el estudiante pide reintentar
 */
class SyncStateTransitionsTest {

    private val api = mockk<GlifoApi>()
    private val syncDao = mockk<SyncOperationDao>(relaxed = true)
    private val noteDao = mockk<NoteDao>(relaxed = true)
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val repository = SyncRepositoryImpl(api, syncDao, noteDao, scheduler)

    private val emptyAnalysis = PageAnalysis(emptyList(), 1200, 1600, 0, 0).toSnapshotJson()
    private val page = NotePageEntity(3L, 7L, 0, "c3a1f0e98b7d6245", "/p.jpg", "N1", 0.8f, emptyAnalysis, 0L)
    private val unsynced = NoteEntity(7L, null, 1L, "Apunte", "2026-09-30", "PROCESSING", null, 0L)
    private val synced = unsynced.copy(serverId = 42L)

    private fun op(id: Long, type: String, pageId: Long? = null, attempts: Int = 0) = SyncOperationEntity(
        id = id, entityType = type, idempotencyKey = "k$id", noteId = 7L, pageId = pageId,
        attempts = attempts, createdAt = 0L, updatedAt = 0L
    )

    private val createOp = op(1L, TYPE_CREATE_NOTE)
    private val uploadOp = op(2L, TYPE_UPLOAD_PAGE, pageId = 3L)

    private fun httpException(code: Int) =
        HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaTypeOrNull())))

    private fun givenUploadThrows(code: Int) {
        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { api.uploadPage(42L, any()) } throws httpException(code)
    }

    // ---------- PENDING → IN_PROGRESS → DONE ----------

    @Test
    fun `every operation passes through IN_PROGRESS before reaching DONE`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { api.uploadPage(42L, any()) } returns ApiResponse(NotePageResponse(9L, 42L, 0))

        repository.syncNow()

        coVerifyOrder {
            syncDao.update(2L, STATUS_IN_PROGRESS, 0, null, any())
            syncDao.update(2L, STATUS_DONE, 1, null, any())
        }
    }

    @Test
    fun `a CREATE_NOTE whose note already has a server id is DONE without calling the API again`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(createOp)
        coEvery { noteDao.noteById(7L) } returns synced

        repository.syncNow()

        coVerify(exactly = 0) { api.createNote(any()) }
        coVerify { syncDao.update(1L, STATUS_DONE, 1, null, any()) }
    }

    @Test
    fun `an operation whose note was deleted locally is closed as DONE without calling the API`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { noteDao.noteById(7L) } returns null

        repository.syncNow()

        coVerify(exactly = 0) { api.uploadPage(any(), any()) }
        coVerify { syncDao.update(2L, STATUS_DONE, 1, null, any()) }
    }

    // ---------- IN_PROGRESS → PENDING (reintentar) ----------

    @Test
    fun `an expired session (401) is retried later, never marked failed`() = runTest {
        givenUploadThrows(401)

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(SyncRunOutcome.RetryLater, summary.outcome)
        coVerify { syncDao.update(2L, STATUS_PENDING, 1, "La sesión venció; se reintenta al volver a iniciar sesión", any()) }
    }

    @Test
    fun `timeout (408) and rate limit (429) are retried later`() = runTest {
        for (code in listOf(408, 429)) {
            givenUploadThrows(code)

            val summary = (repository.syncNow() as Result.Success).value

            assertEquals(SyncRunOutcome.RetryLater, summary.outcome)
            coVerify { syncDao.update(2L, STATUS_PENDING, 1, "El servidor pidió esperar ($code)", any()) }
        }
        coVerify(exactly = 0) { syncDao.update(any(), STATUS_FAILED, any(), any(), any()) }
    }

    @Test
    fun `each retry adds one attempt to the previous count`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(uploadOp.copy(attempts = 3))
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns page
        coEvery { api.uploadPage(42L, any()) } throws httpException(500)

        repository.syncNow()

        coVerify { syncDao.update(2L, STATUS_PENDING, 4, "Error del servidor (500)", any()) }
    }

    @Test
    fun `a retry stops the run so later operations stay untouched and keep their order`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(createOp, uploadOp)
        coEvery { noteDao.noteById(7L) } returns unsynced
        coEvery { api.createNote(any()) } throws httpException(503)

        repository.syncNow()

        coVerify(exactly = 0) { syncDao.update(2L, any(), any(), any(), any()) }
    }

    @Test
    fun `an unexpected exception is retried instead of losing the operation`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(createOp)
        coEvery { noteDao.noteById(7L) } returns unsynced
        coEvery { api.createNote(any()) } throws IllegalStateException("boom")

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(SyncRunOutcome.RetryLater, summary.outcome)
        coVerify { syncDao.update(1L, STATUS_PENDING, 1, "boom", any()) }
    }

    // ---------- IN_PROGRESS → FAILED ----------

    @Test
    fun `a 409 on CREATE_NOTE is a failure, unlike a 409 on a page`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(createOp)
        coEvery { noteDao.noteById(7L) } returns unsynced
        coEvery { api.createNote(any()) } throws httpException(409)

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(1, summary.failed)
        coVerify { syncDao.update(1L, STATUS_FAILED, 1, "El servidor rechazó el apunte (409)", any()) }
    }

    @Test
    fun `a page whose saved analysis cannot be read fails without calling the API`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(uploadOp)
        coEvery { noteDao.noteById(7L) } returns synced
        coEvery { noteDao.pageById(3L) } returns page.copy(analysisJson = "{corrupto")

        repository.syncNow()

        coVerify(exactly = 0) { api.uploadPage(any(), any()) }
        coVerify { syncDao.update(2L, STATUS_FAILED, 1, "El análisis guardado de la página no se puede leer", any()) }
    }

    @Test
    fun `an unknown operation type fails instead of blocking the queue`() = runTest {
        coEvery { syncDao.runnable() } returns listOf(op(5L, "DELETE_NOTE"))
        coEvery { noteDao.noteById(7L) } returns synced

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(SyncRunOutcome.Completed, summary.outcome)
        coVerify { syncDao.update(5L, STATUS_FAILED, 1, "Operación desconocida: DELETE_NOTE", any()) }
    }

    // ---------- FAILED → PENDING ----------

    @Test
    fun `retrying with nothing failed does not schedule a run`() = runTest {
        coEvery { syncDao.requeueFailed(any()) } returns 0

        assertEquals(Result.Success(0), repository.retryFailed())
        coVerify(exactly = 0) { scheduler.schedule() }
    }

    @Test
    fun `the summary reports what is still pending after the run`() = runTest {
        coEvery { syncDao.runnable() } returns emptyList()
        coEvery { syncDao.pendingCount() } returns 4

        val summary = (repository.syncNow() as Result.Success).value

        assertEquals(4, summary.remaining)
        assertEquals(SyncRunOutcome.Completed, summary.outcome)
    }
}

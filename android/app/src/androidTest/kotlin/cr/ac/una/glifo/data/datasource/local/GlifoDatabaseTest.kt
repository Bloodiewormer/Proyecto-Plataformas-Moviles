package cr.ac.una.glifo.data.datasource.local

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cr.ac.una.glifo.core.pipeline.PerceptualHasher
import cr.ac.una.glifo.data.datasource.local.entity.LocalCourseContextEntity
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.datasource.local.entity.SyncOperationEntity
import cr.ac.una.glifo.data.datasource.local.entity.ProcessedPageEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Corre en el dispositivo o emulador: prueba las consultas reales de Room para #27 y #28 y el
 * hash perceptual sobre Bitmaps de Android, que los tests JVM no pueden crear.
 */
@RunWith(AndroidJUnit4::class)
class GlifoDatabaseTest {

    private lateinit var db: GlifoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GlifoDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() = db.close()

    private fun context(glossaryVersion: Int) = LocalCourseContextEntity(
        courseId = 1L,
        courseName = "Cálculo I",
        syllabusVersion = 2,
        glossaryVersion = glossaryVersion,
        glossaryEntries = """[{"term":"Sumatoria","canonicalForm":"\\sum","kind":"SYMBOL"}]""",
        cachedAt = 100L
    )

    @Test
    fun courseContextUpsertReplacesTheRowAndVersionsReadIt() = runBlocking {
        val dao = db.localCourseContextDao()

        dao.upsert(context(glossaryVersion = 3))
        dao.upsert(context(glossaryVersion = 4))

        assertEquals(4, dao.versions(1L)?.glossaryVersion)
        assertEquals(2, dao.versions(1L)?.syllabusVersion)
        assertEquals("Cálculo I", dao.forCourse(1L)?.courseName)
        assertNull(dao.versions(99L))
    }

    @Test
    fun touchOnlyUpdatesCachedAt() = runBlocking {
        val dao = db.localCourseContextDao()
        dao.upsert(context(glossaryVersion = 3))

        dao.touch(1L, cachedAt = 999L)

        val row = dao.forCourse(1L)!!
        assertEquals(999L, row.cachedAt)
        assertEquals(context(3).glossaryEntries, row.glossaryEntries)
    }

    @Test
    fun processedPagesAreFilteredByCourse() = runBlocking {
        val dao = db.processedPageDao()
        val id = dao.insert(ProcessedPageEntity(courseId = 1L, perceptualHash = "c3a1f0e98b7d6245", photoPath = "/a.jpg", analysisJson = "{}", processedAt = 1L))
        dao.insert(ProcessedPageEntity(courseId = 2L, perceptualHash = "0000000000000000", photoPath = "/b.jpg", analysisJson = "{}", processedAt = 1L))

        val hashes = dao.hashesForCourse(1L)

        assertEquals(1, hashes.size)
        assertEquals("c3a1f0e98b7d6245", hashes.single().perceptualHash)
        assertEquals("/a.jpg", dao.getById(id)?.photoPath)
    }

    private fun note(serverId: Long?, title: String, courseId: Long = 1L) = NoteEntity(
        serverId = serverId, courseId = courseId, title = title, classDate = "2026-09-30", status = "READY", createdAt = 0L
    )

    @Test
    fun cachingRemoteNotesUpdatesExistingOnesAndKeepsLocalCaptures() = runBlocking {
        val dao = db.noteDao()
        val localId = dao.insertNote(note(serverId = null, title = "Captura offline"))
        dao.insertPage(NotePageEntity(noteId = localId, pageIndex = 0, perceptualHash = "c3a1f0e98b7d6245", photoPath = "/a.jpg", levelReached = "N1", overallConfidence = 0.8f, analysisJson = "{}", processedAt = 0L))
        dao.cacheRemoteNotes(1L, listOf(note(42L, "Derivadas"), note(43L, "Integrales")))

        // Segunda lectura del servidor: 42 cambió de título y 43 ya no existe.
        dao.cacheRemoteNotes(1L, listOf(note(42L, "Derivadas (v2)")))

        val titles = dao.notesForCourse(1L).map { it.title }.toSet()
        assertEquals(setOf("Captura offline", "Derivadas (v2)"), titles)
        assertEquals(1, dao.pageHashesForCourse(1L).size)
    }

    @Test
    fun syncQueueRowsJoinTheNoteTitleAndDeletingTheNoteCascades() = runBlocking {
        val notes = db.noteDao()
        val sync = db.syncOperationDao()
        val noteId = notes.insertNote(note(serverId = null, title = "Apunte"))
        val pageId = notes.insertPage(NotePageEntity(noteId = noteId, pageIndex = 0, perceptualHash = "0000000000000000", photoPath = "/b.jpg", levelReached = "N1", overallConfidence = 0.5f, analysisJson = "{}", processedAt = 0L))
        sync.insert(SyncOperationEntity(entityType = SyncOperationEntity.TYPE_CREATE_NOTE, idempotencyKey = "k1", noteId = noteId, createdAt = 1L, updatedAt = 1L))
        sync.insert(SyncOperationEntity(entityType = SyncOperationEntity.TYPE_UPLOAD_PAGE, idempotencyKey = "k2", noteId = noteId, pageId = pageId, createdAt = 2L, updatedAt = 2L))

        val rows = sync.queueRows()
        assertEquals(listOf("Apunte", "Apunte"), rows.map { it.noteTitle })
        assertEquals(0, rows.first { it.entityType == SyncOperationEntity.TYPE_UPLOAD_PAGE }.pageIndex)
        assertEquals(listOf(SyncOperationEntity.TYPE_CREATE_NOTE, SyncOperationEntity.TYPE_UPLOAD_PAGE), sync.runnable().map { it.entityType })
        assertEquals(2, sync.pendingCount())

        notes.deleteNote(noteId)

        assertEquals(0, sync.pendingCount())
        assertEquals(0, notes.pagesForNote(noteId).size)
    }

    @Test
    fun failedOperationsCanBeRequeued() = runBlocking {
        val notes = db.noteDao()
        val sync = db.syncOperationDao()
        val noteId = notes.insertNote(note(serverId = null, title = "Apunte"))
        val opId = sync.insert(SyncOperationEntity(entityType = SyncOperationEntity.TYPE_CREATE_NOTE, idempotencyKey = "k3", noteId = noteId, createdAt = 1L, updatedAt = 1L))
        sync.update(opId, SyncOperationEntity.STATUS_FAILED, 1, "400", 5L)
        assertTrue(sync.runnable().isEmpty())

        assertEquals(1, sync.requeueFailed(9L))

        assertEquals(listOf(opId), sync.runnable().map { it.id })
    }

    /**
     * Una hoja de apuntes dibujada: título, párrafos con renglones de largo irregular y huecos
     * entre ellos, y un diagrama. [layout] cambia qué hay en la hoja; los demás parámetros
     * simulan volver a fotografiar la misma hoja.
     */
    private fun notesPage(layout: Int, dx: Float = 0f, dy: Float = 0f, brightness: Int = 245, rotation: Float = 0f): Bitmap {
        val bitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(brightness, brightness, brightness - 10))
        canvas.rotate(rotation, 600f, 800f)
        canvas.translate(dx, dy)
        val random = java.util.Random(layout.toLong())
        val ink = Paint().apply { color = Color.rgb(30, 30, 60); strokeWidth = 7f }
        val bold = Paint().apply { color = Color.rgb(20, 20, 40); strokeWidth = 22f }

        canvas.drawLine(120f, 140f, 120f + 300f + random.nextInt(500), 140f, bold)
        var y = 260f
        while (y < 1500f) {
            val lines = 2 + random.nextInt(5)
            repeat(lines) {
                canvas.drawLine(120f, y, 300f + random.nextInt(800), y, ink)
                y += 48f
            }
            if (random.nextBoolean()) {
                val left = 150f + random.nextInt(500)
                canvas.drawRect(left, y, left + 300f + random.nextInt(200), y + 180f, ink)
                canvas.drawLine(left, y + 180f, left + 300f, y, ink)
                y += 220f
            }
            y += 80f + random.nextInt(80)
        }
        return bitmap
    }

    @Test
    fun hashOnRealBitmapsRecognisesTheSamePageAndSeparatesDifferentOnes() {
        val original = PerceptualHasher.hash(notesPage(layout = 1))
        val retakes = mapOf(
            "dim" to notesPage(layout = 1, brightness = 215),
            "rotate" to notesPage(layout = 1, rotation = 1.5f)
        ).mapValues { (_, bitmap) -> PerceptualHasher.hammingDistance(original, PerceptualHasher.hash(bitmap)) }
        val shifted = mapOf(
            "shift" to notesPage(layout = 1, dx = 15f, dy = 20f),
            "all" to notesPage(layout = 1, dx = -12f, dy = 18f, brightness = 225, rotation = -1f)
        ).mapValues { (_, bitmap) -> PerceptualHasher.hammingDistance(original, PerceptualHasher.hash(bitmap)) }
        val others = (2..6).map { PerceptualHasher.hammingDistance(original, PerceptualHasher.hash(notesPage(layout = it))) }
        // Medido: retakes = {dim=0, rotate=6}, shifted = {shift=14, all=14}, others = 24..32.
        // Con umbral = 10, solo dobles disparos / reimportaciones (dim=0, rotate=6) se consideran duplicados;
        // desplazamientos (14) y paginas distintas con misma maquetacion (14-16) quedan por fuera.
        val info = "retakes=$retakes shifted=$shifted others=$others"

        retakes.values.forEach { assertTrue(info, it <= PerceptualHasher.DUPLICATE_THRESHOLD) }
        shifted.values.forEach { assertFalse(info, it <= PerceptualHasher.DUPLICATE_THRESHOLD) }
        others.forEach { assertFalse(info, it <= PerceptualHasher.DUPLICATE_THRESHOLD) }
    }
}

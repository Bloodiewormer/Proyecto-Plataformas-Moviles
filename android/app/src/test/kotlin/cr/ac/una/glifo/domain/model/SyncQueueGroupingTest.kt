package cr.ac.una.glifo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncQueueGroupingTest {

    @Test
    fun `empty list returns empty list`() {
        assertTrue(groupSyncQueueItems(emptyList()).isEmpty())
    }

    @Test
    fun `single operation note preserves properties and extracts base title`() {
        val items = listOf(
            SyncQueueItem(
                noteId = 42L,
                title = "Cálculo I · apunte",
                timestamp = "18 nov · 10:00",
                status = SyncStatus.Queued,
                attempts = 1
            )
        )

        val result = groupSyncQueueItems(items)
        assertEquals(1, result.size)
        val grouped = result.first()
        assertEquals(42L, grouped.noteId)
        assertEquals("Cálculo I", grouped.title)
        assertEquals(SyncStatus.Queued, grouped.status)
        assertEquals(1, grouped.attempts)
        assertNull(grouped.pageCount)
    }

    @Test
    fun `combines page operations and note operation into single row with page count`() {
        val items = listOf(
            SyncQueueItem(
                noteId = 10L,
                title = "Derivadas · p.1",
                timestamp = "18 nov · 10:00",
                status = SyncStatus.Done,
                progress = 1f
            ),
            SyncQueueItem(
                noteId = 10L,
                title = "Derivadas · p.2",
                timestamp = "18 nov · 10:01",
                status = SyncStatus.Queued,
                progress = 0f
            ),
            SyncQueueItem(
                noteId = 10L,
                title = "Derivadas · apunte",
                timestamp = "18 nov · 10:02",
                status = SyncStatus.Queued,
                progress = 0f
            )
        )

        val result = groupSyncQueueItems(items)
        assertEquals(1, result.size)
        val grouped = result.first()
        assertEquals(10L, grouped.noteId)
        assertEquals("Derivadas", grouped.title)
        assertEquals(2, grouped.pageCount)
        assertEquals(SyncStatus.Queued, grouped.status)
        assertEquals("18 nov · 10:02", grouped.timestamp)
    }

    @Test
    fun `failed status takes precedence over queued and done, preserving error`() {
        val items = listOf(
            SyncQueueItem(
                noteId = 10L,
                title = "Álgebra · p.1",
                timestamp = "18 nov · 09:30",
                status = SyncStatus.Done
            ),
            SyncQueueItem(
                noteId = 10L,
                title = "Álgebra · p.2",
                timestamp = "18 nov · 09:31",
                status = SyncStatus.Failed,
                lastError = "Error de red 503",
                attempts = 2
            ),
            SyncQueueItem(
                noteId = 10L,
                title = "Álgebra · apunte",
                timestamp = "18 nov · 09:32",
                status = SyncStatus.Queued,
                attempts = 1
            )
        )

        val result = groupSyncQueueItems(items)
        assertEquals(1, result.size)
        val grouped = result.first()
        assertEquals(SyncStatus.Failed, grouped.status)
        assertEquals("Error de red 503", grouped.lastError)
        assertEquals(2, grouped.attempts)
        assertEquals(2, grouped.pageCount)
    }

    @Test
    fun `syncing status takes precedence over queued and done with computed progress`() {
        val items = listOf(
            SyncQueueItem(
                noteId = 5L,
                title = "Física · p.1",
                timestamp = "18 nov · 11:00",
                status = SyncStatus.Syncing,
                progress = 0.6f
            ),
            SyncQueueItem(
                noteId = 5L,
                title = "Física · apunte",
                timestamp = "18 nov · 11:01",
                status = SyncStatus.Queued
            )
        )

        val result = groupSyncQueueItems(items)
        assertEquals(1, result.size)
        val grouped = result.first()
        assertEquals(SyncStatus.Syncing, grouped.status)
        assertEquals(0.6f, grouped.progress, 0.01f)
        assertEquals(1, grouped.pageCount)
    }

    @Test
    fun `all operations done results in done status`() {
        val items = listOf(
            SyncQueueItem(
                noteId = 7L,
                title = "Historia · p.1",
                timestamp = "18 nov · 12:00",
                status = SyncStatus.Done,
                progress = 1f
            ),
            SyncQueueItem(
                noteId = 7L,
                title = "Historia · note",
                timestamp = "18 nov · 12:01",
                status = SyncStatus.Done,
                progress = 1f
            )
        )

        val result = groupSyncQueueItems(items)
        assertEquals(1, result.size)
        val grouped = result.first()
        assertEquals(SyncStatus.Done, grouped.status)
        assertEquals(1f, grouped.progress, 0.01f)
        assertEquals(1, grouped.pageCount)
    }

    @Test
    fun `groups multiple notes independently in original encounter order`() {
        val items = listOf(
            SyncQueueItem(noteId = 1L, title = "Nota A · p.1", timestamp = "t1", status = SyncStatus.Done),
            SyncQueueItem(noteId = 2L, title = "Nota B · p.1", timestamp = "t2", status = SyncStatus.Queued),
            SyncQueueItem(noteId = 1L, title = "Nota A · apunte", timestamp = "t3", status = SyncStatus.Queued),
            SyncQueueItem(noteId = 2L, title = "Nota B · apunte", timestamp = "t4", status = SyncStatus.Done)
        )

        val result = groupSyncQueueItems(items)
        assertEquals(2, result.size)
        assertEquals(1L, result[0].noteId)
        assertEquals("Nota A", result[0].title)
        assertEquals(SyncStatus.Queued, result[0].status)
        assertEquals(1, result[0].pageCount)

        assertEquals(2L, result[1].noteId)
        assertEquals("Nota B", result[1].title)
        assertEquals(SyncStatus.Queued, result[1].status)
        assertEquals(1, result[1].pageCount)
    }
}

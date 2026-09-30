package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.datasource.local.entity.PageHashRow
import cr.ac.una.glifo.data.datasource.local.entity.ProcessedPageEntity
import cr.ac.una.glifo.data.mapper.toSnapshotJson
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PageDeduplicatorTest {

    private val dao = mockk<ProcessedPageDao>(relaxed = true)
    private val deduplicator = PageDeduplicator(dao)

    private val analysis = PageAnalysis(
        regions = listOf(
            RecognizedRegion(
                index = 0,
                bounds = RegionBounds(10, 20, 300, 40),
                text = "Límite de una función",
                lineCount = 1,
                ocrConfidence = 0.93f,
                kind = RegionKind.Text,
                kindConfidence = 0.9f,
                route = "N1 · resuelto en el dispositivo",
                confidenceState = ConfidenceState.Verified,
                metrics = RegionInkMetrics(0.12f, 21, 7.5f)
            ),
            RecognizedRegion(
                index = 1,
                bounds = RegionBounds(10, 80, 200, 60),
                text = "lim x→a",
                lineCount = 1,
                ocrConfidence = 0.41f,
                kind = RegionKind.Math,
                kindConfidence = 0.8f,
                route = "N1.5 · OCR matemático",
                confidenceState = ConfidenceState.Escalated,
                metrics = RegionInkMetrics(0.2f, 9, 3.3f)
            )
        ),
        pageWidth = 1200,
        pageHeight = 1600,
        ocrMillis = 850,
        totalMillis = 1900
    )

    private fun storedPage(id: Long, hash: String) = ProcessedPageEntity(
        id = id,
        courseId = 1L,
        perceptualHash = hash,
        photoPath = "/fotos/$id.jpg",
        analysisJson = analysis.toSnapshotJson(),
        processedAt = 1_000L
    )

    @Test
    fun `a near-identical hash reuses the stored analysis`() = runTest {
        coEvery { dao.hashesForCourse(1L) } returns listOf(PageHashRow(5L, "c3a1f0e98b7d6245"))
        coEvery { dao.getById(5L) } returns storedPage(5L, "c3a1f0e98b7d6245")

        // 2 bits distintos respecto del guardado.
        val reused = deduplicator.findSamePage(1L, "c3a1f0e98b7d6246")

        assertNotNull(reused)
        assertEquals(2, reused!!.reusedFrom?.hammingDistance)
        assertEquals(1_000L, reused.reusedFrom?.processedAt)
        assertEquals(analysis.regions, reused.regions)
    }

    @Test
    fun `the stored analysis survives the round trip including the confidence state`() = runTest {
        coEvery { dao.hashesForCourse(1L) } returns listOf(PageHashRow(5L, "0000000000000000"))
        coEvery { dao.getById(5L) } returns storedPage(5L, "0000000000000000")

        val reused = deduplicator.findSamePage(1L, "0000000000000000")!!

        assertEquals(ConfidenceState.Escalated, reused.regions[1].confidenceState)
        assertEquals(RegionKind.Math, reused.regions[1].kind)
        assertEquals(analysis.copy(reusedFrom = reused.reusedFrom), reused)
    }

    @Test
    fun `a different page is processed again`() = runTest {
        coEvery { dao.hashesForCourse(1L) } returns listOf(PageHashRow(5L, "0000000000000000"))

        val reused = deduplicator.findSamePage(1L, "ffffffff00000000")

        assertNull(reused)
        coVerify(exactly = 0) { dao.getById(any()) }
    }

    @Test
    fun `picks the closest page when several are within the threshold`() = runTest {
        coEvery { dao.hashesForCourse(1L) } returns listOf(
            PageHashRow(5L, "000000000000003f"),
            PageHashRow(6L, "0000000000000001")
        )
        coEvery { dao.getById(6L) } returns storedPage(6L, "0000000000000001")

        val reused = deduplicator.findSamePage(1L, "0000000000000000")

        assertEquals(1, reused?.reusedFrom?.hammingDistance)
        coVerify(exactly = 0) { dao.getById(5L) }
    }

    @Test
    fun `an unreadable stored analysis is treated as not found`() = runTest {
        coEvery { dao.hashesForCourse(1L) } returns listOf(PageHashRow(5L, "0000000000000000"))
        coEvery { dao.getById(5L) } returns storedPage(5L, "0000000000000000").copy(analysisJson = "roto")

        assertNull(deduplicator.findSamePage(1L, "0000000000000000"))
    }

    @Test
    fun `remember stores the hash and the serialized analysis`() = runTest {
        val saved = slot<ProcessedPageEntity>()
        coEvery { dao.insert(capture(saved)) } returns 1L

        deduplicator.remember(1L, "c3a1f0e98b7d6245", "/fotos/1.jpg", analysis)

        assertEquals("c3a1f0e98b7d6245", saved.captured.perceptualHash)
        assertEquals(1L, saved.captured.courseId)
        assertEquals(analysis.toSnapshotJson(), saved.captured.analysisJson)
    }
}

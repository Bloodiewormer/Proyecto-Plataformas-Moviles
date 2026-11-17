package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiText
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import cr.ac.una.glifo.domain.model.routeRes
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteContentMapperTest {

    @Test
    fun `buildLocalNoteContent sets localized routeText per region confidence and resolvedAt`() {
        val analysis = PageAnalysis(
            regions = listOf(
                RecognizedRegion(
                    index = 0,
                    bounds = RegionBounds(0, 0, 100, 20),
                    text = "Texto claro",
                    lineCount = 1,
                    ocrConfidence = 0.91f,
                    kind = RegionKind.Text,
                    kindConfidence = 0.9f,
                    route = "N1",
                    confidenceState = ConfidenceState.Verified,
                    metrics = RegionInkMetrics(0.1f, 8, 5f)
                ),
                RecognizedRegion(
                    index = 1,
                    bounds = RegionBounds(0, 30, 100, 20),
                    text = "dudoso",
                    lineCount = 1,
                    ocrConfidence = 0.42f,
                    kind = RegionKind.Text,
                    kindConfidence = 0.8f,
                    route = "N2",
                    confidenceState = ConfidenceState.Uncertain,
                    metrics = RegionInkMetrics(0.1f, 6, 5f)
                ),
                RecognizedRegion(
                    index = 2,
                    bounds = RegionBounds(0, 60, 80, 30),
                    text = "\\frac{1}{2}",
                    lineCount = 1,
                    ocrConfidence = 0.88f,
                    kind = RegionKind.Math,
                    kindConfidence = 0.9f,
                    route = "N1.5",
                    confidenceState = ConfidenceState.Repaired,
                    metrics = RegionInkMetrics(0.2f, 5, 2.6f),
                    resolvedAt = "N1_5"
                )
            ),
            pageWidth = 800,
            pageHeight = 1000,
            ocrMillis = 200L,
            totalMillis = 300L
        )

        val content = buildLocalNoteContent(
            note = NoteEntity(
                id = 1L,
                courseId = 10L,
                title = "Note",
                classDate = "2026-11-10",
                status = "READY",
                createdAt = 1000L
            ),
            pages = listOf(
                NotePageEntity(
                    id = 1L,
                    noteId = 1L,
                    pageIndex = 0,
                    perceptualHash = "abc",
                    photoPath = "/tmp/p.jpg",
                    levelReached = "N1_5",
                    overallConfidence = 0.8f,
                    analysisJson = analysis.toSnapshotJson(),
                    processedAt = 1000L
                )
            ),
            corrections = emptyList(),
            hasPendingSync = false
        )

        assertEquals(UiText.Resource(R.string.region_route_n1_verified), content.fragments[0].routeText)
        assertEquals(UiText.Resource(R.string.region_route_n2_below_gate), content.fragments[1].routeText)
        assertEquals(UiText.Resource(R.string.region_route_n1_5), content.fragments[2].routeText)
    }

    @Test
    fun `parseReconstruction populates localized server routeText`() {
        val json = """
            {
              "blocks": [
                {
                  "id": "b1",
                  "type": "FORMULA",
                  "latex": "\\lim_{x \\to 0} x",
                  "resolvedAt": "N1_5",
                  "confidence": 0.92
                }
              ]
            }
        """.trimIndent()

        val fragments = parseReconstruction(json)
        assertEquals(1, fragments.size)
        assertEquals(UiText.Resource(R.string.note_fragment_route_server, "N1_5"), fragments[0].routeText)
    }

    @Test
    fun `RegionKind labelRes and RecognizedRegion routeRes map all variants`() {
        assertEquals(R.string.region_kind_text, RegionKind.Text.labelRes)
        assertEquals(R.string.region_kind_math, RegionKind.Math.labelRes)
        assertEquals(R.string.region_kind_drawing, RegionKind.Drawing.labelRes)

        val resolvedN2 = RecognizedRegion(
            index = 0,
            bounds = RegionBounds(0, 0, 10, 10),
            text = "x",
            lineCount = 1,
            ocrConfidence = 0.9f,
            kind = RegionKind.Math,
            kindConfidence = 0.9f,
            route = "N2",
            confidenceState = ConfidenceState.Escalated,
            metrics = RegionInkMetrics(0.1f, 1, 1f),
            resolvedAt = "N2"
        )
        assertEquals(R.string.region_route_n2, resolvedN2.routeRes())
    }
}

package cr.ac.una.glifo.core.pipeline

import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.pipeline.CapturePipeline.CaptureStepId
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CapturePipelineTest {

    @Test
    fun `stepSpecsFor maps counts, active math and n2 escalation string resources`() {
        val analysis = PageAnalysis(
            regions = listOf(
                RecognizedRegion(
                    index = 0,
                    bounds = RegionBounds(0, 0, 100, 20),
                    text = "Definición",
                    lineCount = 1,
                    ocrConfidence = 0.92f,
                    kind = RegionKind.Text,
                    kindConfidence = 0.9f,
                    route = "N1",
                    confidenceState = ConfidenceState.Verified,
                    metrics = RegionInkMetrics(0.1f, 10, 5f)
                ),
                RecognizedRegion(
                    index = 1,
                    bounds = RegionBounds(0, 30, 80, 30),
                    text = "x^2",
                    lineCount = 1,
                    ocrConfidence = 0.6f,
                    kind = RegionKind.Math,
                    kindConfidence = 0.85f,
                    route = "N1.5",
                    confidenceState = ConfidenceState.Repaired,
                    metrics = RegionInkMetrics(0.2f, 5, 2.5f)
                ),
                RecognizedRegion(
                    index = 2,
                    bounds = RegionBounds(0, 70, 90, 60),
                    text = "",
                    lineCount = 0,
                    ocrConfidence = 0.2f,
                    kind = RegionKind.Drawing,
                    kindConfidence = 0.8f,
                    route = "N2",
                    confidenceState = ConfidenceState.Escalated,
                    metrics = RegionInkMetrics(0.3f, 3, 1.5f)
                )
            ),
            pageWidth = 800,
            pageHeight = 1000,
            ocrMillis = 400L,
            totalMillis = 650L
        )

        val specs = CapturePipeline.stepSpecsFor(analysis)
        assertEquals(5, specs.size)

        assertEquals(CaptureStepId.N0, specs[0].id)
        assertEquals(R.string.capture_step_n0_name, specs[0].nameRes)
        assertEquals(listOf(3), specs[0].descriptionArgs)

        assertEquals(CaptureStepId.N1, specs[1].id)
        assertEquals(listOf(2, 3, "0.4"), specs[1].descriptionArgs)

        assertEquals(CaptureStepId.Classifier, specs[2].id)
        assertEquals(listOf(1, 1, 1), specs[2].descriptionArgs)

        assertEquals(CaptureStepId.N1_5, specs[3].id)
        assertEquals("now", specs[3].state)
        assertEquals(R.string.capture_step_n15_desc_active, specs[3].descriptionRes)
        assertEquals(listOf(1), specs[3].descriptionArgs)

        assertEquals(CaptureStepId.N2, specs[4].id)
        assertEquals("now", specs[4].state)
        assertEquals(R.string.capture_step_n2_desc_active, specs[4].descriptionRes)
        assertEquals(listOf(1), specs[4].descriptionArgs)
    }

    @Test
    fun `stepSpecsFor marks n1_5 and n2 pending with idle strings when all text is confident`() {
        val analysis = PageAnalysis(
            regions = listOf(
                RecognizedRegion(
                    index = 0,
                    bounds = RegionBounds(0, 0, 100, 20),
                    text = "Texto claro",
                    lineCount = 1,
                    ocrConfidence = 0.95f,
                    kind = RegionKind.Text,
                    kindConfidence = 0.9f,
                    route = "N1",
                    confidenceState = ConfidenceState.Verified,
                    metrics = RegionInkMetrics(0.1f, 8, 5f)
                )
            ),
            pageWidth = 800,
            pageHeight = 1000,
            ocrMillis = 100L,
            totalMillis = 150L
        )

        val specs = CapturePipeline.stepSpecsFor(analysis)
        assertEquals("pending", specs[3].state)
        assertEquals(R.string.capture_step_n15_desc_empty, specs[3].descriptionRes)
        assertEquals("pending", specs[4].state)
        assertEquals(R.string.capture_step_n2_desc_idle, specs[4].descriptionRes)
    }
}

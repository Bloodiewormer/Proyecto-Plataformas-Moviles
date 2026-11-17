package cr.ac.una.glifo.core.pipeline

import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.core.mlkit.RecognizedBlock
import cr.ac.una.glifo.core.mlkit.RecognizedLine
import cr.ac.una.glifo.core.pipeline.CapturePipeline.CandidateRegion
import cr.ac.una.glifo.core.pipeline.CapturePipeline.CaptureStepId
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `splitMixedBlocks separates prose lines from adjacent formula lines`() {
        val proseLine = RecognizedLine(
            text = "El eje de las abscisas es la asintota de la curva",
            bounds = RegionBounds(20, 100, 500, 32),
            wordCount = 11,
            confidence = 0.62f
        )
        val mathLine = RecognizedLine(
            text = "x = a tan t, t = 0",
            bounds = RegionBounds(20, 140, 220, 32),
            wordCount = 7,
            confidence = 0.45f
        )
        val combined = RecognizedBlock(
            bounds = RegionBounds(20, 100, 500, 72),
            ocr = OcrResult("${proseLine.text}\n${mathLine.text}", 2, 18, 0.53f),
            lines = listOf(proseLine, mathLine)
        )

        val split = CapturePipeline.splitMixedBlocks(listOf(combined), pageWidth = 800)

        assertEquals(2, split.size)
        assertEquals(proseLine.text, split[0].ocr.text)
        assertEquals(mathLine.text, split[1].ocr.text)
    }

    @Test
    fun `mergeFragmentedMath coalesces horizontally split math row without swallowing 2D drawings`() {
        val leftFrag = CandidateRegion(
            bounds = RegionBounds(40, 540, 140, 55),
            ocr = OcrResult("V = 2", 1, 3, 0.41f),
            metrics = RegionInkMetrics(0.08f, 10, 2.5f, horizontalBarCount = 1, tallStrokeCount = 1)
        )
        val rightFrag = CandidateRegion(
            bounds = RegionBounds(210, 542, 180, 52),
            ocr = OcrResult("a^2 sec t dt", 1, 4, 0.39f),
            metrics = RegionInkMetrics(0.08f, 14, 3.4f, horizontalBarCount = 1)
        )
        val curveDrawing = CandidateRegion(
            bounds = RegionBounds(460, 500, 210, 130),
            ocr = OcrResult.Empty,
            metrics = RegionInkMetrics(0.18f, 23, 1.6f, maxStrokeMinSpanRatio = 0.15f)
        )

        val merged = CapturePipeline.mergeFragmentedMath(listOf(leftFrag, rightFrag, curveDrawing), pageWidth = 800)

        assertEquals(2, merged.size)
        assertEquals(curveDrawing.bounds, merged[0].bounds)
        assertEquals("V = 2 a^2 sec t dt", merged[1].ocr.text)
    }

    @Test
    fun `regionIoU and lineCoverageFraction distinguish real overlap from loose outer block containment`() {
        val diagram = RegionBounds(300, 150, 200, 110)
        val looseOuterBlock = RegionBounds(20, 100, 500, 180)
        val proseLineOnlyOnLeft = RecognizedLine("El eje de las abscisas", RegionBounds(20, 100, 240, 30), 5, 0.8f)

        val iou = CapturePipeline.regionIoU(diagram, looseOuterBlock)
        val coverage = CapturePipeline.lineCoverageFraction(diagram, listOf(proseLineOnlyOnLeft))

        assertTrue(iou < 0.30f)
        assertEquals(0f, coverage, 0.001f)
    }

    @Test
    fun `splitMixedBlocks uses formula geometry callback to separate garbled inline math from prose`() {
        val proseLine = RecognizedLine(
            text = "Hallar el area de la region comprendida entre la curva de",
            bounds = RegionBounds(20, 20, 500, 30),
            wordCount = 11,
            confidence = 0.8f
        )
        val garbledFormulaLine = RecognizedLine(
            text = "Rgnesi ya",
            bounds = RegionBounds(20, 52, 160, 42),
            wordCount = 2,
            confidence = 0.4f
        )
        val block = RecognizedBlock(
            bounds = RegionBounds(20, 20, 500, 74),
            ocr = OcrResult("${proseLine.text}\n${garbledFormulaLine.text}", 2, 13, 0.6f),
            lines = listOf(proseLine, garbledFormulaLine)
        )

        val split = CapturePipeline.splitMixedBlocks(listOf(block), pageWidth = 800) { line ->
            line.text == garbledFormulaLine.text
        }

        assertEquals(2, split.size)
        assertEquals(proseLine.text, split[0].ocr.text)
        assertEquals(garbledFormulaLine.text, split[1].ocr.text)
    }

    @Test
    fun `partitionProseAndOther isolates fluent prose from short math snippets`() {
        val proseBlock = RecognizedBlock(
            bounds = RegionBounds(20, 100, 500, 35),
            ocr = OcrResult("El eje de las abscisas es la asintota de la curva", 1, 11, 0.72f),
            lines = emptyList()
        )
        val mathSnippet = RecognizedBlock(
            bounds = RegionBounds(20, 145, 160, 30),
            ocr = OcrResult("pues Lim ao", 1, 3, 0.45f),
            lines = emptyList()
        )

        val (prose, other) = CapturePipeline.partitionProseAndOther(listOf(proseBlock, mathSnippet))

        assertEquals(listOf(proseBlock), prose)
        assertEquals(listOf(mathSnippet), other)
    }
}

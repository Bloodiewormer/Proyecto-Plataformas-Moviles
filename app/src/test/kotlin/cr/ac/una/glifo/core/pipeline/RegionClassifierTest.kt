package cr.ac.una.glifo.core.pipeline

import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionClassifierTest {

    private val defaultBounds = RegionBounds(0, 0, 240, 60)

    @Test
    fun `classifies handwritten formula snippet with fraction bar and sparse OCR as Math`() {
        val region = RegionClassifier.classify(
            index = 0,
            bounds = defaultBounds,
            metrics = RegionInkMetrics(
                inkDensity = 0.08f,
                componentCount = 24,
                aspectRatio = 4.2f,
                horizontalBarCount = 2,
                tallStrokeCount = 1,
                maxStrokeMinSpanRatio = 0.04f,
                heightRatio = 0.06f
            ),
            ocr = OcrResult(text = "A-", lineCount = 1, wordCount = 1, confidence = 0.31f)
        )

        assertEquals(RegionKind.Math, region.kind)
        assertEquals(ConfidenceState.Repaired, region.confidenceState)
    }

    @Test
    fun `classifies unread formula region with fraction bars as Math rather than Drawing`() {
        val region = RegionClassifier.classify(
            index = 0,
            bounds = defaultBounds,
            metrics = RegionInkMetrics(
                inkDensity = 0.09f,
                componentCount = 18,
                aspectRatio = 3.5f,
                horizontalBarCount = 1,
                tallStrokeCount = 1,
                maxStrokeMinSpanRatio = 0.03f,
                heightRatio = 0.05f
            ),
            ocr = OcrResult.Empty
        )

        assertEquals(RegionKind.Math, region.kind)
    }

    @Test
    fun `classifies calculus expression with integral, trig keyword, and digits as Math`() {
        val region = RegionClassifier.classify(
            index = 0,
            bounds = defaultBounds,
            metrics = RegionInkMetrics(
                inkDensity = 0.07f,
                componentCount = 16,
                aspectRatio = 4.0f,
                horizontalBarCount = 1,
                tallStrokeCount = 0,
                maxStrokeMinSpanRatio = 0.03f
            ),
            ocr = OcrResult(text = "Jo Sec", lineCount = 1, wordCount = 2, confidence = 0.40f)
        )

        assertEquals(RegionKind.Math, region.kind)
    }

    @Test
    fun `classifies large 2D curve figure as Drawing even if short axis labels are read`() {
        val unreadCurve = RegionClassifier.classify(
            index = 0,
            bounds = RegionBounds(200, 140, 213, 104),
            metrics = RegionInkMetrics(
                inkDensity = 0.18f,
                componentCount = 23,
                aspectRatio = 2.05f,
                horizontalBarCount = 0,
                tallStrokeCount = 0,
                maxStrokeMinSpanRatio = 0.155f,
                heightRatio = 0.155f
            ),
            ocr = OcrResult.Empty
        )
        assertEquals(RegionKind.Drawing, unreadCurve.kind)
        assertEquals(ConfidenceState.Escalated, unreadCurve.confidenceState)

        val labeledCurve = RegionClassifier.classify(
            index = 1,
            bounds = RegionBounds(200, 140, 213, 104),
            metrics = RegionInkMetrics(
                inkDensity = 0.18f,
                componentCount = 23,
                aspectRatio = 2.05f,
                horizontalBarCount = 0,
                tallStrokeCount = 0,
                maxStrokeMinSpanRatio = 0.155f,
                heightRatio = 0.155f
            ),
            ocr = OcrResult(text = "y x 0", lineCount = 1, wordCount = 3, confidence = 0.55f)
        )
        assertEquals(RegionKind.Drawing, labeledCurve.kind)
    }

    @Test
    fun `classifies Spanish prose paragraph as Text and applies glossary boost with ML Kit confidence`() {
        val ocr = OcrResult(
            text = "El eje de las abscisas es la asintota de la curva",
            lineCount = 1,
            wordCount = 11,
            confidence = 0.66f
        )
        val metrics = RegionInkMetrics(
            inkDensity = 0.07f,
            componentCount = 38,
            aspectRatio = 6.5f
        )

        val withoutGlossary = RegionClassifier.classify(0, defaultBounds, metrics, ocr)
        val withGlossary = RegionClassifier.classify(0, defaultBounds, metrics, ocr, glossary = listOf("asíntota"))

        assertEquals(RegionKind.Text, withGlossary.kind)
        assertEquals(ConfidenceState.Uncertain, withoutGlossary.confidenceState)
        assertEquals(ConfidenceState.Verified, withGlossary.confidenceState)
        assertTrue(withGlossary.ocrConfidence > withoutGlossary.ocrConfidence)
    }

    @Test
    fun `keeps fluent multi-word prose as Text even when cursive strokes trigger bar or tall counts`() {
        val prose = RegionClassifier.classify(
            index = 0,
            bounds = defaultBounds,
            metrics = RegionInkMetrics(
                inkDensity = 0.08f,
                componentCount = 42,
                aspectRatio = 7.0f,
                horizontalBarCount = 2,
                tallStrokeCount = 1,
                maxStrokeMinSpanRatio = 0.02f
            ),
            ocr = OcrResult(
                text = "para realizar la integracion aplicamos el cambio de variable",
                lineCount = 1,
                wordCount = 9,
                confidence = 0.53f
            )
        )

        assertEquals(RegionKind.Text, prose.kind)
    }

    @Test
    fun `detects short limit keyword snippet as math text`() {
        assertTrue(RegionClassifier.looksLikeMathText("pues Lim ao", wordCount = 3))
    }

    @Test
    fun `keeps prose headings ending in colon as Text despite tall ascender strokes`() {
        val heading = RegionClassifier.classify(
            index = 0,
            bounds = defaultBounds,
            metrics = RegionInkMetrics(
                inkDensity = 0.08f,
                componentCount = 8,
                aspectRatio = 2.5f,
                horizontalBarCount = 0,
                tallStrokeCount = 1,
                maxStrokeMinSpanRatio = 0.02f
            ),
            ocr = OcrResult("Selaiek:", lineCount = 1, wordCount = 1, confidence = 0.44f)
        )

        assertEquals(RegionKind.Text, heading.kind)
    }

    @Test
    fun `classifies limit and arctan bracket expression with compact fraction bars as Math`() {
        val expr = RegionClassifier.classify(
            index = 0,
            bounds = defaultBounds,
            metrics = RegionInkMetrics(
                inkDensity = 0.09f,
                componentCount = 26,
                aspectRatio = 4.5f,
                horizontalBarCount = 2,
                tallStrokeCount = 0,
                maxStrokeMinSpanRatio = 0.03f
            ),
            ocr = OcrResult("Loo\nbo", lineCount = 2, wordCount = 2, confidence = 0.47f)
        )

        assertEquals(RegionKind.Math, expr.kind)
    }
}

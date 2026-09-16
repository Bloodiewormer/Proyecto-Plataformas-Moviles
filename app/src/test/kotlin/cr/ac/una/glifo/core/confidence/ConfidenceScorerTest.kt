package cr.ac.una.glifo.core.confidence

import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceScorerTest {

    private fun cleanFragment(text: String) = OcrResult(
        text = text,
        lineCount = 1,
        wordCount = text.trim().split(Regex("\\s+")).count { it.isNotBlank() },
        confidence = null
    )

    private fun metrics(componentCount: Int) =
        RegionInkMetrics(inkDensity = 0.05f, componentCount = componentCount, aspectRatio = 4f)

    @Test
    fun `should score above threshold when text is clean and ink agrees with recognized characters`() {
        val text = "la derivada de una funcion constante es cero"
        val ocr = cleanFragment(text)

        val result = ConfidenceScorer.score(ocr, metrics(componentCount = text.count { !it.isWhitespace() }))

        assertTrue(result.isAboveThreshold)
        assertTrue(result.value >= ConfidenceScorer.DEFAULT_THRESHOLD)
    }

    @Test
    fun `should lower ink agreement when far fewer characters were recognized than components`() {
        val ocr = cleanFragment("limite")

        // 6 recognized characters against 100 connected components: mostly unread ink.
        val result = ConfidenceScorer.score(ocr, metrics(componentCount = 100))

        assertEquals(0.2f, result.breakdown.getValue("inkAgreement"), 0.001f)
        assertFalse(result.isAboveThreshold)
    }

    @Test
    fun `should score zero regardless of ink when no text was recognized at all`() {
        val result = ConfidenceScorer.score(OcrResult.Empty, metrics(componentCount = 500))

        assertEquals(0f, result.breakdown.getValue("inkAgreement"), 0.0001f)
        assertEquals(0f, result.value, 0.0001f)
    }

    @Test
    fun `should lower legibility signal when text is mostly OCR noise`() {
        val ocr = cleanFragment("¤§~^`|\\{}[]<>@#$%&_")

        val result = ConfidenceScorer.score(ocr, metrics(componentCount = 20))

        assertTrue(result.breakdown.getValue("legibility") < 0.2f)
    }

    @Test
    fun `should lower fragmentation signal when element count does not match token count`() {
        // A 3-word line ML Kit split into 12 disconnected elements — typical of misread math notation.
        val ocr = cleanFragment("x mas y").copy(wordCount = 12)

        val result = ConfidenceScorer.score(ocr, metrics(componentCount = 12))

        assertEquals(3f / 12f, result.breakdown.getValue("fragmentation"), 0.001f)
    }

    @Test
    fun `should escalate uncertain text unless a glossary term is confirmed present`() {
        val ocr = cleanFragment("teorema")

        val withoutGlossary = ConfidenceScorer.score(ocr, metrics(componentCount = 100))
        val withGlossary = ConfidenceScorer.score(ocr, metrics(componentCount = 100), glossary = listOf("teorema", "limite"))

        assertFalse(withoutGlossary.isAboveThreshold)
        assertTrue(withGlossary.isAboveThreshold)
        assertTrue(withGlossary.value > withoutGlossary.value)
    }

    @Test
    fun `should not reward an absent glossary term`() {
        val ocr = cleanFragment("un enunciado cualquiera sin vocabulario del curso")

        val result = ConfidenceScorer.score(ocr, metrics(componentCount = 30), glossary = listOf("jacobiano", "autovalor"))

        assertEquals(0f, result.breakdown.getValue("glossaryBoost"), 0.0001f)
    }

    @Test
    fun `should score zero for an empty fragment with no ink either`() {
        val result = ConfidenceScorer.score(OcrResult.Empty, metrics(componentCount = 0))

        assertEquals(0f, result.value, 0.0001f)
        assertFalse(result.isAboveThreshold)
    }

    @Test
    fun `should reject weights that do not sum to one`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScoringWeights(inkAgreement = 0.5f, legibility = 0.5f, fragmentation = 0.5f)
        }
    }

    @Test
    fun `should respect a custom threshold`() {
        val text = "area bajo la curva"
        val ocr = cleanFragment(text)
        val regionMetrics = metrics(componentCount = text.count { !it.isWhitespace() } * 3)

        val strict = ConfidenceScorer.score(ocr, regionMetrics, threshold = 0.99f)
        val lenient = ConfidenceScorer.score(ocr, regionMetrics, threshold = 0.1f)

        assertFalse(strict.isAboveThreshold)
        assertTrue(lenient.isAboveThreshold)
    }
}

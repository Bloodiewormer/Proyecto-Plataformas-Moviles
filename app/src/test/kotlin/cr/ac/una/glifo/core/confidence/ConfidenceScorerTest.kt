package cr.ac.una.glifo.core.confidence

import cr.ac.una.glifo.domain.model.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceScorerTest {

    private fun cleanFragment(text: String) = OcrResult(
        recognizedText = text,
        elementCount = text.trim().split(Regex("\\s+")).count { it.isNotBlank() },
        lineCount = 1,
        recognizedArea = 90,
        regionArea = 100
    )

    @Test
    fun `should score above threshold when text is clean and fully covered`() {
        val ocr = cleanFragment("la derivada de una funcion constante es cero")

        val result = ConfidenceScorer.score(ocr)

        assertTrue(result.isAboveThreshold)
        assertTrue(result.value >= ConfidenceScorer.DEFAULT_THRESHOLD)
    }

    @Test
    fun `should lower coverage signal when recognized area is much smaller than region area`() {
        val ocr = cleanFragment("limite").copy(recognizedArea = 5, regionArea = 100)

        val result = ConfidenceScorer.score(ocr)

        assertEquals(0.05f, result.breakdown.getValue("coverage"), 0.001f)
        assertFalse(result.isAboveThreshold)
    }

    @Test
    fun `should lower legibility signal when text is mostly OCR noise`() {
        val ocr = cleanFragment("¤§~^`|\\{}[]<>@#$%&_")

        val result = ConfidenceScorer.score(ocr)

        assertTrue(result.breakdown.getValue("legibility") < 0.2f)
    }

    @Test
    fun `should lower fragmentation signal when element count does not match token count`() {
        // A 3-word line ML Kit split into 12 disconnected elements — typical of misread math notation.
        val ocr = cleanFragment("x mas y").copy(elementCount = 12)

        val result = ConfidenceScorer.score(ocr)

        assertEquals(3f / 12f, result.breakdown.getValue("fragmentation"), 0.001f)
    }

    @Test
    fun `should escalate uncertain text unless a glossary term is confirmed present`() {
        val ocr = cleanFragment("teorema").copy(recognizedArea = 30, regionArea = 100)

        val withoutGlossary = ConfidenceScorer.score(ocr)
        val withGlossary = ConfidenceScorer.score(ocr, glossary = listOf("teorema", "limite"))

        assertFalse(withoutGlossary.isAboveThreshold)
        assertTrue(withGlossary.isAboveThreshold)
        assertTrue(withGlossary.value > withoutGlossary.value)
    }

    @Test
    fun `should not reward an absent glossary term`() {
        val ocr = cleanFragment("un enunciado cualquiera sin vocabulario del curso")

        val result = ConfidenceScorer.score(ocr, glossary = listOf("jacobiano", "autovalor"))

        assertEquals(0f, result.breakdown.getValue("glossaryBoost"), 0.0001f)
    }

    @Test
    fun `should score zero for an empty fragment`() {
        val ocr = OcrResult(recognizedText = "", elementCount = 0, lineCount = 0, recognizedArea = 0, regionArea = 100)

        val result = ConfidenceScorer.score(ocr)

        assertEquals(0f, result.value, 0.0001f)
        assertFalse(result.isAboveThreshold)
    }

    @Test
    fun `should reject weights that do not sum to one`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScoringWeights(coverage = 0.5f, legibility = 0.5f, fragmentation = 0.5f)
        }
    }

    @Test
    fun `should respect a custom threshold`() {
        val ocr = cleanFragment("area bajo la curva")

        val strict = ConfidenceScorer.score(ocr, threshold = 0.99f)
        val lenient = ConfidenceScorer.score(ocr, threshold = 0.1f)

        assertFalse(strict.isAboveThreshold)
        assertTrue(lenient.isAboveThreshold)
    }
}

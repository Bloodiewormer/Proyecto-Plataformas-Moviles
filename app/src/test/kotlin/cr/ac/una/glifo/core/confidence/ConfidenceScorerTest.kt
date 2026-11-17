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

    // ---------- Reglas agregadas en el issue #36 ----------

    @Test
    fun `should treat a region without segmentation as neutral ink evidence`() {
        // Página completa sin segmentar: componentCount = 0 no dice nada, ni a favor ni en contra.
        val result = ConfidenceScorer.score(cleanFragment("integral"), metrics(componentCount = 0))

        assertEquals(0.7f, result.breakdown.getValue("inkAgreement"), 0.0001f)
    }

    @Test
    fun `should step ink agreement exactly at each characters-per-component boundary`() {
        val ocr = cleanFragment("abcdefghij") // 10 caracteres

        fun ink(components: Int) =
            ConfidenceScorer.score(ocr, metrics(componentCount = components)).breakdown.getValue("inkAgreement")

        assertEquals(1f, ink(20), 0.0001f)    // 0.5 por componente
        assertEquals(0.75f, ink(40), 0.0001f) // 0.25
        assertEquals(0.5f, ink(100), 0.0001f) // 0.1
        assertEquals(0.2f, ink(101), 0.0001f) // por debajo de 0.1
    }

    @Test
    fun `should never exceed 1 even when a perfect fragment also matches the glossary`() {
        val text = "serie geometrica"
        val result = ConfidenceScorer.score(
            cleanFragment(text),
            metrics(componentCount = text.count { !it.isWhitespace() }),
            glossary = listOf("geometrica")
        )

        assertEquals(0.10f, result.breakdown.getValue("glossaryBoost"), 0.0001f)
        assertEquals(1f, result.value, 0.0001f)
    }

    @Test
    fun `should not count blank glossary terms as a match`() {
        // "".contains vale siempre true: un término vacío del glosario no debe premiar todo.
        val result = ConfidenceScorer.score(cleanFragment("limite"), metrics(componentCount = 6), glossary = listOf("", "   "))

        assertEquals(0f, result.breakdown.getValue("glossaryBoost"), 0.0001f)
    }

    @Test
    fun `should match glossary terms ignoring case`() {
        val result = ConfidenceScorer.score(cleanFragment("LIMITE lateral"), metrics(componentCount = 13), glossary = listOf("limite"))

        assertEquals(0.10f, result.breakdown.getValue("glossaryBoost"), 0.0001f)
    }

    @Test
    fun `should be deterministic for the same input`() {
        val ocr = cleanFragment("x al cuadrado mas 3")
        val regionMetrics = metrics(componentCount = 30)

        assertEquals(ConfidenceScorer.score(ocr, regionMetrics), ConfidenceScorer.score(ocr, regionMetrics))
    }

    @Test
    fun `should apply glossary boost even when ML Kit reports its own confidence`() {
        val ocr = cleanFragment("el eje es la asintota").copy(confidence = 0.65f)
        val regionMetrics = metrics(componentCount = 18)

        val withoutGlossary = ConfidenceScorer.score(ocr, regionMetrics)
        val withGlossary = ConfidenceScorer.score(ocr, regionMetrics, glossary = listOf("asíntota"))

        assertFalse(withoutGlossary.isAboveThreshold)
        assertTrue(withGlossary.isAboveThreshold)
        assertEquals(0.10f, withGlossary.breakdown.getValue("glossaryBoost"), 0.0001f)
        assertTrue(withGlossary.value > withoutGlossary.value)
    }

    @Test
    fun `should cap overconfident ML Kit read when most ink went unread`() {
        val ocr = cleanFragment("ab").copy(confidence = 0.92f)
        val denseUnreadInk = metrics(componentCount = 40)

        val result = ConfidenceScorer.score(ocr, denseUnreadInk)

        assertEquals(0.2f, result.breakdown.getValue("inkAgreement"), 0.001f)
        assertFalse(result.isAboveThreshold)
    }
}

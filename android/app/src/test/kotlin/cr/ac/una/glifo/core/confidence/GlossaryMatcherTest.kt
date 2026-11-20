package cr.ac.una.glifo.core.confidence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryMatcherTest {

    private val glossary = listOf("Sumatoria", "Converge", "Radio de convergencia", "Integral impropia", "Límite")

    @Test
    fun `a misread word suggests the glossary term it resembles`() {
        assertEquals("Converge", GlossaryMatcher.suggestions("la serie comverge", glossary).first())
    }

    @Test
    fun `accents and case do not matter`() {
        assertTrue("Límite" in GlossaryMatcher.suggestions("el LIMTE de f", glossary))
    }

    @Test
    fun `two-word terms are matched against consecutive words`() {
        assertEquals("Integral impropia", GlossaryMatcher.suggestions("una integal impropa", glossary).first())
    }

    @Test
    fun `a term already written correctly is not suggested`() {
        assertTrue("Sumatoria" !in GlossaryMatcher.suggestions("la sumatoria converge", glossary))
    }

    @Test
    fun `unrelated text gives no suggestions`() {
        assertEquals(emptyList<String>(), GlossaryMatcher.suggestions("hola mundo", glossary))
    }

    @Test
    fun `similarity is 1 for equal strings and decreases with edits`() {
        assertEquals(1f, GlossaryMatcher.similarity("limite", "limite"))
        assertTrue(GlossaryMatcher.similarity("limte", "limite") > GlossaryMatcher.similarity("lmt", "limite"))
    }
}

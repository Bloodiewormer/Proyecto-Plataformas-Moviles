package cr.ac.una.glifo.coverage

import cr.ac.una.glifo.coverage.service.CoveragePrefilter
import cr.ac.una.glifo.coverage.service.PrefilterNote
import cr.ac.una.glifo.coverage.service.PrefilterTopic
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas UNITARIAS del pre-filtro determinista de cobertura (D-08): sin IA ni base de datos. */
class CoveragePrefilterTest {

    private val prefilter = CoveragePrefilter()
    private val limits = PrefilterTopic(101L, "U1.1", "Límites de funciones", setOf(100L))
    private val integrals = PrefilterTopic(102L, "U2.1", "Integrales impropias")
    private val series = PrefilterTopic(103L, "U3.1", "Series de potencias")

    @Test
    fun `las palabras clave ignoran tildes, palabras vacias, plurales y vocal final`() {
        assertEquals(setOf("limit", "funcio"), CoveragePrefilter.stems("Introducción a los Límites de una función"))
        assertEquals(CoveragePrefilter.stems("límite"), CoveragePrefilter.stems("Límites"))
        assertEquals(CoveragePrefilter.stems("serie"), CoveragePrefilter.stems("series"))
        assertEquals(CoveragePrefilter.stems("Un árbol conexo es un grafo"), CoveragePrefilter.stems("Árboles conexos y grafos"))
        assertEquals(CoveragePrefilter.stems("matriz"), CoveragePrefilter.stems("matrices"))
        assertEquals(CoveragePrefilter.stems("base"), CoveragePrefilter.stems("bases"))
        assertEquals(CoveragePrefilter.stems("índice"), CoveragePrefilter.stems("índices"))
    }

    @Test
    fun `las siglas y palabras cortas significativas cuentan tambien en titulos mixtos`() {
        assertEquals(setOf("sql"), CoveragePrefilter.keywords("SQL"))
        assertEquals(setOf("ley", "ohm"), CoveragePrefilter.keywords("Ley de Ohm"))
        assertEquals(setOf("consul", "sql"), CoveragePrefilter.keywords("Consultas SQL"))
        assertTrue(CoveragePrefilter.keywords("Si no").isEmpty())
    }

    @Test
    fun `sin apuntes con contenido todo queda ABSENT sin pasar por la IA`() {
        val decisions = prefilter.decide(listOf(limits, PrefilterTopic(104L, "U4", "SQL")), emptyList())

        assertTrue(decisions.all { it.state == "ABSENT" && it.score == 0f })
    }

    @Test
    fun `tema con todas sus palabras clave y un apunte asignado queda SOLID`() {
        val notes = listOf(PrefilterNote(101L, "El límite de una función cuando x tiende a a.\nOtras funciones."))

        val decision = prefilter.decide(listOf(limits), notes).single()

        assertEquals("SOLID", decision.state)
        assertEquals(1f, decision.score)
    }

    @Test
    fun `un apunte asignado solo a la unidad no basta para SOLID ni impide ABSENT`() {
        val withHits = prefilter.decide(listOf(limits), listOf(PrefilterNote(100L, "Límites de funciones racionales"))).single()
        val withoutHits = prefilter.decide(listOf(limits), listOf(PrefilterNote(100L, "Continuidad en un intervalo"))).single()

        assertNull(withHits.state)
        assertEquals("ABSENT", withoutHits.state)
    }

    @Test
    fun `un apunte asignado irrelevante no da SOLID aunque otras notas mencionen el tema`() {
        val notes = listOf(
            PrefilterNote(101L, "Pendiente pasar ejercicios de pizarra"),
            PrefilterNote(null, "Repaso de límites"),
            PrefilterNote(null, "Composición de funciones")
        )

        val decision = prefilter.decide(listOf(limits), notes).single()

        assertNull(decision.state)
        assertEquals(1f, decision.score)
    }

    @Test
    fun `un titulo con una sola palabra clave nunca queda SOLID sin IA`() {
        val decision = prefilter.decide(listOf(PrefilterTopic(104L, "U4", "Derivadas")), listOf(PrefilterNote(104L, "Derivadas parciales"))).single()

        assertNull(decision.state)
    }

    @Test
    fun `los fragmentos priorizan las oraciones con palabras clave sobre las de la unidad`() {
        val notes = listOf(
            PrefilterNote(100L, (1..6).joinToString("\n") { "Oración $it de la unidad" }),
            PrefilterNote(null, "Las integrales impropias convergen")
        )

        val excerpts = prefilter.decide(listOf(integrals.copy(ancestorIds = setOf(100L))), notes).single().excerpts

        assertEquals("Las integrales impropias convergen", excerpts.first())
    }

    @Test
    fun `tema sin ninguna palabra clave ni apunte asignado queda ABSENT`() {
        val decision = prefilter.decide(listOf(series), listOf(PrefilterNote(101L, "Límites laterales"))).single()

        assertEquals("ABSENT", decision.state)
        assertEquals(0f, decision.score)
    }

    @Test
    fun `una sigla presente en los apuntes no queda ABSENT`() {
        val decision = prefilter.decide(listOf(PrefilterTopic(104L, "U4", "SQL")), listOf(PrefilterNote(null, "Consultas SQL con JOIN"))).single()

        assertNull(decision.state)
        assertEquals(listOf("Consultas SQL con JOIN"), decision.excerpts)
    }

    @Test
    fun `coincidencias parciales son ambiguas y los fragmentos no se cortan en decimales`() {
        val notes = listOf(
            PrefilterNote(null, "1.1 Integrales con eps = 0.05 en el intervalo. Regla de Barrow"),
            PrefilterNote(101L, "Límites de funciones racionales")
        )

        val decisions = prefilter.decide(listOf(limits, integrals), notes).associateBy { it.topic.topicId }

        assertEquals("SOLID", decisions.getValue(101L).state)
        val ambiguous = decisions.getValue(102L)
        assertNull(ambiguous.state)
        assertEquals(0.5f, ambiguous.score)
        assertEquals(listOf("1.1 Integrales con eps = 0.05 en el intervalo"), ambiguous.excerpts)
    }

    @Test
    fun `todas las palabras clave sin apunte asignado siguen siendo ambiguas`() {
        val decision = prefilter.decide(listOf(integrals), listOf(PrefilterNote(null, "integrales impropias"))).single()

        assertNull(decision.state)
        assertEquals(1f, decision.score)
    }

    @Test
    fun `los fragmentos se limitan en cantidad y largo`() {
        val text = (1..10).joinToString("\n") { "Integrales número $it " + "x".repeat(400) }

        val excerpts = prefilter.decide(listOf(integrals), listOf(PrefilterNote(null, text))).single().excerpts

        assertEquals(CoveragePrefilter.MAX_EXCERPTS, excerpts.size)
        assertTrue(excerpts.all { it.length <= CoveragePrefilter.MAX_EXCERPT_CHARS })
    }
}

package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.validation.EscalationReason
import cr.ac.una.glifo.ai.validation.FormulaEscalationPolicy
import cr.ac.una.glifo.ai.validation.FormulaStatus
import cr.ac.una.glifo.ai.validation.LatexValidator
import cr.ac.una.glifo.ai.validation.MathRegion
import cr.ac.una.glifo.ai.validation.MathTranscription
import cr.ac.una.glifo.ai.validation.mathRegions
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas UNITARIAS de la compuerta N1.5 → N2. Usan el LatexValidator real (no tiene
 * colaboradores externos) y páginas armadas en memoria con el mismo formato de regiones que
 * sube la app (PageUploadMapper): sin base de datos, SimpleTex ni OpenAI.
 */
class FormulaEscalationPolicyTest {

    private val policy = FormulaEscalationPolicy(LatexValidator())
    private val note = Note().apply { id = 7L }

    /** Regiones con el formato de PageUploadMapper: el `text` de MATH es OCR local, no LaTeX. */
    private fun page(id: Long, pageIndex: Int, regionsJson: String) = NotePage().apply {
        this.id = id
        this.pageIndex = pageIndex
        this.note = this@FormulaEscalationPolicyTest.note
        this.regions = regionsJson
    }

    private val page0 = page(
        id = 100L, pageIndex = 0, regionsJson = """
        [
          {"id": "r0", "bbox": [0.1, 0.1, 0.9, 0.2], "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.93, "isUncertain": false, "route": "N1 - OCR local", "text": "Límite de una función"},
          {"id": "r1", "bbox": [0.2, 0.3, 0.8, 0.4], "kind": "MATH", "resolvedAt": "UNRESOLVED", "confidence": 0.41, "isUncertain": true, "route": "N1.5 - OCR matematico", "text": "lim x→a f(x) = L"},
          {"id": "r2", "bbox": [0.2, 0.5, 0.8, 0.6], "kind": "MATH", "resolvedAt": "UNRESOLVED", "confidence": 0.30, "isUncertain": true, "route": "N1.5 - OCR matematico", "text": "∫ x dx"}
        ]
        """.trimIndent()
    )

    private val page1 = page(
        id = 101L, pageIndex = 1, regionsJson = """
        [{"id": "r1", "bbox": [0.0, 0.0, 0.5, 0.5], "kind": "MATH", "resolvedAt": "UNRESOLVED", "confidence": 0.2, "isUncertain": true, "route": "N1.5 - OCR matematico", "text": "Σ i"}]
        """.trimIndent()
    )

    private fun regionOf(page: NotePage, regionId: String): MathRegion = page.mathRegions().single { it.regionId == regionId }

    @Test
    fun `solo las regiones kind MATH son candidatas y conservan pagina, bbox y OCR original`() {
        val regions = page0.mathRegions()

        assertEquals(listOf("r1", "r2"), regions.map { it.regionId })
        val r1 = regions.first()
        assertEquals(7L, r1.noteId)
        assertEquals(100L, r1.pageId)
        assertEquals(0, r1.pageIndex)
        assertEquals(listOf(0.2, 0.3, 0.8, 0.4), r1.bbox)
        assertEquals("lim x→a f(x) = L", r1.ocrText)
    }

    @Test
    fun `LaTeX valido con confianza suficiente se queda en N1_5 sin escalar`() {
        val decision = policy.decide(regionOf(page0, "r1"), MathTranscription("""\lim_{x \to a} f(x) = L""", 0.92f))

        assertEquals(FormulaStatus.KEEP_N1_5, decision.status)
        assertEquals(true, decision.latexCompiles)
        assertTrue(decision.escalationReasons.isEmpty())
        assertEquals("N1_5", decision.status.resolvedAt)
        assertEquals("""\lim_{x \to a} f(x) = L""", decision.latex)
    }

    @Test
    fun `LaTeX que no compila queda como candidata a N2 sin marcar N2 como ejecutado`() {
        val region = regionOf(page0, "r2")
        val decision = policy.decide(region, MathTranscription("""\int x \dx""", 0.95f))

        assertEquals(FormulaStatus.NEEDS_N2, decision.status)
        assertEquals(false, decision.latexCompiles)
        assertEquals(setOf(EscalationReason.LATEX_DOES_NOT_COMPILE), decision.escalationReasons)
        // "Necesita N2" no es "resuelto por N2": Android leería N2 como Escalated (ya reparado).
        assertEquals("UNRESOLVED", decision.status.resolvedAt)
        // La región y su contexto viajan intactos para que N2 sepa qué recorte reparar.
        assertEquals(region, decision.region)
        assertEquals("∫ x dx", decision.region.ocrText)
        // El LaTeX recibido se conserva tal cual, sin corregir.
        assertEquals("""\int x \dx""", decision.latex)
    }

    @Test
    fun `LaTeX vacio recibido de N1_5 es invalido, no pendiente`() {
        val decision = policy.decide(regionOf(page0, "r1"), MathTranscription("   ", 0.9f))

        assertEquals(FormulaStatus.NEEDS_N2, decision.status)
        assertEquals(false, decision.latexCompiles)
    }

    @Test
    fun `region sin resultado de N1_5 queda pendiente de OCR, no invalida ni escalada`() {
        val decision = policy.decide(regionOf(page0, "r1"), null)

        assertEquals(FormulaStatus.AWAITING_N1_5, decision.status)
        assertNull(decision.latex)
        assertNull(decision.latexCompiles)
        assertTrue(decision.escalationReasons.isEmpty())
        assertEquals("UNRESOLVED", decision.status.resolvedAt)
    }

    @Test
    fun `el texto OCR que sube Android nunca se valida como LaTeX`() {
        // Sin transcripción de N1.5 no se mira el `text` de la región, aunque "parezca" LaTeX.
        val withLatexLookingText = page(
            id = 102L, pageIndex = 2,
            regionsJson = """[{"id": "r0", "kind": "MATH", "resolvedAt": "UNRESOLVED", "text": "\\frac{a}{b}"}]"""
        )
        val decision = policy.decidePage(withLatexLookingText, emptyMap()).single()

        assertEquals(FormulaStatus.AWAITING_N1_5, decision.status)
        assertNull(decision.latexCompiles)
    }

    @Test
    fun `LaTeX que compila con confianza baja escala por confianza`() {
        val decision = policy.decide(regionOf(page0, "r1"), MathTranscription("""\frac{a}{b}""", 0.40f))

        assertEquals(FormulaStatus.NEEDS_N2, decision.status)
        assertEquals(true, decision.latexCompiles)
        assertEquals(setOf(EscalationReason.LOW_CONFIDENCE), decision.escalationReasons)
    }

    @Test
    fun `la compuerta de confianza es inclusiva y ambos motivos se acumulan`() {
        val atGate = policy.decide(regionOf(page0, "r1"), MathTranscription("x", FormulaEscalationPolicy.CONFIDENCE_GATE))
        assertEquals(FormulaStatus.KEEP_N1_5, atGate.status)

        val both = policy.decide(regionOf(page0, "r1"), MathTranscription("""\foo""", 0.1f))
        assertEquals(setOf(EscalationReason.LATEX_DOES_NOT_COMPILE, EscalationReason.LOW_CONFIDENCE), both.escalationReasons)
    }

    @Test
    fun `decidePage mezcla valida, invalida y pendiente en la misma pagina`() {
        val decisions = policy.decidePage(page0, mapOf("r1" to MathTranscription("""\sqrt{x}""", 0.9f)))

        assertEquals(listOf("r1", "r2"), decisions.map { it.region.regionId })
        assertEquals(listOf(FormulaStatus.KEEP_N1_5, FormulaStatus.AWAITING_N1_5), decisions.map { it.status })
    }

    @Test
    fun `el mismo id de region en paginas distintas no se mezcla`() {
        val onPage0 = policy.decidePage(page0, mapOf("r1" to MathTranscription("""\sqrt{x}""", 0.9f))).single { it.region.regionId == "r1" }
        val onPage1 = policy.decidePage(page1, mapOf("r1" to MathTranscription("""\sum_{i} \foo""", 0.9f))).single()

        assertEquals(0, onPage0.region.pageIndex)
        assertEquals(100L, onPage0.region.pageId)
        assertEquals(FormulaStatus.KEEP_N1_5, onPage0.status)

        assertEquals(1, onPage1.region.pageIndex)
        assertEquals(101L, onPage1.region.pageId)
        assertEquals("Σ i", onPage1.region.ocrText)
        assertEquals(FormulaStatus.NEEDS_N2, onPage1.status)
    }

    @Test
    fun `una transcripcion para una region que no es MATH de la pagina es un error del llamador`() {
        assertThrows<IllegalArgumentException> {
            policy.decidePage(page0, mapOf("r0" to MathTranscription("x", 0.9f)))
        }
    }

    @Test
    fun `la confianza de N1_5 fuera de rango se rechaza`() {
        assertThrows<IllegalArgumentException> { MathTranscription("x", 1.5f) }
        assertThrows<IllegalArgumentException> { MathTranscription("x", Float.NaN) }
    }

    @Test
    fun `pagina sin regiones o con regiones que no son lista no tiene candidatas`() {
        assertTrue(page(103L, 3, "[]").mathRegions().isEmpty())
        assertTrue(page(104L, 4, """{"id": "r0"}""").mathRegions().isEmpty())
        assertTrue(NotePage().apply { this.note = this@FormulaEscalationPolicyTest.note }.mathRegions().isEmpty())
    }
}

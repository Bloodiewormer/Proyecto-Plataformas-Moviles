package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.OpenAiProperties
import cr.ac.una.glifo.ai.engine.MathOcrEngine
import cr.ac.una.glifo.ai.engine.MathResult
import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.ledger.AiCallContext
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
import cr.ac.una.glifo.ai.ledger.AiCallType
import cr.ac.una.glifo.ai.service.AmbiguousTopic
import cr.ac.una.glifo.ai.service.MathOcrService
import cr.ac.una.glifo.ai.service.PageRegionStore
import cr.ac.una.glifo.ai.service.SemanticJudgeService
import cr.ac.una.glifo.ai.service.VisionRepairService
import cr.ac.una.glifo.ai.validation.FormulaEscalationPolicy
import cr.ac.una.glifo.ai.validation.LatexValidator
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.entity.User
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.transaction.support.TransactionOperations
import tools.jackson.databind.json.JsonMapper
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Registro en ai_calls (#46) de las llamadas que agregaron #38 (N1.5), #41 (N2) y #43 (IA-04),
 * con motores y proveedores falsos (sin OpenAI) y el recorder en memoria.
 */
class PipelineAuditTest {

    private val json = JsonMapper.builder().findAndAddModules().build()
    private lateinit var recorder: RecordingAiCallRecorder
    private lateinit var noteRepository: NoteRepository
    private lateinit var pageRepository: NotePageRepository
    private lateinit var store: PageRegionStore

    private val note = Note().apply { id = 7L; user = User().apply { id = 1L }; course = Course().apply { id = 10L } }
    private val png = RegionCrop(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00), "image/png")
    private val origin = AiCallOrigin(userId = 1L, courseId = 10L)

    private var regions = """
        [
          {"id": "r1", "kind": "TEXT", "resolvedAt": "UNRESOLVED", "confidence": 0.3, "isUncertain": true, "text": "pa7a t0do"},
          {"id": "r2", "kind": "MATH", "resolvedAt": "UNRESOLVED", "confidence": 0.4, "isUncertain": true, "text": "lim"}
        ]
    """.trimIndent()

    private fun page() = NotePage().apply {
        id = 70L; pageIndex = 0; levelReached = "N1"; note = this@PipelineAuditTest.note; this.regions = this@PipelineAuditTest.regions
    }

    @BeforeEach
    fun setUp() {
        recorder = RecordingAiCallRecorder()
        noteRepository = mock(NoteRepository::class.java)
        pageRepository = mock(NotePageRepository::class.java)
        store = PageRegionStore(noteRepository, pageRepository, TransactionOperations.withoutTransaction(), json, mock(EntityManager::class.java))
        `when`(noteRepository.findById(7L)).thenReturn(Optional.of(note))
        `when`(pageRepository.findByNoteIdAndPageIndex(7L, 0)).thenAnswer { page() }
        `when`(pageRepository.findForUpdate(7L, 0)).thenAnswer { page() }
        `when`(pageRepository.save(any(NotePage::class.java))).thenAnswer { it.arguments[0] }
    }

    // ---------- N1.5 (#38): OCR_M, nivel N1_5 ----------

    private class FakeEngine(var result: () -> MathResult, var available: Boolean = true) : MathOcrEngine {
        var calls = 0
        override fun name() = "Fake"
        override fun isAvailable() = available
        override fun recognize(crop: RegionCrop): MathResult { calls++; return result() }
    }

    private fun mathResult(latex: String, confidence: Float) =
        MathResult(latex, confidence, "Fake", "gpt-4o-mini-2024-07-18", inputTokens = 2900, outputTokens = 12, latencyMs = 300)

    private fun mathOcr(engine: MathOcrEngine) =
        MathOcrService(store, engine, FormulaEscalationPolicy(LatexValidator()), recordingLedger(recorder))

    @Test
    fun `N1_5 aceptado deja un registro OCR_M en N1_5 con el consumo del motor`() {
        mathOcr(FakeEngine({ mathResult("\\frac{a}{b}", 0.92f) })).transcribeRegion(7L, 1L, 0, "r2", png)

        val record = recorder.records.single()
        assertEquals(AiCallType.OCR_M, record.context.callType)
        assertEquals("N1_5", record.context.level)
        assertEquals(origin, record.context.origin)
        assertEquals("gpt-4o-mini-2024-07-18", record.model)
        assertEquals(2900, record.inputTokens)
        assertEquals(12, record.outputTokens)
        assertEquals(300L, record.latencyMs)
        assertEquals(true, record.success)
        assertEquals(true, record.outputAccepted)
    }

    @Test
    fun `N1_5 que escala a N2 queda como respuesta recibida pero no aceptada, no como N2`() {
        val response = mathOcr(FakeEngine({ mathResult("\\frac{a}{b}", 0.40f) })).transcribeRegion(7L, 1L, 0, "r2", png)

        assertEquals("NEEDS_N2", response.status)
        val record = recorder.records.single()
        assertEquals("N1_5", record.context.level)
        assertEquals(true, record.success)
        assertEquals(false, record.outputAccepted)
        assertEquals("OUTPUT_REJECTED", record.errorCode)
        assertTrue(record.errorMessage!!.contains("LOW_CONFIDENCE"))
    }

    @Test
    fun `N1_5 con fallo del proveedor registra success false y relanza la misma excepcion`() {
        val original = AiProviderException("El proveedor de IA devolvió una respuesta vacía")

        val thrown = assertThrows<AiProviderException> {
            mathOcr(FakeEngine({ throw original })).transcribeRegion(7L, 1L, 0, "r2", png)
        }

        assertSame(original, thrown)
        assertEquals(false, recorder.records.single().success)
    }

    @Test
    fun `N1_5 sin motor no registra nada y con resultado ya guardado cuenta una evitada`() {
        val unavailable = FakeEngine({ mathResult("x", 0.9f) }, available = false)
        assertThrows<AiProviderException> { mathOcr(unavailable).transcribeRegion(7L, 1L, 0, "r2", png) }
        assertTrue(recorder.avoided.isEmpty())

        regions = regions.replace(
            """"kind": "MATH", "resolvedAt": "UNRESOLVED", "confidence": 0.4, "isUncertain": true, "text": "lim"""",
            """"kind": "MATH", "resolvedAt": "N1_5", "confidence": 0.9, "isUncertain": false, "latex": "x^2", "text": "lim""""
        )
        val engine = FakeEngine({ mathResult("x", 0.9f) })
        val cached = mathOcr(engine).transcribeRegion(7L, 1L, 0, "r2", png)

        assertTrue(cached.cached)
        assertEquals(0, engine.calls + unavailable.calls)
        assertTrue(recorder.records.isEmpty())
        assertEquals(listOf(AiCallContext(origin, AiCallType.OCR_M, "N1_5")), recorder.avoided)
    }

    // ---------- N2 (#41): IA_00, nivel N2 ----------

    private class FakeLlm(private val tokens: List<TokenLogprob>) : LlmClient {
        var calls = 0
        override fun completeJson(request: LlmRequest): LlmResult {
            calls++
            return LlmResult(tokens.joinToString("") { it.token }, "gpt-4o-mini", 3100, 40, 900, tokenLogprobs = tokens, finishReason = "stop")
        }
    }

    private fun repair(llm: LlmClient, apiKey: String = "key") =
        VisionRepairService(store, llm, OpenAiProperties(apiKey = apiKey), LatexValidator(), recordingLedger(recorder))

    @Test
    fun `N2 registra una sola fila IA_00 en N2 por pagina aunque viajen varios recortes`() {
        val llm = FakeLlm(listOf(
            TokenLogprob("r1|", 0.0), TokenLogprob("para todo", -0.05), TokenLogprob("\n", 0.0),
            TokenLogprob("r2|", 0.0), TokenLogprob("\\frac{a}{b}", -0.1)
        ))

        val response = repair(llm).repairPage(7L, 1L, 0, linkedMapOf("r1" to png, "r2" to png))

        assertEquals(2, response.repaired)
        val record = recorder.records.single()
        assertEquals(AiCallType.IA_00, record.context.callType)
        assertEquals("N2", record.context.level)
        assertEquals(origin, record.context.origin)
        assertEquals(3100, record.inputTokens)
        assertEquals(true, record.outputAccepted)
    }

    @Test
    fun `N2 sin ninguna region aceptada queda como contenido no aceptado`() {
        val llm = FakeLlm(listOf(TokenLogprob("r1|", 0.0), TokenLogprob("ILEGIBLE", -0.01), TokenLogprob("\n", 0.0), TokenLogprob("r2|", 0.0), TokenLogprob("ILEGIBLE", -0.01)))

        val response = repair(llm).repairPage(7L, 1L, 0, linkedMapOf("r1" to png, "r2" to png))

        assertEquals(0, response.repaired)
        val record = recorder.records.single()
        assertEquals(true, record.success)
        assertEquals(false, record.outputAccepted)
        assertEquals("OUTPUT_REJECTED", record.errorCode)
    }

    @Test
    fun `N2 sin configurar no llama al proveedor ni registra`() {
        val llm = FakeLlm(emptyList())

        assertThrows<AiProviderException> { repair(llm, apiKey = "").repairPage(7L, 1L, 0, linkedMapOf("r1" to png)) }

        assertEquals(0, llm.calls)
        assertTrue(recorder.records.isEmpty())
        assertTrue(recorder.avoided.isEmpty())
    }

    @Test
    fun `N2 con todas las regiones pedidas ya resueltas no llama y cuenta una evitada por pagina`() {
        regions = regions.replace(""""resolvedAt": "UNRESOLVED"""", """"resolvedAt": "N1"""")
        val llm = FakeLlm(emptyList())

        val response = repair(llm).repairPage(7L, 1L, 0, linkedMapOf("r1" to png, "r2" to png))

        assertEquals(0, response.repaired)
        assertEquals(0, llm.calls)
        assertTrue(recorder.records.isEmpty())
        assertEquals(listOf(AiCallContext(origin, AiCallType.IA_00, "N2")), recorder.avoided)
    }

    @Test
    fun `N2 con una sola region pendiente llama y no cuenta evitadas`() {
        regions = regions.replace(""""kind": "TEXT", "resolvedAt": "UNRESOLVED"""", """"kind": "TEXT", "resolvedAt": "N1"""")
        val llm = FakeLlm(listOf(TokenLogprob("r2|", 0.0), TokenLogprob("\\frac{a}{b}", -0.1)))

        repair(llm).repairPage(7L, 1L, 0, linkedMapOf("r2" to png))

        assertEquals(1, llm.calls)
        assertEquals(1, recorder.records.size)
        assertTrue(recorder.avoided.isEmpty())
    }

    // ---------- IA-04 (#43): IA_04, sin nivel ----------

    private class FixedLlm(private val content: String) : LlmClient {
        var calls = 0
        override fun completeJson(request: LlmRequest): LlmResult {
            calls++
            return LlmResult(content, "gpt-4o-mini", 900, 60, 400, finishReason = "stop")
        }
    }

    private val topics = listOf(AmbiguousTopic(101L, "U1.1", "Límites", listOf("límite lateral")))
    private fun judge(llm: LlmClient) = SemanticJudgeService(llm, OpenAiProperties(apiKey = "key"), json, recordingLedger(recorder))

    @Test
    fun `IA-04 registra una fila IA_04 sin nivel a nombre del estudiante y su curso`() {
        val verdicts = judge(FixedLlm("""{"verdicts": [{"topicId": 101, "state": "SOLID", "score": 0.9}]}""")).judge("Cálculo I", topics, origin)

        assertEquals(1, verdicts.size)
        val record = recorder.records.single()
        assertEquals(AiCallType.IA_04, record.context.callType)
        assertNull(record.context.level)
        assertEquals(origin, record.context.origin)
        assertEquals(true, record.outputAccepted)
    }

    @Test
    fun `IA-04 sin veredictos validos queda como contenido no aceptado`() {
        assertTrue(judge(FixedLlm("no es json")).judge("Cálculo I", topics, origin).isEmpty())

        val record = recorder.records.single()
        assertEquals(true, record.success)
        assertEquals(false, record.outputAccepted)
        assertEquals("OUTPUT_REJECTED", record.errorCode)
    }

    @Test
    fun `IA-04 sin temas ambiguos no llama al proveedor ni registra`() {
        val llm = FixedLlm("{}")

        assertTrue(judge(llm).judge("Cálculo I", emptyList(), origin).isEmpty())

        assertEquals(0, llm.calls)
        assertTrue(recorder.records.isEmpty())
        // El pre-filtro determinista no es una reutilización: no cuenta como evitada.
        assertTrue(recorder.avoided.isEmpty())
    }
}

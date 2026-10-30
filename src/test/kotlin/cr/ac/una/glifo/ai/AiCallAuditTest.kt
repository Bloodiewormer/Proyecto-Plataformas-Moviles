package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.AiPricingProperties
import cr.ac.una.glifo.ai.ledger.AiCallContext
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
import cr.ac.una.glifo.ai.ledger.AiCallRecord
import cr.ac.una.glifo.ai.ledger.AiCallRecorder
import cr.ac.una.glifo.ai.ledger.AiCallType
import cr.ac.una.glifo.ai.ledger.CostCalculator
import cr.ac.una.glifo.ai.ledger.CostLedgerService
import cr.ac.una.glifo.ai.service.GenerationService
import cr.ac.una.glifo.ai.service.ReconstructionService
import cr.ac.una.glifo.ai.service.TopicInput
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.glossary.repository.CourseGlossaryRepository
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.study.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.study.entity.StudyItem
import cr.ac.una.glifo.study.repository.StudyItemRepository
import cr.ac.una.glifo.study.service.StudyItemService
import cr.ac.una.glifo.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.web.client.ResourceAccessException
import tools.jackson.databind.json.JsonMapper
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * IA-01 e IA-02 con la auditoría de ai_calls (#46) conectada de punta a punta: servicio →
 * CostLedgerService → AiCallRecorder. El proveedor es un LlmClient simulado (sin OpenAI ni
 * créditos) y el recorder un doble que junta las filas en memoria.
 */
class AiCallAuditTest {

    /** Doble del recorder: guarda en una lista o falla, según la prueba. */
    private class FakeRecorder : AiCallRecorder(
        mock(cr.ac.una.glifo.ops.repository.AiCallRepository::class.java),
        mock(cr.ac.una.glifo.ops.repository.AiAvoidedCallRepository::class.java),
        mock(cr.ac.una.glifo.user.repository.UserRepository::class.java),
        mock(CourseRepository::class.java)
    ) {
        val records = mutableListOf<AiCallRecord>()
        val avoided = mutableListOf<AiCallContext>()
        var failWith: RuntimeException? = null
        override fun save(record: AiCallRecord): Long {
            failWith?.let { throw it }
            records += record
            return records.size.toLong()
        }
        override fun saveAvoided(context: AiCallContext): Long {
            failWith?.let { throw it }
            avoided += context
            return avoided.size.toLong()
        }
    }

    private val jsonMapper = JsonMapper.builder().findAndAddModules().build()
    private lateinit var recorder: FakeRecorder
    private lateinit var llmClient: LlmClient
    private lateinit var ledger: CostLedgerService

    private val student = User().apply { id = 1L }
    private val course = Course().apply { id = 10L; name = "Cálculo I" }

    @BeforeEach
    fun setUp() {
        recorder = FakeRecorder()
        llmClient = mock(LlmClient::class.java)
        ledger = CostLedgerService(recorder, CostCalculator(AiPricingProperties()), System::nanoTime)
    }

    private fun stubLlm(content: String) {
        `when`(llmClient.completeJson(any(LlmRequest::class.java) ?: LlmRequest("", "")))
            .thenReturn(LlmResult(content, "gpt-4o-mini-2024-07-18", inputTokens = 210, outputTokens = 190, latencyMs = 380L))
    }

    private fun stubLlmFailure(ex: Exception) {
        `when`(llmClient.completeJson(any(LlmRequest::class.java) ?: LlmRequest("", ""))).thenThrow(ex)
    }

    // ---------- IA-02 ----------

    private val generation get() = GenerationService(llmClient, jsonMapper, ledger)
    private val topic = TopicInput(id = 101L, code = "U1.1", title = "Límite")
    private val origin = AiCallOrigin(userId = 1L, courseId = 10L)

    private val validItems = """
        {"items": [{"topicId": 101, "kind": "FLASHCARD", "front": "¿Qué es un límite?", "back": "El valor al que tiende f(x)"}]}
    """.trimIndent()

    @Test
    fun `IA-02 exitosa deja un unico registro IA_02 sin nivel a nombre del usuario y curso`() {
        stubLlm(validItems)

        generation.generateStudyItems("Cálculo I", listOf(topic), 1, origin)

        val record = recorder.records.single()
        assertEquals(AiCallType.IA_02, record.context.callType)
        assertNull(record.context.level)
        assertEquals(origin, record.context.origin)
        assertEquals("gpt-4o-mini-2024-07-18", record.model)
        assertEquals(210, record.inputTokens)
        assertEquals(380L, record.latencyMs)
        assertEquals(true, record.success)
        assertEquals(true, record.outputAccepted)
    }

    @Test
    fun `IA-02 con fallo de red registra success false y conserva la excepcion de siempre`() {
        stubLlmFailure(AiProviderException("No se pudo obtener respuesta del proveedor de IA", cause = ResourceAccessException("Connection refused")))

        val ex = assertThrows<AiProviderException> { generation.generateStudyItems("Cálculo I", listOf(topic), 1, origin) }

        assertEquals("No se pudo obtener respuesta del proveedor de IA", ex.message)
        assertEquals("AI_PROVIDER_ERROR", ex.code)
        val record = recorder.records.single()
        assertEquals(false, record.success)
        assertEquals("PROVIDER_UNREACHABLE", record.errorCode)
    }

    @Test
    fun `IA-02 con respuesta que no cumple el esquema registra la llamada pagada como contenido rechazado`() {
        stubLlm("""{"otraCosa": []}""")

        val ex = assertThrows<AiProviderException> { generation.generateStudyItems("Cálculo I", listOf(topic), 1, origin) }

        assertEquals("El JSON devuelto por la IA no contiene el arreglo 'items'", ex.message)
        val record = recorder.records.single()
        assertEquals(true, record.success)
        assertEquals(false, record.outputAccepted)
        assertEquals("OUTPUT_REJECTED", record.errorCode)
        assertEquals(210, record.inputTokens)
    }

    @Test
    fun `IA-02 sin configurar no registra nada y responde igual que antes`() {
        stubLlmFailure(AiProviderException.notConfigured())

        val ex = assertThrows<AiProviderException> { generation.generateStudyItems("Cálculo I", listOf(topic), 1, origin) }

        assertTrue(ex.isNotConfigured)
        assertTrue(recorder.records.isEmpty())
    }

    @Test
    fun `IA-02 exitosa sigue devolviendo los items aunque falle la auditoria`() {
        stubLlm(validItems)
        recorder.failWith = DataAccessResourceFailureException("Connection refused")

        val batch = generation.generateStudyItems("Cálculo I", listOf(topic), 1, origin)

        assertEquals(1, batch.items.size)
    }

    @Test
    fun `reutilizar todo el material no llama al proveedor y cuenta una llamada IA-02 evitada`() {
        val courseRepository = mock(CourseRepository::class.java)
        val enrollmentRepository = mock(EnrollmentRepository::class.java)
        val syllabusTopicRepository = mock(SyllabusTopicRepository::class.java)
        val studyItemRepository = mock(StudyItemRepository::class.java)
        val owner = User().apply { id = 2L }
        val ownedCourse = Course().apply { id = 10L; name = "Cálculo I"; this.owner = owner }
        val syllabusTopic = SyllabusTopic().apply { id = 101L; code = "U1.1"; title = "Límite"; this.course = ownedCourse }
        `when`(courseRepository.findById(10L)).thenReturn(Optional.of(ownedCourse))
        `when`(syllabusTopicRepository.findByCourseIdOrderByOrderIndex(10L)).thenReturn(listOf(syllabusTopic))
        `when`(studyItemRepository.findByCourseIdAndSyllabusTopicId(10L, 101L)).thenReturn(
            listOf(StudyItem().apply { id = 900L; this.course = ownedCourse; this.syllabusTopic = syllabusTopic; kind = "FLASHCARD"; payload = "{}" })
        )
        val studyItemService = StudyItemService(courseRepository, enrollmentRepository, syllabusTopicRepository, studyItemRepository, generation)

        val outcome = studyItemService.generate(10L, GenerateStudyItemsRequest(topicIds = listOf(101L), itemsPerTopic = 2), userId = 2L)

        assertEquals(1, outcome.reusedTopics)
        verify(llmClient, never()).completeJson(any(LlmRequest::class.java) ?: LlmRequest("", ""))
        assertTrue(recorder.records.isEmpty())
        assertEquals(listOf(AiCallContext(AiCallOrigin(2L, 10L), AiCallType.IA_02, null)), recorder.avoided)
    }

    // ---------- IA-01 ----------

    private val noteRepository = mock(NoteRepository::class.java)
    private val notePageRepository = mock(NotePageRepository::class.java)
    private val syllabusTopicRepository = mock(SyllabusTopicRepository::class.java)
    private val courseGlossaryRepository = mock(CourseGlossaryRepository::class.java)

    private val reconstruction get() = ReconstructionService(
        noteRepository = noteRepository,
        notePageRepository = notePageRepository,
        syllabusTopicRepository = syllabusTopicRepository,
        courseGlossaryRepository = courseGlossaryRepository,
        llmClient = llmClient,
        latexValidator = cr.ac.una.glifo.ai.validation.LatexValidator(),
        jsonMapper = jsonMapper,
        costLedgerService = ledger
    )

    private fun noteWithPage(status: NoteStatus = NoteStatus.PROCESSING, content: String? = null): Note {
        val note = Note().apply {
            id = 50L; user = student; this.course = this@AiCallAuditTest.course
            title = "Apunte"; this.status = status.name; this.content = content
        }
        val page = NotePage().apply {
            id = 1L; this.note = note; pageIndex = 0; perceptualHash = "c3a1f0e98b7d6245"
            regions = """[{"id": "r1", "bbox": [0.1, 0.1, 0.9, 0.2], "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.96, "text": "Decimos que f tiende a L"}]"""
        }
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(note))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(50L)).thenReturn(listOf(page))
        `when`(noteRepository.save(any(Note::class.java))).thenAnswer { it.arguments[0] }
        return note
    }

    private val validBlocks = """
        {"schemaVersion": 1, "blocks": [{"id": "b1", "type": "PARAGRAPH", "spans": [{"text": "Decimos que f tiende a L", "regionId": "r1"}]}]}
    """.trimIndent()

    @Test
    fun `IA-01 exitosa deja un unico registro IA_01 a nombre del duenno del apunte y su curso`() {
        noteWithPage()
        stubLlm(validBlocks)

        val outcome = reconstruction.reconstructNote(50L, 1L)

        assertTrue(outcome.reconstructed)
        val record = recorder.records.single()
        assertEquals(AiCallType.IA_01, record.context.callType)
        assertNull(record.context.level)
        assertEquals(AiCallOrigin(userId = 1L, courseId = 10L), record.context.origin)
        assertEquals(true, record.success)
        assertEquals(true, record.outputAccepted)
    }

    @Test
    fun `IA-01 con error del proveedor registra success false y relanza la misma excepcion`() {
        noteWithPage()
        val original = AiProviderException("El proveedor de IA devolvió una respuesta vacía")
        stubLlmFailure(original)

        val thrown = assertThrows<AiProviderException> { reconstruction.reconstructNote(50L, 1L) }

        assertSame(original, thrown)
        val record = recorder.records.single()
        assertEquals(false, record.success)
        assertEquals("PROVIDER_INVALID_RESPONSE", record.errorCode)
    }

    @Test
    fun `IA-01 con bloques invalidos registra contenido rechazado y no guarda el apunte`() {
        noteWithPage()
        stubLlm("""{"blocks": [{"id": "b1", "type": "TABLA"}]}""")

        assertThrows<AiProviderException> { reconstruction.reconstructNote(50L, 1L) }

        val record = recorder.records.single()
        assertEquals(true, record.success)
        assertEquals(false, record.outputAccepted)
        verify(noteRepository, never()).save(any(Note::class.java))
    }

    @Test
    fun `IA-01 sobre un apunte ya reconstruido no llama al proveedor y cuenta una llamada evitada`() {
        noteWithPage(status = NoteStatus.READY, content = validBlocks)

        val outcome = reconstruction.reconstructNote(50L, 1L)

        assertEquals(false, outcome.reconstructed)
        verify(llmClient, never()).completeJson(any(LlmRequest::class.java) ?: LlmRequest("", ""))
        assertTrue(recorder.records.isEmpty())
        assertEquals(listOf(AiCallContext(AiCallOrigin(1L, 10L), AiCallType.IA_01, null)), recorder.avoided)
    }

    @Test
    fun `si falla el registro de una evitada, IA-01 igual devuelve el apunte guardado`() {
        noteWithPage(status = NoteStatus.READY, content = validBlocks)
        recorder.failWith = DataAccessResourceFailureException("Connection refused")

        assertEquals(false, reconstruction.reconstructNote(50L, 1L).reconstructed)
    }

    @Test
    fun `IA-01 exitosa guarda el apunte aunque falle la auditoria`() {
        noteWithPage()
        stubLlm(validBlocks)
        recorder.failWith = DataAccessResourceFailureException("Connection refused")

        val outcome = reconstruction.reconstructNote(50L, 1L)

        assertTrue(outcome.reconstructed)
        assertEquals(NoteStatus.READY.name, outcome.note.status)
    }
}

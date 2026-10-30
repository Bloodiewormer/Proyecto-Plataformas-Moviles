package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.AiPricingProperties
import cr.ac.una.glifo.ai.ledger.AiCallRecorder
import cr.ac.una.glifo.ai.ledger.CostCalculator
import cr.ac.una.glifo.ai.ledger.CostLedgerService
import cr.ac.una.glifo.ai.service.ReconstructionService
import cr.ac.una.glifo.ai.validation.LatexValidator
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.glossary.entity.CourseGlossary
import cr.ac.una.glifo.glossary.repository.CourseGlossaryRepository
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import tools.jackson.databind.json.JsonMapper
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReconstructionServiceTest {

    private lateinit var noteRepository: NoteRepository
    private lateinit var notePageRepository: NotePageRepository
    private lateinit var syllabusTopicRepository: SyllabusTopicRepository
    private lateinit var courseGlossaryRepository: CourseGlossaryRepository
    private lateinit var llmClient: LlmClient
    private lateinit var latexValidator: LatexValidator
    private lateinit var service: ReconstructionService

    private val jsonMapper = JsonMapper.builder().findAndAddModules().build()

    private val student = User().apply { id = 1L; email = "estudiante@est.una.ac.cr" }
    private val otherStudent = User().apply { id = 2L; email = "otro@est.una.ac.cr" }
    private val course = Course().apply { id = 10L; name = "Cálculo I" }

    private val topic101 = SyllabusTopic().apply {
        id = 101L
        code = "U1.1"
        title = "Definición épsilon-delta"
        this.course = this@ReconstructionServiceTest.course
    }

    private val glossaryLim = CourseGlossary().apply {
        id = 1L
        term = "lim"
        canonicalForm = "\\lim_{x \\to a}"
        kind = "NOTATION"
        this.course = this@ReconstructionServiceTest.course
    }

    @BeforeEach
    fun setUp() {
        noteRepository = mock(NoteRepository::class.java)
        notePageRepository = mock(NotePageRepository::class.java)
        syllabusTopicRepository = mock(SyllabusTopicRepository::class.java)
        courseGlossaryRepository = mock(CourseGlossaryRepository::class.java)
        llmClient = mock(LlmClient::class.java)
        latexValidator = LatexValidator()

        service = ReconstructionService(
            noteRepository = noteRepository,
            notePageRepository = notePageRepository,
            syllabusTopicRepository = syllabusTopicRepository,
            courseGlossaryRepository = courseGlossaryRepository,
            llmClient = llmClient,
            latexValidator = latexValidator,
            jsonMapper = jsonMapper,
            // El registro en ai_calls se prueba en AiCallAuditTest; acá solo se simula el guardado.
            costLedgerService = CostLedgerService(mock(AiCallRecorder::class.java), CostCalculator(AiPricingProperties()), System::nanoTime)
        )

        `when`(noteRepository.save(any(Note::class.java))).thenAnswer { it.arguments[0] }
        `when`(syllabusTopicRepository.findByCourseIdOrderByOrderIndex(10L)).thenReturn(listOf(topic101))
        `when`(courseGlossaryRepository.findByCourseIdOrderByTerm(10L)).thenReturn(listOf(glossaryLim))
    }

    private fun processingNote(
        id: Long = 50L,
        owner: User = student,
        title: String = "Apunte de clase",
        topic: SyllabusTopic? = null
    ) = Note().apply {
        this.id = id
        this.user = owner
        this.course = this@ReconstructionServiceTest.course
        this.syllabusTopic = topic
        this.title = title
        this.status = NoteStatus.PROCESSING.name
    }

    private fun defaultPage(note: Note, pageIndex: Int = 0, regionsJson: String = sampleRegionsJson()) = NotePage().apply {
        this.id = (pageIndex + 1).toLong()
        this.note = note
        this.pageIndex = pageIndex
        this.perceptualHash = "c3a1f0e98b7d6245"
        this.levelReached = "N2"
        this.overallConfidence = 0.70f
        this.regions = regionsJson
    }

    private fun sampleRegionsJson() = """
        [
          {"id": "r1", "bbox": [0.08, 0.12, 0.92, 0.19], "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.96, "isUncertain": false, "text": "Decimos que f tiende a L cuando x tiende a a si"},
          {"id": "r2", "bbox": [0.08, 0.20, 0.92, 0.26], "kind": "TEXT", "resolvedAt": "N2", "confidence": 0.74, "isUncertain": false, "text": "para todo ε existe δ"},
          {"id": "r3", "bbox": [0.14, 0.34, 0.63, 0.43], "kind": "MATH", "resolvedAt": "N1_5", "confidence": 0.88, "isUncertain": false, "latex": "\\lim_{x \\to a} f(x) = L"},
          {"id": "r4", "bbox": [0.12, 0.82, 0.88, 0.90], "kind": "TEXT", "resolvedAt": "UNRESOLVED", "confidence": 0.21, "isUncertain": true, "text": "[ilegible]"}
        ]
    """.trimIndent()

    private fun stubLlmContent(jsonContent: String): ArgumentCaptor<LlmRequest> {
        val captor = ArgumentCaptor.forClass(LlmRequest::class.java)
        `when`(llmClient.completeJson(captor.capture() ?: LlmRequest("", ""))).thenReturn(
            LlmResult(
                content = jsonContent,
                model = "gpt-4o-mini",
                inputTokens = 210,
                outputTokens = 190,
                latencyMs = 380L
            )
        )
        return captor
    }

    @Test
    fun `a - JSON valido guarda content con schemaVersion 1, status READY y contentGeneratedAt`() {
        val note = processingNote(title = "")
        val page = defaultPage(note)
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(note))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(50L)).thenReturn(listOf(page))

        val llmJson = """
            {
              "schemaVersion": 1,
              "blocks": [
                {"id": "b1", "type": "HEADING", "level": 2, "text": "Límite de una función", "regionId": "r1"},
                {
                  "id": "b2",
                  "type": "PARAGRAPH",
                  "spans": [
                    {"text": "Decimos que f tiende a L cuando x tiende a a si ", "regionId": "r1"},
                    {"text": "para todo ε existe δ", "regionId": "r2"}
                  ]
                },
                {"id": "b3", "type": "FORMULA", "latex": "\\lim_{x \\to a} f(x) = L", "regionId": "r3"},
                {
                  "id": "b4",
                  "type": "PARAGRAPH",
                  "spans": [
                    {"text": "[ilegible]", "regionId": "r4"}
                  ]
                }
              ],
              "suggestedTopicId": 101,
              "title": "Límites: definición épsilon-delta"
            }
        """.trimIndent()
        stubLlmContent(llmJson)

        val outcome = service.reconstructNote(50L, 1L)

        assertTrue(outcome.reconstructed)
        assertEquals("READY", note.status)
        assertNotNull(note.contentGeneratedAt)
        assertNotNull(note.content)
        assertEquals(101L, note.syllabusTopic?.id)
        assertEquals("Límites: definición épsilon-delta", note.title)

        val contentNode = jsonMapper.readTree(note.content!!)
        assertEquals(1, contentNode.get("schemaVersion").asInt())
        assertEquals(4, contentNode.get("blocks").size())
        assertTrue(contentNode.get("blocks")[2].get("latexCompiles").asBoolean())
    }

    @Test
    fun `b - si la IA devuelve otra confidence o isUncertain false para region incierta el servidor conserva la original`() {
        val note = processingNote()
        val page = defaultPage(note)
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(note))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(50L)).thenReturn(listOf(page))

        // La IA intenta inflar confidence a 0.99 y poner isUncertain=false / resolvedAt=N1 en r4 (que es UNRESOLVED, 0.21, isUncertain=true)
        val tamperedJson = """
            {
              "schemaVersion": 99,
              "blocks": [
                {
                  "id": "b1",
                  "type": "PARAGRAPH",
                  "spans": [
                    {
                      "text": "Texto inventado como seguro",
                      "confidence": 0.99,
                      "resolvedAt": "N1",
                      "regionId": "r4",
                      "isUncertain": false
                    }
                  ]
                },
                {
                  "id": "b2",
                  "type": "FORMULA",
                  "latex": "\\lim_{x \\to a} f(x) = L",
                  "confidence": 1.0,
                  "resolvedAt": "N1",
                  "regionId": "r3",
                      "isUncertain": true
                }
              ]
            }
        """.trimIndent()
        stubLlmContent(tamperedJson)

        service.reconstructNote(50L, 1L)

        val contentNode = jsonMapper.readTree(note.content!!)
        assertEquals(1, contentNode.get("schemaVersion").asInt())

        val spanR4 = contentNode.get("blocks")[0].get("spans")[0]
        assertEquals(0.21, spanR4.get("confidence").asDouble(), 0.0001)
        assertEquals("UNRESOLVED", spanR4.get("resolvedAt").asString())
        assertTrue(spanR4.get("isUncertain").asBoolean())

        val formulaR3 = contentNode.get("blocks")[1]
        assertEquals(0.88, formulaR3.get("confidence").asDouble(), 0.0001)
        assertEquals("N1_5", formulaR3.get("resolvedAt").asString())
        assertFalse(formulaR3.get("isUncertain").asBoolean())
    }

    @Test
    fun `c - span con regionId inexistente o tipo de bloque desconocido se descarta`() {
        val note = processingNote()
        val page = defaultPage(note)
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(note))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(50L)).thenReturn(listOf(page))

        val mixedJson = """
            {
              "schemaVersion": 1,
              "blocks": [
                {
                  "id": "b1",
                  "type": "CODE_SNIPPET",
                  "text": "Bloque con tipo inválido"
                },
                {
                  "id": "b2",
                  "type": "FORMULA",
                  "latex": "x^2",
                  "regionId": "r999"
                },
                {
                  "id": "b3",
                  "type": "PARAGRAPH",
                  "spans": [
                    {"text": "Span fantasma", "regionId": "r_no_existe"},
                    {"text": "Decimos que f tiende a L", "regionId": "r1"}
                  ]
                }
              ]
            }
        """.trimIndent()
        stubLlmContent(mixedJson)

        service.reconstructNote(50L, 1L)

        val contentNode = jsonMapper.readTree(note.content!!)
        val blocks = contentNode.get("blocks")
        assertEquals(1, blocks.size())
        assertEquals("PARAGRAPH", blocks[0].get("type").asString())

        val spans = blocks[0].get("spans")
        assertEquals(1, spans.size())
        assertEquals("r1", spans[0].get("regionId").asString())
    }

    @Test
    fun `d - summary calculado desde las regiones y no desde la IA`() {
        val note = processingNote()
        val page = defaultPage(note)
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(note))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(50L)).thenReturn(listOf(page))

        // La IA envía un summary falso (100, 200, 300, 0), pero las 4 regiones son 1 N1, 1 N1_5, 1 N2 y 1 UNRESOLVED
        val jsonWithFakeSummary = """
            {
              "schemaVersion": 1,
              "blocks": [
                {
                  "id": "b1",
                  "type": "PARAGRAPH",
                  "spans": [
                    {"text": "Decimos que f tiende a L", "regionId": "r1"}
                  ]
                }
              ],
              "summary": {"verified": 100, "repaired": 200, "escalated": 300, "uncertain": 0}
            }
        """.trimIndent()
        stubLlmContent(jsonWithFakeSummary)

        service.reconstructNote(50L, 1L)

        val summaryNode = jsonMapper.readTree(note.content!!).get("summary")
        assertEquals(1, summaryNode.get("verified").asInt())
        assertEquals(1, summaryNode.get("repaired").asInt())
        assertEquals(1, summaryNode.get("escalated").asInt())
        assertEquals(1, summaryNode.get("uncertain").asInt())
    }

    @Test
    fun `e - nota ya READY con content no llama a la IA`() {
        val existingContent = """{"schemaVersion":1,"blocks":[{"id":"b1","type":"HEADING","level":2,"text":"Existente"}],"summary":{"verified":1,"repaired":0,"escalated":0,"uncertain":0}}"""
        val readyNote = processingNote().apply {
            status = NoteStatus.READY.name
            content = existingContent
        }
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(readyNote))

        val outcome = service.reconstructNote(50L, 1L)

        assertFalse(outcome.reconstructed)
        assertEquals(existingContent, outcome.note.content)
        verify(llmClient, never()).completeJson(any(LlmRequest::class.java) ?: LlmRequest("", ""))
        verify(noteRepository, never()).save(any(Note::class.java))
    }

    @Test
    fun `f - nota sin paginas lanza ConflictException (409) y nota de otro usuario lanza ResourceNotFoundException (404)`() {
        val emptyNote = processingNote(id = 60L)
        `when`(noteRepository.findById(60L)).thenReturn(Optional.of(emptyNote))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(60L)).thenReturn(emptyList())

        val conflictEx = assertThrows<ConflictException> {
            service.reconstructNote(60L, 1L)
        }
        assertEquals("NOTE_HAS_NO_PAGES", conflictEx.code)

        val foreignNote = processingNote(id = 70L, owner = otherStudent)
        `when`(noteRepository.findById(70L)).thenReturn(Optional.of(foreignNote))

        assertThrows<ResourceNotFoundException> {
            service.reconstructNote(70L, 1L)
        }

        verify(llmClient, never()).completeJson(any(LlmRequest::class.java) ?: LlmRequest("", ""))
    }

    @Test
    fun `g - suggestedTopicId de otro curso se ignora`() {
        val note = processingNote(topic = null)
        val page = defaultPage(note)
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(note))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(50L)).thenReturn(listOf(page))

        val jsonWithForeignTopic = """
            {
              "schemaVersion": 1,
              "blocks": [
                {
                  "id": "b1",
                  "type": "PARAGRAPH",
                  "spans": [
                    {"text": "Decimos que f tiende a L", "regionId": "r1"}
                  ]
                }
              ],
              "suggestedTopicId": 999
            }
        """.trimIndent()
        stubLlmContent(jsonWithForeignTopic)

        service.reconstructNote(50L, 1L)

        assertNull(note.syllabusTopic)
    }

    @Test
    fun `h - todos los bloques invalidos lanza AiProviderException`() {
        val note = processingNote()
        val page = defaultPage(note)
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(note))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(50L)).thenReturn(listOf(page))

        val allInvalidJson = """
            {
              "schemaVersion": 1,
              "blocks": [
                {
                  "id": "b1",
                  "type": "UNKNOWN_TYPE",
                  "text": "Inválido"
                },
                {
                  "id": "b2",
                  "type": "PARAGRAPH",
                  "spans": [
                    {"text": "Región inexistente", "regionId": "r_inexistente"}
                  ]
                }
              ]
            }
        """.trimIndent()
        stubLlmContent(allInvalidJson)

        assertThrows<AiProviderException> {
            service.reconstructNote(50L, 1L)
        }
        verify(noteRepository, never()).save(any(Note::class.java))
    }

    @Test
    fun `i - LlmClient se llama exactamente 1 vez aunque la nota tenga varias paginas y ordena por pageIndex y bboxY`() {
        val note = processingNote()
        val page0 = defaultPage(
            note = note,
            pageIndex = 0,
            regionsJson = """
                [
                  {"id": "r2", "bbox": [0.1, 0.50, 0.9, 0.60], "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.90, "isUncertain": false, "text": "Segunda línea pág 0"},
                  {"id": "r1", "bbox": [0.1, 0.10, 0.9, 0.20], "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.95, "isUncertain": false, "text": "Primera línea pág 0"}
                ]
            """.trimIndent()
        )
        val page1 = defaultPage(
            note = note,
            pageIndex = 1,
            regionsJson = """
                [
                  {"id": "r3", "bbox": [0.1, 0.15, 0.9, 0.25], "kind": "MATH", "resolvedAt": "N1_5", "confidence": 0.89, "isUncertain": false, "latex": "f'(x) = 2x"}
                ]
            """.trimIndent()
        )
        `when`(noteRepository.findById(50L)).thenReturn(Optional.of(note))
        `when`(notePageRepository.findByNoteIdOrderByPageIndex(50L)).thenReturn(listOf(page0, page1))

        val multiPageJson = """
            {
              "schemaVersion": 1,
              "blocks": [
                {
                  "id": "b1",
                  "type": "PARAGRAPH",
                  "spans": [
                    {"text": "Primera línea pág 0 ", "regionId": "r1"},
                    {"text": "Segunda línea pág 0", "regionId": "r2"}
                  ]
                },
                {
                  "id": "b2",
                  "type": "FORMULA",
                  "latex": "f'(x) = 2x",
                  "regionId": "r3"
                }
              ]
            }
        """.trimIndent()
        val captor = stubLlmContent(multiPageJson)

        val outcome = service.reconstructNote(50L, 1L)

        assertTrue(outcome.reconstructed)
        verify(llmClient, times(1)).completeJson(captor.value)

        // Verifica que en el userPrompt r1 (bboxY=0.10, pág 0) aparece antes que r2 (bboxY=0.50, pág 0) y r3 (pág 1)
        val userPrompt = captor.value.userPrompt
        val idxR1 = userPrompt.indexOf("regionId=r1")
        val idxR2 = userPrompt.indexOf("regionId=r2")
        val idxR3 = userPrompt.indexOf("regionId=r3")
        assertTrue(idxR1 in 0..<idxR2)
        assertTrue(idxR2 < idxR3)
    }

    @Test
    fun `j - en un DRAWING la descripcion de N2 tiene prioridad sobre el texto suelto de N1`() {
        val page = defaultPage(
            processingNote(),
            regionsJson = """[{"id": "r1", "kind": "DRAWING", "resolvedAt": "N2", "confidence": 0.9, "text": "x 0", "caption": "Una parábola"},
                              {"id": "r2", "kind": "TEXT", "resolvedAt": "N1", "confidence": 0.9, "text": "hola", "caption": "otra"}]"""
        )

        val regions = service.extractOrderedRegions(listOf(page)).associateBy { it.id }

        assertEquals("Una parábola", regions.getValue("r1").text)
        assertEquals("hola", regions.getValue("r2").text)
    }
}

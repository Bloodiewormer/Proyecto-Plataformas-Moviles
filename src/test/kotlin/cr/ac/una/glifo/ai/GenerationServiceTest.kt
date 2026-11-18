package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.AiPricingProperties
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
import cr.ac.una.glifo.ai.ledger.AiCallRecorder
import cr.ac.una.glifo.ai.ledger.CostCalculator
import cr.ac.una.glifo.ai.ledger.CostLedgerService
import cr.ac.una.glifo.ai.service.GenerationService
import cr.ac.una.glifo.ai.service.TopicInput
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerationServiceTest {

    private lateinit var llmClient: LlmClient
    private lateinit var service: GenerationService
    private val jsonMapper = JsonMapper.builder().findAndAddModules().build()
    private val origin = AiCallOrigin(userId = 1L, courseId = 10L)

    private val topic101 = TopicInput(id = 101L, code = "U1.1", title = "Definición de límite")
    private val topic102 = TopicInput(id = 102L, code = "U1.2", title = "Continuidad")

    @BeforeEach
    fun setUp() {
        llmClient = mock(LlmClient::class.java)
        // El registro en ai_calls se prueba en AiCallAuditTest; acá solo se simula el guardado.
        val ledger = CostLedgerService(mock(AiCallRecorder::class.java), CostCalculator(AiPricingProperties()), System::nanoTime)
        service = GenerationService(llmClient, jsonMapper, ledger)
    }

    private fun stubLlmContent(jsonContent: String) {
        val captor = ArgumentCaptor.forClass(LlmRequest::class.java)
        `when`(llmClient.completeJson(captor.capture() ?: LlmRequest("", ""))).thenReturn(
            LlmResult(
                content = jsonContent,
                model = "gpt-4o-mini",
                inputTokens = 180,
                outputTokens = 320,
                latencyMs = 450L
            )
        )
    }

    @Test
    fun `a - JSON valido con 3 kinds acepta los 3 y normaliza schemaVersion y sourceNoteIds`() {
        val validJson = """
            {
              "items": [
                {
                  "topicId": 101,
                  "kind": "FLASHCARD",
                  "front": "¿Qué significa lim x->a f(x) = L?",
                  "back": "Para todo ε > 0 existe δ > 0 tal que 0 < |x - a| < δ implica |f(x) - L| < ε"
                },
                {
                  "topicId": 101,
                  "kind": "MULTIPLE_CHOICE",
                  "stem": "¿Qué teorema garantiza un máximo en un compacto?",
                  "options": [
                    {"id": "a", "text": "Bolzano", "isCorrect": false},
                    {"id": "b", "text": "Weierstrass", "isCorrect": true},
                    {"id": "c", "text": "Rolle", "isCorrect": false}
                  ],
                  "explanation": "El teorema del valor extremo de Weierstrass.",
                  "difficulty": "HARD"
                },
                {
                  "topicId": 101,
                  "kind": "TRUE_FALSE",
                  "statement": "Toda función derivable es continua.",
                  "answer": true,
                  "explanation": "La diferenciabilidad implica continuidad.",
                  "difficulty": "EASY"
                }
              ]
            }
        """.trimIndent()
        stubLlmContent(validJson)

        val batch = service.generateStudyItems("Cálculo I", listOf(topic101), itemsPerTopic = 3, origin = origin)

        assertEquals(3, batch.items.size)
        assertEquals(0, batch.discardedCount)

        for (item in batch.items) {
            val payloadNode = jsonMapper.readTree(item.payload)
            assertEquals(1, payloadNode.get("schemaVersion").asInt())
            assertEquals(item.kind, payloadNode.get("kind").asString())
            assertTrue(payloadNode.get("sourceNoteIds").isArray)
            assertEquals(0, payloadNode.get("sourceNoteIds").size())
        }

        val flashcardPayload = jsonMapper.readTree(batch.items[0].payload)
        assertEquals("MEDIUM", flashcardPayload.get("difficulty").asString())

        val mcPayload = jsonMapper.readTree(batch.items[1].payload)
        assertTrue(mcPayload.get("shuffleOptions").asBoolean())
        assertEquals("HARD", mcPayload.get("difficulty").asString())
    }

    @Test
    fun `b - MULTIPLE_CHOICE con 0 o con 2 correctas se descarta`() {
        val json = """
            {
              "items": [
                {
                  "topicId": 101,
                  "kind": "MULTIPLE_CHOICE",
                  "stem": "Pregunta sin ninguna correcta",
                  "options": [
                    {"id": "a", "text": "Op 1", "isCorrect": false},
                    {"id": "b", "text": "Op 2", "isCorrect": false},
                    {"id": "c", "text": "Op 3", "isCorrect": false}
                  ],
                  "explanation": "Inválida"
                },
                {
                  "topicId": 101,
                  "kind": "MULTIPLE_CHOICE",
                  "stem": "Pregunta con dos correctas",
                  "options": [
                    {"id": "a", "text": "Op 1", "isCorrect": true},
                    {"id": "b", "text": "Op 2", "isCorrect": true},
                    {"id": "c", "text": "Op 3", "isCorrect": false}
                  ],
                  "explanation": "Inválida"
                },
                {
                  "topicId": 101,
                  "kind": "FLASHCARD",
                  "front": "Frente válido",
                  "back": "Reverso válido"
                }
              ]
            }
        """.trimIndent()
        stubLlmContent(json)

        val batch = service.generateStudyItems("Cálculo I", listOf(topic101), itemsPerTopic = 3, origin = origin)

        assertEquals(1, batch.items.size)
        assertEquals("FLASHCARD", batch.items[0].kind)
        assertEquals(2, batch.discardedCount)
    }

    @Test
    fun `c - kind desconocido o topicId fuera del lote se descarta`() {
        val json = """
            {
              "items": [
                {
                  "topicId": 101,
                  "kind": "OPEN_ESSAY",
                  "front": "No soportado",
                  "back": "No soportado"
                },
                {
                  "topicId": 999,
                  "kind": "FLASHCARD",
                  "front": "Tema fuera del lote",
                  "back": "Se debe descartar"
                },
                {
                  "topicId": 101,
                  "kind": "TRUE_FALSE",
                  "statement": "Afirmación válida",
                  "answer": false,
                  "explanation": "Explicación válida"
                }
              ]
            }
        """.trimIndent()
        stubLlmContent(json)

        val batch = service.generateStudyItems("Cálculo I", listOf(topic101), itemsPerTopic = 3, origin = origin)

        assertEquals(1, batch.items.size)
        assertEquals("TRUE_FALSE", batch.items[0].kind)
        assertEquals(2, batch.discardedCount)
    }

    @Test
    fun `d - todos invalidos lanza AiProviderException`() {
        val allInvalidJson = """
            {
              "items": [
                {
                  "topicId": 999,
                  "kind": "FLASHCARD",
                  "front": "Tema ajeno",
                  "back": "Inválido"
                },
                {
                  "topicId": 101,
                  "kind": "FLASHCARD",
                  "front": "",
                  "back": ""
                }
              ]
            }
        """.trimIndent()
        stubLlmContent(allInvalidJson)

        assertThrows<AiProviderException> {
            service.generateStudyItems("Cálculo I", listOf(topic101), itemsPerTopic = 2, origin = origin)
        }
    }

    @Test
    fun `e - varios temas llama a LlmClient exactamente 1 vez`() {
        val json = """
            {
              "items": [
                {
                  "topicId": 101,
                  "kind": "FLASHCARD",
                  "front": "Frente 101",
                  "back": "Reverso 101"
                },
                {
                  "topicId": 102,
                  "kind": "FLASHCARD",
                  "front": "Frente 102",
                  "back": "Reverso 102"
                }
              ]
            }
        """.trimIndent()
        val captor = ArgumentCaptor.forClass(LlmRequest::class.java)
        `when`(llmClient.completeJson(captor.capture() ?: LlmRequest("", ""))).thenReturn(
            LlmResult(
                content = json,
                model = "gpt-4o-mini",
                inputTokens = 100,
                outputTokens = 200,
                latencyMs = 300L
            )
        )

        val batch = service.generateStudyItems("Cálculo I", listOf(topic101, topic102), itemsPerTopic = 1, origin = origin)

        assertEquals(2, batch.items.size)
        verify(llmClient, times(1)).completeJson(captor.value)
    }

    @Test
    fun `f - prompt incluye texto de apuntes y cobertura del estudiante`() {
        val topicWithNotes = TopicInput(
            id = 101L,
            code = "U1.1",
            title = "Definición de límite",
            coverageState = "SOLID",
            notesText = "lim x->a f(x) = L cuando para todo eps>0 existe del>0"
        )
        val topicWithoutNotes = TopicInput(
            id = 102L,
            code = "U1.2",
            title = "Continuidad",
            coverageState = "ABSENT",
            notesText = null
        )

        val json = """
            {
              "items": [
                {
                  "topicId": 101,
                  "kind": "FLASHCARD",
                  "front": "Frente 101",
                  "back": "Reverso 101"
                },
                {
                  "topicId": 102,
                  "kind": "FLASHCARD",
                  "front": "Frente 102",
                  "back": "Reverso 102"
                }
              ]
            }
        """.trimIndent()

        val captor = ArgumentCaptor.forClass(LlmRequest::class.java)
        `when`(llmClient.completeJson(captor.capture() ?: LlmRequest("", ""))).thenReturn(
            LlmResult(
                content = json,
                model = "gpt-4o-mini",
                inputTokens = 120,
                outputTokens = 150,
                latencyMs = 250L
            )
        )

        service.generateStudyItems("Cálculo I", listOf(topicWithNotes, topicWithoutNotes), itemsPerTopic = 1, origin = origin)

        val request = captor.value
        assertTrue(request.userPrompt.contains("cobertura=SOLID"))
        assertTrue(request.userPrompt.contains("apuntes_estudiante:"))
        assertTrue(request.userPrompt.contains("lim x->a f(x) = L"))
        assertTrue(request.userPrompt.contains("(sin apuntes del estudiante para este tema)"))
        assertTrue(request.systemPrompt.contains("refuercen lo que el estudiante escribió"))
        assertTrue(request.systemPrompt.contains("preguntas de quiz"))
    }
}

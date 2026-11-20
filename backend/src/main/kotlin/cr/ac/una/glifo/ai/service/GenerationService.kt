package cr.ac.una.glifo.ai.service

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.LlmResult
import cr.ac.una.glifo.ai.ledger.AiCallContext
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
import cr.ac.una.glifo.ai.ledger.AiCallType
import cr.ac.una.glifo.ai.ledger.CostLedgerService
import cr.ac.una.glifo.ai.prompt.PromptBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

data class TopicInput(
    val id: Long,
    val code: String,
    val title: String,
    val coverageState: String? = null,
    val notesText: String? = null
)

data class GeneratedStudyItem(
    val topicId: Long,
    val kind: String,
    val payload: String
)

data class GeneratedBatch(
    val items: List<GeneratedStudyItem>,
    val llmResult: LlmResult,
    val discardedCount: Int = 0
)

private val VALID_KINDS = setOf("FLASHCARD", "MULTIPLE_CHOICE", "TRUE_FALSE")
private val VALID_DIFFICULTIES = setOf("EASY", "MEDIUM", "HARD")

private val OUTPUT_SCHEMA_EXAMPLE = """
{
  "items": [
    {
      "topicId": 101,
      "kind": "FLASHCARD",
      "front": "Pregunta o concepto en el frente",
      "back": "Respuesta concisa en el reverso",
      "latex": "\\lim_{x \\to a} f(x) = L",
      "hint": "Pista opcional",
      "difficulty": "MEDIUM"
    },
    {
      "topicId": 101,
      "kind": "MULTIPLE_CHOICE",
      "stem": "Enunciado de la pregunta de selección única",
      "options": [
        {"id": "a", "text": "Opción A", "isCorrect": false},
        {"id": "b", "text": "Opción B", "isCorrect": true},
        {"id": "c", "text": "Opción C", "isCorrect": false}
      ],
      "explanation": "Explicación pedagógica de la respuesta correcta",
      "difficulty": "MEDIUM"
    },
    {
      "topicId": 101,
      "kind": "TRUE_FALSE",
      "statement": "Afirmación para evaluar como verdadera o falsa.",
      "answer": true,
      "explanation": "Justificación de por qué es verdadera o falsa.",
      "difficulty": "EASY"
    }
  ]
}
""".trimIndent()

@Service
class GenerationService @Autowired constructor(
    private val llmClient: LlmClient,
    private val jsonMapper: JsonMapper,
    private val costLedgerService: CostLedgerService
) {

    constructor(llmClient: LlmClient, costLedgerService: CostLedgerService) : this(
        llmClient = llmClient,
        jsonMapper = JsonMapper.builder().findAndAddModules().build(),
        costLedgerService = costLedgerService
    )

    private val log = LoggerFactory.getLogger(GenerationService::class.java)

    /**
     * Todos los temas del lote ya tenían ítems y no se llamó a IA-02: queda en
     * ai_avoided_calls a nombre de [origin] (#46). Un lote reutilizado en parte no cuenta,
     * porque la llamada igual se hace.
     */
    fun recordReusedBatch(origin: AiCallOrigin) =
        costLedgerService.avoided(AiCallContext(origin, AiCallType.IA_02, level = null))

    /**
     * IA-02: Generación por lotes de flashcards y preguntas de práctica.
     * Realiza una única llamada a [LlmClient] para todos los temas del lote y la registra en
     * ai_calls a nombre de [origin]. IA-02 no es un nivel de la escalera de percepción, así
     * que el registro va sin nivel (level = NULL).
     */
    fun generateStudyItems(
        courseName: String,
        topics: List<TopicInput>,
        itemsPerTopic: Int,
        origin: AiCallOrigin
    ): GeneratedBatch {
        if (topics.isEmpty()) {
            throw IllegalArgumentException("La lista de temas no puede estar vacía")
        }

        val topicEntries = topics.map { topic ->
            buildString {
                append("topicId=${topic.id} | código=${topic.code} | título=${topic.title}")
                if (!topic.coverageState.isNullOrBlank()) {
                    append(" | cobertura=${topic.coverageState}")
                }
                if (!topic.notesText.isNullOrBlank()) {
                    val formatted = topic.notesText.take(1500).lines().joinToString("\n  ") { it.trim() }.trim()
                    append(" | apuntes_estudiante:\n  $formatted")
                } else {
                    append(" | (sin apuntes del estudiante para este tema)")
                }
            }
        }

        val constraints = listOf(
            "Generá exactamente $itemsPerTopic ítems por cada tema de la lista.",
            "Mezclá los tipos FLASHCARD, MULTIPLE_CHOICE y TRUE_FALSE.",
            "Escribí todo el contenido en español claro y académico.",
            "Usá únicamente los topicId entregados en la lista; no inventés temas fuera del lote.",
            "En MULTIPLE_CHOICE incluí entre 3 y 5 opciones y exactamente una opción con isCorrect=true.",
            "En difficulty usá solo EASY, MEDIUM o HARD.",
            "Para temas con apuntes del estudiante: generá flashcards que refuercen lo que el estudiante escribió (usando sus propias definiciones y fórmulas), y preguntas de quiz (MULTIPLE_CHOICE o TRUE_FALSE) enfocadas en lo que falta o está débil según su cobertura.",
            "Para temas sin apuntes del estudiante: generá material conceptual a partir del título del tema e indicá en la explicación o hint que es material conceptual base."
        )

        val (systemPrompt, userPrompt) = PromptBuilder()
            .withRole("Sos un docente universitario experto creando material de estudio riguroso en español.")
            .withCourse(courseName)
            .withTopics(topicEntries)
            .withConstraints(constraints)
            .withOutputSchema(OUTPUT_SCHEMA_EXAMPLE)
            .build()

        return costLedgerService.track(
            context = AiCallContext(origin, AiCallType.IA_02, level = null),
            call = {
                try {
                    llmClient.completeJson(
                        LlmRequest(
                            systemPrompt = systemPrompt,
                            userPrompt = userPrompt,
                            temperature = 0.4
                        )
                    )
                } catch (ex: Exception) {
                    if (ex is AiProviderException) throw ex
                    throw AiProviderException("Error al generar ítems de estudio con la IA", cause = ex)
                }
            },
            accept = { llmResult -> parseBatch(llmResult, topics, courseName) }
        )
    }

    /** Interpreta y valida la respuesta de IA-02; lanza AiProviderException si no sirve. */
    private fun parseBatch(llmResult: LlmResult, topics: List<TopicInput>, courseName: String): GeneratedBatch {
        val root = try {
            jsonMapper.readTree(llmResult.content)
        } catch (ex: Exception) {
            throw AiProviderException("La IA devolvió un JSON mal formado", cause = ex)
        }

        val itemsNode = root.path("items")
        if (!itemsNode.isArray) {
            throw AiProviderException("El JSON devuelto por la IA no contiene el arreglo 'items'")
        }

        val validTopicIds = topics.map { it.id }.toSet()
        val validItems = mutableListOf<GeneratedStudyItem>()
        var discardedCount = 0

        for (itemNode in itemsNode) {
            val validated = validateAndNormalizeItem(itemNode, validTopicIds)
            if (validated != null) {
                validItems.add(validated)
            } else {
                discardedCount++
            }
        }

        if (discardedCount > 0) {
            log.warn("IA-02 descartó {} ítems inválidos en el lote del curso '{}'", discardedCount, courseName)
        }

        if (validItems.isEmpty()) {
            throw AiProviderException("La IA no produjo ningún ítem de estudio válido para los temas solicitados")
        }

        return GeneratedBatch(
            items = validItems,
            llmResult = llmResult,
            discardedCount = discardedCount
        )
    }

    private fun validateAndNormalizeItem(
        node: JsonNode,
        validTopicIds: Set<Long>
    ): GeneratedStudyItem? {
        if (!node.isObject) return null

        val topicIdNode = node.get("topicId") ?: node.get("topic_id") ?: return null
        if (!topicIdNode.isNumber) return null
        val topicId = topicIdNode.asLong()
        if (topicId !in validTopicIds) return null

        val kind = node.textOrNull("kind")?.uppercase() ?: return null
        if (kind !in VALID_KINDS) return null

        val rawDifficulty = node.textOrNull("difficulty")?.uppercase()
        val difficulty = if (rawDifficulty.isNullOrBlank()) {
            "MEDIUM"
        } else if (rawDifficulty in VALID_DIFFICULTIES) {
            rawDifficulty
        } else {
            return null
        }

        val normalizedMap: Map<String, Any?> = when (kind) {
            "FLASHCARD" -> buildFlashcardPayload(node, difficulty)
            "MULTIPLE_CHOICE" -> buildMultipleChoicePayload(node, difficulty)
            "TRUE_FALSE" -> buildTrueFalsePayload(node, difficulty)
            else -> null
        } ?: return null

        val payloadJson = jsonMapper.writeValueAsString(normalizedMap)
        return GeneratedStudyItem(
            topicId = topicId,
            kind = kind,
            payload = payloadJson
        )
    }

    private fun buildFlashcardPayload(node: JsonNode, difficulty: String): Map<String, Any?>? {
        val front = node.textOrNull("front") ?: return null
        val back = node.textOrNull("back") ?: return null
        val latex = node.textOrNull("latex")
        val hint = node.textOrNull("hint")

        return buildMap {
            put("schemaVersion", 1)
            put("kind", "FLASHCARD")
            put("front", front)
            put("back", back)
            if (latex != null) put("latex", latex)
            if (hint != null) put("hint", hint)
            put("difficulty", difficulty)
            put("sourceNoteIds", emptyList<Long>())
        }
    }

    private fun buildMultipleChoicePayload(node: JsonNode, difficulty: String): Map<String, Any?>? {
        val stem = node.textOrNull("stem") ?: return null
        val explanation = node.textOrNull("explanation") ?: return null
        val optionsNode = node.get("options") ?: return null
        if (!optionsNode.isArray) return null

        val optionCount = optionsNode.size()
        if (optionCount !in 3..5) return null

        val normalizedOptions = mutableListOf<Map<String, Any>>()
        var correctCount = 0

        for (optNode in optionsNode) {
            if (!optNode.isObject) return null
            val optId = optNode.textOrNull("id") ?: return null
            val optText = optNode.textOrNull("text") ?: return null
            val isCorrectNode = optNode.get("isCorrect") ?: optNode.get("is_correct") ?: return null
            if (!isCorrectNode.isBoolean) return null
            val isCorrect = isCorrectNode.asBoolean()
            if (isCorrect) correctCount++

            normalizedOptions.add(
                mapOf(
                    "id" to optId,
                    "text" to optText,
                    "isCorrect" to isCorrect
                )
            )
        }

        if (correctCount != 1) return null

        return mapOf(
            "schemaVersion" to 1,
            "kind" to "MULTIPLE_CHOICE",
            "stem" to stem,
            "options" to normalizedOptions,
            "explanation" to explanation,
            "shuffleOptions" to true,
            "difficulty" to difficulty,
            "sourceNoteIds" to emptyList<Long>()
        )
    }

    private fun buildTrueFalsePayload(node: JsonNode, difficulty: String): Map<String, Any?>? {
        val statement = node.textOrNull("statement") ?: return null
        val explanation = node.textOrNull("explanation") ?: return null
        val answerNode = node.get("answer") ?: return null
        if (!answerNode.isBoolean) return null
        val answer = answerNode.asBoolean()

        return mapOf(
            "schemaVersion" to 1,
            "kind" to "TRUE_FALSE",
            "statement" to statement,
            "answer" to answer,
            "explanation" to explanation,
            "difficulty" to difficulty,
            "sourceNoteIds" to emptyList<Long>()
        )
    }

    private fun JsonNode.textOrNull(fieldName: String): String? {
        val child = this.get(fieldName) ?: return null
        if (child.isNull || !child.isString) return null
        val text = child.asString().trim()
        return text.ifEmpty { null }
    }
}

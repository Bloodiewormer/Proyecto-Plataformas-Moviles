package cr.ac.una.glifo.ai.service

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.prompt.PromptBuilder
import cr.ac.una.glifo.ai.validation.LatexValidator
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.glossary.repository.CourseGlossaryRepository
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.note.entity.NotePage
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.mapper.toResponse
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

data class RegionInput(
    val id: String,
    val pageIndex: Int = 0,
    val bboxX: Double = 0.0,
    val bboxY: Double = 0.0,
    val kind: String = "TEXT",
    val text: String? = null,
    val latex: String? = null,
    val confidence: Double = 0.0,
    val resolvedAt: String = "N1",
    val isUncertain: Boolean = false
)

data class ReconstructionSummary(
    val verified: Int,
    val repaired: Int,
    val escalated: Int,
    val uncertain: Int
)

data class NoteReconstructionOutcome(
    val note: NoteResponse,
    val reconstructed: Boolean
)

private val ALLOWED_BLOCK_TYPES = setOf("HEADING", "PARAGRAPH", "FORMULA", "FIGURE")

private val RECONSTRUCTION_SCHEMA_EXAMPLE = """
{
  "schemaVersion": 1,
  "blocks": [
    {
      "id": "b1",
      "type": "HEADING",
      "level": 2,
      "text": "Límite de una función",
      "regionId": "r1"
    },
    {
      "id": "b2",
      "type": "PARAGRAPH",
      "spans": [
        {
          "text": "Decimos que f tiende a L cuando x tiende a a si ",
          "confidence": 0.96,
          "resolvedAt": "N1",
          "regionId": "r1",
          "isUncertain": false
        },
        {
          "text": "para todo ε existe δ",
          "confidence": 0.74,
          "resolvedAt": "N2",
          "regionId": "r2",
          "isUncertain": false
        }
      ]
    },
    {
      "id": "b3",
      "type": "FORMULA",
      "latex": "\\lim_{x \\to a} f(x) = L",
      "confidence": 0.88,
      "resolvedAt": "N1_5",
      "regionId": "r3",
      "latexCompiles": true,
      "isUncertain": false
    },
    {
      "id": "b4",
      "type": "PARAGRAPH",
      "spans": [
        {
          "text": "[ilegible]",
          "confidence": 0.21,
          "resolvedAt": "UNRESOLVED",
          "regionId": "r4",
          "isUncertain": true
        }
      ]
    }
  ],
  "suggestedTopicId": 101,
  "title": "Límites: definición épsilon-delta"
}
""".trimIndent()

@Service
class ReconstructionService @Autowired constructor(
    private val noteRepository: NoteRepository,
    private val notePageRepository: NotePageRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val courseGlossaryRepository: CourseGlossaryRepository,
    private val llmClient: LlmClient,
    private val latexValidator: LatexValidator,
    private val jsonMapper: JsonMapper
) {

    constructor(
        noteRepository: NoteRepository,
        notePageRepository: NotePageRepository,
        syllabusTopicRepository: SyllabusTopicRepository,
        courseGlossaryRepository: CourseGlossaryRepository,
        llmClient: LlmClient,
        latexValidator: LatexValidator = LatexValidator()
    ) : this(
        noteRepository = noteRepository,
        notePageRepository = notePageRepository,
        syllabusTopicRepository = syllabusTopicRepository,
        courseGlossaryRepository = courseGlossaryRepository,
        llmClient = llmClient,
        latexValidator = latexValidator,
        jsonMapper = JsonMapper.builder().findAndAddModules().build()
    )

    private val log = LoggerFactory.getLogger(ReconstructionService::class.java)

    /**
     * IA-01: Reconstrucción de apunte estructurado a partir de las regiones de todas sus páginas.
     * - Idempotente: si el apunte ya está en READY y tiene content, lo devuelve sin llamar a la IA.
     * - No mantiene una transacción abierta durante la llamada HTTP a OpenAI.
     */
    fun reconstructNote(noteId: Long, userId: Long): NoteReconstructionOutcome {
        val note = noteRepository.findById(noteId)
            .orElseThrow { ResourceNotFoundException("Apunte no encontrado") }
        if (note.user.id != userId) {
            throw ResourceNotFoundException("Apunte no encontrado")
        }

        if (note.status == NoteStatus.READY.name && !note.content.isNullOrBlank()) {
            return NoteReconstructionOutcome(
                note = note.toResponse(includeContent = true),
                reconstructed = false
            )
        }

        val pages = notePageRepository.findByNoteIdOrderByPageIndex(noteId)
            .ifEmpty { note.pages.sortedBy { it.pageIndex } }
        if (pages.isEmpty()) {
            throw ConflictException("El apunte no tiene páginas para reconstruir", "NOTE_HAS_NO_PAGES")
        }

        val orderedRegions = extractOrderedRegions(pages)
        if (orderedRegions.isEmpty()) {
            throw AiProviderException("El apunte no contiene regiones válidas para reconstruir")
        }

        val courseId = note.course.id
        val courseName = runCatching { note.course.name }.getOrDefault("")
        val courseTopics = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
        val glossaryEntries = courseGlossaryRepository.findByCourseIdOrderByTerm(courseId)

        val (systemPrompt, userPrompt) = PromptBuilder()
            .withRole("Sos un asistente académico universitario que reconstruye apuntes manuscritos digitalizados en español a partir de regiones OCR.")
            .withCourse(courseName)
            .withTopics(courseTopics.map { "topicId=${it.id} | código=${it.code} | título=${it.title}" })
            .withGlossary(glossaryEntries.map { "${it.term} -> ${it.canonicalForm} (${it.kind})" })
            .withRegions(orderedRegions.map { formatRegionForPrompt(it) })
            .withConstraints(
                listOf(
                    "Escribí todo el contenido en español respetando fielmente el texto de las regiones.",
                    "No inventés contenido que no esté en las regiones ni corrijás el contenido matemático.",
                    "Usá únicamente los tipos de bloque HEADING, PARAGRAPH, FORMULA o FIGURE.",
                    "Cada span de un PARAGRAPH y cada bloque FORMULA o FIGURE debe incluir el regionId exacto de la región original.",
                    "Si una región tiene isUncertain=true o resolvedAt=UNRESOLVED, conservá su incertidumbre (por ejemplo '[ilegible]') sin inventar lo que falta.",
                    "Si podés identificar a qué tema del sílabo corresponde el apunte, indicá su id numérico en suggestedTopicId (o null si ninguno aplica) y un título breve en title."
                )
            )
            .withOutputSchema(RECONSTRUCTION_SCHEMA_EXAMPLE)
            .build()

        // Llamada HTTP a OpenAI fuera de una transacción de base de datos.
        val llmResult = try {
            val result = llmClient.completeJson(
                LlmRequest(
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = 0.2
                )
            )
            // TODO(#46): Registrar llamada exitosa en CostLedgerService con call_type = 'IA_01',
            // usando result.model, result.inputTokens, result.outputTokens, result.latencyMs y success = true.
            result
        } catch (ex: Exception) {
            // TODO(#46): Registrar llamada fallida en CostLedgerService con call_type = 'IA_01' y success = false.
            if (ex is AiProviderException) throw ex
            throw AiProviderException("Error al reconstruir el apunte con la IA", cause = ex)
        }

        val root = try {
            jsonMapper.readTree(llmResult.content)
        } catch (ex: Exception) {
            throw AiProviderException("La IA devolvió un JSON mal formado al reconstruir el apunte", cause = ex)
        }

        val blocksNode = root.path("blocks")
        if (!blocksNode.isArray) {
            throw AiProviderException("El JSON devuelto por la IA no contiene el arreglo 'blocks'")
        }

        val regionsById = orderedRegions.associateBy { it.id }
        val validBlocks = mutableListOf<Map<String, Any?>>()
        var discardedCount = 0

        for (blockNode in blocksNode) {
            val blockId = "b${validBlocks.size + 1}"
            val normalizedBlock = validateAndNormalizeBlock(blockNode, blockId, regionsById)
            if (normalizedBlock != null) {
                validBlocks.add(normalizedBlock)
            } else {
                discardedCount++
            }
        }

        if (discardedCount > 0) {
            log.warn("IA-01 descartó {} bloques inválidos al reconstruir el apunte {}", discardedCount, noteId)
        }

        if (validBlocks.isEmpty()) {
            throw AiProviderException("La IA no produjo ningún bloque válido para reconstruir el apunte")
        }

        val summary = computeSummary(orderedRegions)
        val contentMap = linkedMapOf<String, Any>(
            "schemaVersion" to 1,
            "blocks" to validBlocks,
            "summary" to linkedMapOf(
                "verified" to summary.verified,
                "repaired" to summary.repaired,
                "escalated" to summary.escalated,
                "uncertain" to summary.uncertain
            )
        )
        val contentJson = jsonMapper.writeValueAsString(contentMap)

        val suggestedTopicId = (root.get("suggestedTopicId") ?: root.get("suggested_topic_id"))
            ?.takeIf { it.isNumber }
            ?.asLong()
        if (note.syllabusTopic == null && suggestedTopicId != null) {
            val matchingTopic = courseTopics.firstOrNull {
                it.id == suggestedTopicId && (it.course == null || it.course?.id == courseId)
            }
            if (matchingTopic != null) {
                note.syllabusTopic = matchingTopic
            }
        }

        val suggestedTitle = root.textOrNull("title")
        if (note.title.isBlank() && !suggestedTitle.isNullOrBlank()) {
            note.title = suggestedTitle.take(200)
        }

        note.content = contentJson
        note.contentGeneratedAt = Instant.now()
        note.status = NoteStatus.READY.name

        val savedNote = noteRepository.save(note) ?: note
        return NoteReconstructionOutcome(
            note = savedNote.toResponse(includeContent = true),
            reconstructed = true
        )
    }

    internal fun extractOrderedRegions(pages: List<NotePage>): List<RegionInput> {
        val extracted = mutableListOf<RegionInput>()
        val usedIds = mutableSetOf<String>()

        for (page in pages.sortedBy { it.pageIndex }) {
            val rawJson = page.regions
            if (rawJson.isNullOrBlank()) continue
            val arrayNode = try {
                jsonMapper.readTree(rawJson)
            } catch (_: Exception) {
                null
            } ?: continue
            if (!arrayNode.isArray) continue

            arrayNode.forEachIndexed { idx, regionNode ->
                if (!regionNode.isObject) return@forEachIndexed
                val baseId = regionNode.textOrNull("id") ?: "r$idx"
                val regionId = if (usedIds.add(baseId)) {
                    baseId
                } else {
                    val disambiguated = "p${page.pageIndex}_$baseId"
                    usedIds.add(disambiguated)
                    disambiguated
                }

                val bboxNode = regionNode.get("bbox")
                val bboxX = if (bboxNode != null && bboxNode.isArray && bboxNode.size() >= 1 && bboxNode[0].isNumber) {
                    bboxNode[0].asDouble()
                } else {
                    0.0
                }
                val bboxY = if (bboxNode != null && bboxNode.isArray && bboxNode.size() >= 2 && bboxNode[1].isNumber) {
                    bboxNode[1].asDouble()
                } else {
                    idx.toDouble()
                }

                val kind = regionNode.textOrNull("kind")?.uppercase() ?: "TEXT"
                val confidence = regionNode.get("confidence")
                    ?.takeIf { it.isNumber }
                    ?.asDouble()
                    ?.coerceIn(0.0, 1.0)
                    ?: 0.0
                val rawUncertain = regionNode.get("isUncertain")
                    ?.takeIf { it.isBoolean }
                    ?.asBoolean()
                    ?: false
                val rawResolvedAt = regionNode.textOrNull("resolvedAt")?.uppercase()
                val resolvedAt = rawResolvedAt ?: if (!rawUncertain && confidence >= 0.75) "N1" else "UNRESOLVED"
                val isUncertain = rawUncertain || resolvedAt == "UNRESOLVED"

                val text = regionNode.textOrNull("text") ?: regionNode.textOrNull("caption")
                val latex = regionNode.textOrNull("latex")

                extracted.add(
                    RegionInput(
                        id = regionId,
                        pageIndex = page.pageIndex,
                        bboxX = bboxX,
                        bboxY = bboxY,
                        kind = kind,
                        text = text,
                        latex = latex,
                        confidence = confidence,
                        resolvedAt = resolvedAt,
                        isUncertain = isUncertain
                    )
                )
            }
        }

        return extracted.sortedWith(compareBy<RegionInput>({ it.pageIndex }, { it.bboxY }, { it.bboxX }))
    }

    /**
     * Calcula el resumen de confianza en el servidor a partir de las regiones originales,
     * usando exactamente el mismo criterio que NoteContentMapper.serverFragment en Android
     * y que el ejemplo canónico de import.sql:
     * - uncertain: isUncertain == true || resolvedAt == "UNRESOLVED"
     * - repaired:  resolvedAt == "N1_5"
     * - escalated: resolvedAt == "N2" || resolvedAt == "N3"
     * - verified:  resto (N1 / N0 con isUncertain == false)
     */
    internal fun computeSummary(regions: List<RegionInput>): ReconstructionSummary {
        var verified = 0
        var repaired = 0
        var escalated = 0
        var uncertain = 0

        for (region in regions) {
            val resolvedAt = region.resolvedAt.uppercase()
            val isUncertain = region.isUncertain || resolvedAt == "UNRESOLVED"
            when {
                isUncertain -> uncertain++
                resolvedAt == "N1_5" -> repaired++
                resolvedAt == "N2" || resolvedAt == "N3" -> escalated++
                else -> verified++
            }
        }

        return ReconstructionSummary(
            verified = verified,
            repaired = repaired,
            escalated = escalated,
            uncertain = uncertain
        )
    }

    private fun validateAndNormalizeBlock(
        node: JsonNode,
        blockId: String,
        regionsById: Map<String, RegionInput>
    ): Map<String, Any?>? {
        if (!node.isObject) return null
        val type = node.textOrNull("type")?.uppercase() ?: return null
        if (type !in ALLOWED_BLOCK_TYPES) return null

        return when (type) {
            "HEADING" -> buildHeadingBlock(node, blockId, regionsById)
            "PARAGRAPH" -> buildParagraphBlock(node, blockId, regionsById)
            "FORMULA" -> buildFormulaBlock(node, blockId, regionsById)
            "FIGURE" -> buildFigureBlock(node, blockId, regionsById)
            else -> null
        }
    }

    private fun buildHeadingBlock(
        node: JsonNode,
        blockId: String,
        regionsById: Map<String, RegionInput>
    ): Map<String, Any?>? {
        val text = node.textOrNull("text") ?: return null
        val level = node.get("level")?.takeIf { it.isNumber }?.asInt()?.coerceIn(1, 4) ?: 2
        val regionId = node.textOrNull("regionId") ?: node.textOrNull("region_id")
        if (regionId != null && regionId !in regionsById) {
            return null
        }

        return buildMap {
            put("id", blockId)
            put("type", "HEADING")
            put("level", level)
            put("text", text)
            if (regionId != null) {
                val orig = regionsById.getValue(regionId)
                val origUncertain = orig.isUncertain || orig.resolvedAt.equals("UNRESOLVED", ignoreCase = true)
                put("regionId", orig.id)
                put("confidence", orig.confidence)
                put("resolvedAt", orig.resolvedAt)
                put("isUncertain", origUncertain)
            }
        }
    }

    private fun buildParagraphBlock(
        node: JsonNode,
        blockId: String,
        regionsById: Map<String, RegionInput>
    ): Map<String, Any?>? {
        val spansNode = node.get("spans") ?: return null
        if (!spansNode.isArray) return null

        val validSpans = mutableListOf<Map<String, Any?>>()
        for (spanNode in spansNode) {
            if (!spanNode.isObject) continue
            val regionId = spanNode.textOrNull("regionId") ?: spanNode.textOrNull("region_id") ?: continue
            val orig = regionsById[regionId] ?: continue
            val origUncertain = orig.isUncertain || orig.resolvedAt.equals("UNRESOLVED", ignoreCase = true)

            val spanText = spanNode.textOrNull("text")
                ?: orig.text?.trim()?.ifEmpty { null }
                ?: if (origUncertain) "[ilegible]" else null
            if (spanText == null) continue

            validSpans.add(
                linkedMapOf(
                    "text" to spanText,
                    "confidence" to orig.confidence,
                    "resolvedAt" to orig.resolvedAt,
                    "regionId" to orig.id,
                    "isUncertain" to origUncertain
                )
            )
        }

        if (validSpans.isEmpty()) return null

        return linkedMapOf(
            "id" to blockId,
            "type" to "PARAGRAPH",
            "spans" to validSpans
        )
    }

    private fun buildFormulaBlock(
        node: JsonNode,
        blockId: String,
        regionsById: Map<String, RegionInput>
    ): Map<String, Any?>? {
        val regionId = node.textOrNull("regionId") ?: node.textOrNull("region_id") ?: return null
        val orig = regionsById[regionId] ?: return null
        val origUncertain = orig.isUncertain || orig.resolvedAt.equals("UNRESOLVED", ignoreCase = true)

        val latex = node.textOrNull("latex")
            ?: orig.latex?.trim()?.ifEmpty { null }
            ?: orig.text?.trim()?.ifEmpty { null }
            ?: return null

        val compiles = latexValidator.compiles(latex)

        return linkedMapOf(
            "id" to blockId,
            "type" to "FORMULA",
            "latex" to latex,
            "confidence" to orig.confidence,
            "resolvedAt" to orig.resolvedAt,
            "regionId" to orig.id,
            "latexCompiles" to compiles,
            "isUncertain" to origUncertain
        )
    }

    private fun buildFigureBlock(
        node: JsonNode,
        blockId: String,
        regionsById: Map<String, RegionInput>
    ): Map<String, Any?>? {
        val regionId = node.textOrNull("regionId") ?: node.textOrNull("region_id") ?: return null
        val orig = regionsById[regionId] ?: return null
        val origUncertain = orig.isUncertain || orig.resolvedAt.equals("UNRESOLVED", ignoreCase = true)

        val caption = node.textOrNull("caption")
            ?: node.textOrNull("text")
            ?: orig.text?.trim()?.ifEmpty { null }
            ?: if (origUncertain) "[ilegible]" else return null

        return linkedMapOf(
            "id" to blockId,
            "type" to "FIGURE",
            "caption" to caption,
            "confidence" to orig.confidence,
            "resolvedAt" to orig.resolvedAt,
            "regionId" to orig.id,
            "isUncertain" to origUncertain
        )
    }

    private fun formatRegionForPrompt(region: RegionInput): String {
        val isUncertain = region.isUncertain || region.resolvedAt.equals("UNRESOLVED", ignoreCase = true)
        val contentValue = if (region.kind.equals("MATH", ignoreCase = true)) {
            "latex=${region.latex ?: region.text ?: ""}"
        } else {
            "text=${region.text ?: region.latex ?: ""}"
        }
        return "pageIndex=${region.pageIndex} | bboxY=${region.bboxY} | regionId=${region.id} | kind=${region.kind} | resolvedAt=${region.resolvedAt} | confidence=${region.confidence} | isUncertain=$isUncertain | $contentValue"
    }

    private fun JsonNode.textOrNull(fieldName: String): String? {
        val child = this.get(fieldName) ?: return null
        if (child.isNull || !child.isString) return null
        val text = child.asString().trim()
        return text.ifEmpty { null }
    }
}

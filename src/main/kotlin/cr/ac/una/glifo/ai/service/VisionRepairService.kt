package cr.ac.una.glifo.ai.service

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.LlmResult
import cr.ac.una.glifo.ai.TokenLogprob
import cr.ac.una.glifo.ai.config.OpenAiProperties
import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.engine.stripLatexDelimiters
import cr.ac.una.glifo.ai.ledger.AiCallContext
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
import cr.ac.una.glifo.ai.ledger.AiCallType
import cr.ac.una.glifo.ai.ledger.CostLedgerService
import cr.ac.una.glifo.ai.validation.FormulaEscalationPolicy
import cr.ac.una.glifo.ai.validation.LatexValidator
import cr.ac.una.glifo.common.exception.BadRequestException
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import java.util.Base64
import kotlin.math.exp

data class RepairedRegion(
    val regionId: String,
    val kind: String,
    /** Texto, LaTeX o descripción vigente de la región; null si sigue sin resolver. */
    val value: String?,
    val confidence: Float,
    val resolvedAt: String,
    @get:JsonProperty("isUncertain")
    val isUncertain: Boolean
)

data class PageRepairResponse(
    val pageIndex: Int,
    val levelReached: String,
    /** Regiones que esta llamada resolvió en N2. */
    val repaired: Int,
    val uncertain: Int,
    val regions: List<RepairedRegion>
)

internal data class RepairLine(
    val regionId: String,
    val value: String,
    val confidence: Float,
    val detectedKind: String? = null
)

private const val ILLEGIBLE = "ILEGIBLE"
private const val ENGINE = "VisionRepair"
private const val ATTEMPTS_FIELD = "visionRepairAttempts"
private const val TEXT_CONFIDENCE_GATE = 0.55f
private const val DRAWING_CONFIDENCE_GATE = 0.35f
private const val UNCERTAIN_SAVE_FLOOR = 0.40f

private val ALLOWED_KINDS = setOf("TEXT", "MATH", "DRAWING")
private val LATEX_COMMAND_HINT = Regex("""\\(?:frac|int|iint|lim|sum|prod|sqrt|sin|cos|tan|arctan|sec|csc|cot|ln|log|infty|cdot|left|right|pi|theta|alpha|beta)(?![a-zA-Z])""")
private val LEADING_LIST_MARKER = Regex("""^(?:[-*•]\s+|\d+[.)]\s+)""")

private val SYSTEM_PROMPT = """
Sos un motor de reparación de apuntes manuscritos universitarios en español. Recibís varios recortes de una misma página, cada uno con su regionId y una pista de tipo del clasificador local.
Respondé exactamente una línea por recorte, en el mismo orden, con el formato:
regionId|TIPO|valor
donde TIPO es el contenido REAL del recorte: TEXT, MATH o DRAWING. La pista de tipo recibida puede estar equivocada (por ejemplo, recortes marcados como DRAWING que en realidad son fórmulas matemáticas o texto): corregí TIPO según lo que ves.
- MATH: solo el LaTeX de la fórmula en una sola línea, sin delimitadores ($, $$, \(, \[) ni bloques de código. Distinguí \infty, +\infty y -\infty del dígito 2; conservá límites en \lim e \int, corchetes de evaluación, diferenciales (\,dx, \,dt) y '=' inicial si aparece. Normalizá trigonometría en español a LaTeX estándar para JLaTeXMath: sen -> \sin, tg -> \tan, arctg -> \arctan, ctg -> \cot, cosec -> \csc, sec -> \sec.
- TEXT: el texto manuscrito en español exactamente como está escrito, en una sola línea (incluyendo prefijos como a), b), i) si aparecen).
- DRAWING: solo para dibujos, gráficas, curvas o diagramas reales (no fórmulas): una descripción breve y objetiva en español, en una sola línea.
Transcribí lo que se ve: no completes ni inventes. Usá $ILLEGIBLE únicamente si el recorte es completamente imposible de leer.
""".trimIndent()

/**
 * IA-00, N2 (issue #41): repara por visión las regiones sin resolver de una página, todas en
 * una sola llamada. La confianza de cada región sale de los logprobs de sus propios tokens.
 * Permite reclasificar regiones mal etiquetadas (por ejemplo DRAWING con fórmulas) y conserva
 * descripciones breves para dibujos reales.
 */
@Service
class VisionRepairService(
    private val store: PageRegionStore,
    private val llmClient: LlmClient,
    private val properties: OpenAiProperties,
    private val latexValidator: LatexValidator,
    private val costLedgerService: CostLedgerService
) {

    fun repairPage(noteId: Long, userId: Long, pageIndex: Int, crops: Map<String, RegionCrop>): PageRepairResponse {
        val page = store.ownedPage(noteId, userId, pageIndex)
        if (crops.isEmpty()) throw BadRequestException("Enviá al menos un recorte")
        if (crops.size > MAX_CROPS) throw BadRequestException("Se pueden reparar como máximo $MAX_CROPS regiones por llamada")
        val pageRegions = store.regions(page.regions)
        val unresolved = crops.keys.associateWith { store.uniqueRegion(pageRegions, it) }
            .filterValues { it != null && isPending(it) }
            .mapValues { it.value!! }
        val notPending = crops.keys - unresolved.keys
        val context = AiCallContext(AiCallOrigin(userId, page.note.course.id), AiCallType.IA_00, level = "N2")
        if (unresolved.isEmpty() && notPending.all { store.uniqueRegion(pageRegions, it) != null }) {
            // Todas las regiones pedidas ya estaban resueltas: la página no se envía a N2.
            costLedgerService.avoided(context)
            return PageRepairResponse(
                pageIndex = pageIndex,
                levelReached = page.levelReached,
                repaired = 0,
                uncertain = 0,
                regions = crops.keys.map { id -> currentState(store.uniqueRegion(pageRegions, id)!!, id) }
            )
        }
        if (notPending.isNotEmpty()) {
            throw BadRequestException("Estas regiones no existen o no están pendientes de reparación: ${notPending.sorted().joinToString()}")
        }
        unresolved.values.forEach { store.requireAttemptsLeft(it, ATTEMPTS_FIELD) }
        crops.values.forEach { it.requireValidImage() }
        if (properties.apiKey.isBlank()) {
            throw AiProviderException.notConfigured("La reparación por visión no está configurada en el servidor")
        }

        val ordered = crops.keys.toList()
        val kinds = ordered.associateWith { unresolved.getValue(it).path("kind").asString("TEXT").uppercase() }
        // Una fila de ai_calls (#46) por página: es una sola llamada con todos los recortes. Si
        // ninguna región supera la compuerta, el contenido queda registrado como no aceptado.
        val outcomes = costLedgerService.track(
            context = context,
            call = {
                llmClient.completeJson(
                    LlmRequest(
                        systemPrompt = SYSTEM_PROMPT,
                        userPrompt = ordered.mapIndexed { i, id -> "Imagen ${i + 1}: regionId=$id, tipo=${kinds.getValue(id)}" }
                            .joinToString("\n"),
                        images = ordered.map { id ->
                            val crop = crops.getValue(id)
                            "data:${crop.normalizedMediaType()};base64," + Base64.getEncoder().encodeToString(crop.bytes)
                        },
                        temperature = 0.0,
                        maxOutputTokens = 150 * ordered.size,
                        jsonResponse = false,
                        logprobs = true,
                        imageDetail = "low"
                    )
                )
            },
            accept = { result -> judgeLines(result, ordered, kinds) },
            rejection = { outcomes ->
                if (outcomes.any { it.resolvedAt == "N2" }) null else "Ninguna región superó la compuerta de N2"
            }
        )

        return store.update(noteId, pageIndex) { regions, freshPage ->
            var written = 0
            var contentUpdated = false
            val final = outcomes.map { outcome ->
                val node = store.uniqueRegion(regions, outcome.regionId)
                    ?: return@map outcome.copy(value = null, resolvedAt = "UNRESOLVED", isUncertain = true)
                if (!isPending(node)) return@map currentState(node, outcome.regionId)
                node.put(ATTEMPTS_FIELD, store.attempts(node, ATTEMPTS_FIELD) + 1)
                if (outcome.value != null) {
                    node.put("kind", outcome.kind)
                    when (outcome.kind) {
                        "MATH" -> {
                            node.put("latex", outcome.value)
                            node.put("latexCompiles", true)
                        }
                        "DRAWING" -> node.put("caption", outcome.value)
                        else -> node.put("text", outcome.value)
                    }
                    node.put("confidence", outcome.confidence.toDouble())
                    node.put("resolvedAt", outcome.resolvedAt)
                    node.put("isUncertain", outcome.isUncertain)
                    node.put("engine", ENGINE)
                    contentUpdated = true
                    if (!outcome.isUncertain && outcome.resolvedAt == "N2") {
                        written++
                    }
                }
                outcome
            }
            if (written > 0) PageRegionStore.raiseLevel(freshPage, "N2")
            PageRepairResponse(
                pageIndex = pageIndex,
                levelReached = freshPage.levelReached,
                repaired = written,
                uncertain = final.count { it.isUncertain },
                regions = final
            ) to contentUpdated
        }
    }

    /** Decide cada región con su línea de la respuesta, permitiendo reclasificar pistas erróneas. */
    private fun judgeLines(result: LlmResult, ordered: List<String>, kinds: Map<String, String>): List<RepairedRegion> {
        val truncated = result.finishReason == "length"
        val lines = parseLines(result.content, result.tokenLogprobs).groupBy { it.regionId }.mapValues { it.value.first() }
        return ordered.map { id ->
            val hintedKind = kinds.getValue(id)
            val line = lines[id]
            val rawValue = line?.value?.trim().orEmpty()
            var effectiveKind = line?.detectedKind ?: hintedKind

            if (effectiveKind == "DRAWING" && rawValue.isNotEmpty() && !rawValue.equals(ILLEGIBLE, ignoreCase = true)) {
                val candidateLatex = stripLatexDelimiters(rawValue)
                if (LATEX_COMMAND_HINT.containsMatchIn(candidateLatex) && latexValidator.compiles(candidateLatex)) {
                    effectiveKind = "MATH"
                }
            }

            val value = if (effectiveKind == "MATH") stripLatexDelimiters(rawValue) else rawValue
            val legible = value.isNotEmpty() && !value.equals(ILLEGIBLE, ignoreCase = true)
            val confidence = line?.confidence ?: 0f
            val mathCompiles = effectiveKind != "MATH" || latexValidator.compiles(value)
            val gate = when (effectiveKind) {
                "MATH" -> FormulaEscalationPolicy.CONFIDENCE_GATE
                "DRAWING" -> DRAWING_CONFIDENCE_GATE
                else -> TEXT_CONFIDENCE_GATE
            }
            val accepted = legible && !truncated && mathCompiles && confidence >= gate
            // Si el modelo leyó contenido válido pero con confianza intermedia (>= 0.40), se conserva
            // en la región como incierto en lugar de perderse bajo [ilegible] (N3 fue eliminado).
            val saveAsUncertain = !accepted && legible && !truncated && mathCompiles && confidence >= UNCERTAIN_SAVE_FLOOR
            RepairedRegion(
                regionId = id,
                kind = effectiveKind,
                value = if (accepted || saveAsUncertain) value else null,
                confidence = confidence,
                resolvedAt = if (accepted) "N2" else "UNRESOLVED",
                isUncertain = !accepted
            )
        }
    }

    private fun isPending(region: JsonNode) = region.path("resolvedAt").asString("").uppercase() == "UNRESOLVED"

    private fun currentState(node: JsonNode, regionId: String): RepairedRegion {
        val kind = node.path("kind").asString("TEXT").uppercase()
        val field = when (kind) {
            "MATH" -> "latex"
            "DRAWING" -> "caption"
            else -> "text"
        }
        return RepairedRegion(
            regionId = regionId,
            kind = kind,
            value = node.get(field)?.takeIf { it.isString }?.asString(),
            confidence = node.path("confidence").asDouble(0.0).toFloat(),
            resolvedAt = node.path("resolvedAt").asString(""),
            isUncertain = node.path("isUncertain").asBoolean(false)
        )
    }

    companion object {
        const val MAX_CROPS = 24

        /**
         * Separa la salida en líneas `regionId|valor` o `regionId|TIPO|valor`. Si hay logprobs,
         * el texto se arma con los bytes UTF-8 de los tokens (un carácter puede quedar repartido
         * entre dos) y la confianza de cada línea es exp(media) de los tokens que caen dentro de
         * su valor; sin logprobs es 0.
         */
        internal fun parseLines(content: String, tokens: List<TokenLogprob>): List<RepairLine> {
            val tokenBytes = tokens.map { it.utf8() }
            val text = if (tokens.isNotEmpty()) {
                String(tokenBytes.fold(ByteArray(0)) { acc, bytes -> acc + bytes }, Charsets.UTF_8)
            } else {
                content
            }
            val starts = IntArray(tokens.size)
            var offset = 0
            tokenBytes.forEachIndexed { i, bytes -> starts[i] = offset; offset += bytes.size }
            fun byteOffset(charIndex: Int) = text.substring(0, charIndex).toByteArray(Charsets.UTF_8).size

            val lines = mutableListOf<RepairLine>()
            var lineStart = 0
            for (rawLine in text.split('\n')) {
                val separator = rawLine.indexOf('|')
                if (separator > 0) {
                    val regionId = rawLine.substring(0, separator)
                        .trim()
                        .replace(LEADING_LIST_MARKER, "")
                        .trim()
                    val afterFirst = rawLine.substring(separator + 1)
                    val secondSep = afterFirst.indexOf('|')
                    val candidateKind = if (secondSep > 0) afterFirst.substring(0, secondSep).trim().uppercase() else null
                    val (detectedKind, rawValue, valueOffsetInLine) = if (candidateKind in ALLOWED_KINDS) {
                        Triple(candidateKind, afterFirst.substring(secondSep + 1), separator + 1 + secondSep + 1)
                    } else {
                        Triple(null, afterFirst, separator + 1)
                    }
                    val value = rawValue.trim()
                    val valueStartChar = lineStart + valueOffsetInLine + (rawValue.length - rawValue.trimStart().length)
                    val valueStart = byteOffset(valueStartChar)
                    val valueEnd = byteOffset(valueStartChar + value.length)
                    val inside = tokens.indices.filter { i ->
                        val end = starts[i] + tokenBytes[i].size
                        end > valueStart && starts[i] < valueEnd
                    }
                    val confidence = if (inside.isEmpty() || value.isEmpty()) 0f else
                        exp(inside.map { tokens[it].logprob }.average()).toFloat().coerceIn(0f, 1f)
                    if (regionId.isNotEmpty()) lines.add(RepairLine(regionId, value, confidence, detectedKind))
                }
                lineStart += rawLine.length + 1
            }
            return lines
        }
    }
}

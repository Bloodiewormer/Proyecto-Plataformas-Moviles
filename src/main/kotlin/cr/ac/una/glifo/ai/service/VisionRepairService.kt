package cr.ac.una.glifo.ai.service

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.TokenLogprob
import cr.ac.una.glifo.ai.config.OpenAiProperties
import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.engine.stripLatexDelimiters
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

internal data class RepairLine(val regionId: String, val value: String, val confidence: Float)

private const val ILLEGIBLE = "ILEGIBLE"
private const val ENGINE = "VisionRepair"
private const val ATTEMPTS_FIELD = "visionRepairAttempts"

private val SYSTEM_PROMPT = """
Sos un motor de reparación de apuntes manuscritos universitarios en español. Recibís varios recortes de una misma página, cada uno con su regionId y su tipo.
Respondé una línea por recorte, en el mismo orden, con el formato exacto regionId|valor y nada más.
- Tipo TEXT: el texto exactamente como está escrito, en una sola línea.
- Tipo MATH: solo el LaTeX de la fórmula, en una sola línea, sin delimitadores.
- Tipo DRAWING: una descripción breve y objetiva del dibujo, en español.
Transcribí lo que se ve: no corrijas, no completes ni inventes. Si un recorte no se puede leer, el valor es exactamente $ILLEGIBLE.
""".trimIndent()

/**
 * IA-00, N2 (issue #41): repara por visión las regiones sin resolver de una página, todas en
 * una sola llamada. La confianza de cada región sale de los logprobs de sus propios tokens.
 * Lo que no supera la compuerta sigue sin resolver y no se escribe: no se infiere.
 */
@Service
class VisionRepairService(
    private val store: PageRegionStore,
    private val llmClient: LlmClient,
    private val properties: OpenAiProperties,
    private val latexValidator: LatexValidator
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
        if (unresolved.isEmpty() && notPending.all { store.uniqueRegion(pageRegions, it) != null }) {
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
        val result = llmClient.completeJson(
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
        val truncated = result.finishReason == "length"
        val lines = parseLines(result.content, result.tokenLogprobs).groupBy { it.regionId }.mapValues { it.value.first() }

        val outcomes = ordered.map { id ->
            val kind = kinds.getValue(id)
            val line = lines[id]
            val value = line?.value?.let { if (kind == "MATH") stripLatexDelimiters(it) else it }.orEmpty()
            val legible = value.isNotEmpty() && !value.equals(ILLEGIBLE, ignoreCase = true)
            val accepted = legible && !truncated && line!!.confidence >= FormulaEscalationPolicy.CONFIDENCE_GATE &&
                (kind != "MATH" || latexValidator.compiles(value))
            RepairedRegion(
                regionId = id,
                kind = kind,
                value = if (accepted) value else null,
                confidence = line?.confidence ?: 0f,
                resolvedAt = if (accepted) "N2" else "UNRESOLVED",
                isUncertain = !accepted
            )
        }

        return store.update(noteId, pageIndex) { regions, freshPage ->
            var written = 0
            val final = outcomes.map { outcome ->
                val node = store.uniqueRegion(regions, outcome.regionId)
                    ?: return@map outcome.copy(value = null, resolvedAt = "UNRESOLVED", isUncertain = true)
                if (!isPending(node)) return@map currentState(node, outcome.regionId)
                node.put(ATTEMPTS_FIELD, store.attempts(node, ATTEMPTS_FIELD) + 1)
                if (outcome.value != null) {
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
                    node.put("isUncertain", false)
                    node.put("engine", ENGINE)
                    written++
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
            ) to (written > 0)
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
        const val MAX_CROPS = 8

        /**
         * Separa la salida en líneas regionId|valor. Si hay logprobs, el texto se arma con los bytes
         * UTF-8 de los tokens (un carácter puede quedar repartido entre dos) y la confianza de cada
         * línea es exp(media) de los tokens que caen dentro de su valor; sin logprobs es 0.
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
                    val regionId = rawLine.substring(0, separator).trim()
                    val rawValue = rawLine.substring(separator + 1)
                    val value = rawValue.trim()
                    val valueStartChar = lineStart + separator + 1 + (rawValue.length - rawValue.trimStart().length)
                    val valueStart = byteOffset(valueStartChar)
                    val valueEnd = byteOffset(valueStartChar + value.length)
                    val inside = tokens.indices.filter { i ->
                        val end = starts[i] + tokenBytes[i].size
                        end > valueStart && starts[i] < valueEnd
                    }
                    val confidence = if (inside.isEmpty() || value.isEmpty()) 0f else
                        exp(inside.map { tokens[it].logprob }.average()).toFloat().coerceIn(0f, 1f)
                    if (regionId.isNotEmpty()) lines.add(RepairLine(regionId, value, confidence))
                }
                lineStart += rawLine.length + 1
            }
            return lines
        }
    }
}

package cr.ac.una.glifo.ai.service

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.engine.MathOcrEngine
import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.validation.FormulaEscalationPolicy
import cr.ac.una.glifo.ai.validation.FormulaStatus
import cr.ac.una.glifo.ai.validation.MathTranscription
import cr.ac.una.glifo.ai.validation.mathRegions
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

data class MathOcrResponse(
    val pageIndex: Int,
    val regionId: String,
    val latex: String,
    val confidence: Float,
    val latexCompiles: Boolean,
    val status: String,
    val resolvedAt: String,
    val escalationReasons: List<String>,
    val engine: String?,
    /** true: se devolvió el resultado ya guardado, sin llamar al motor. */
    val cached: Boolean
)

private const val ATTEMPTS_FIELD = "mathOcrAttempts"

/**
 * N1.5 (issue #38): transcribe a LaTeX el recorte de una región MATH de una página propia,
 * aplica la compuerta de #39 y guarda el resultado en la región, donde IA-01 lo lee.
 * La llamada al motor ocurre fuera de cualquier transacción de base de datos.
 */
@Service
class MathOcrService(
    private val store: PageRegionStore,
    private val engine: MathOcrEngine,
    private val policy: FormulaEscalationPolicy
) {

    fun transcribeRegion(noteId: Long, userId: Long, pageIndex: Int, regionId: String, crop: RegionCrop): MathOcrResponse {
        val page = store.ownedPage(noteId, userId, pageIndex)
        val node = mathNode(store.regions(page.regions), regionId)
        val region = page.mathRegions().firstOrNull { it.regionId == regionId }
            ?: throw ResourceNotFoundException("Región MATH no encontrada")

        storedResult(node, pageIndex, regionId)?.let { return it }
        requirePending(node)
        store.requireAttemptsLeft(node, ATTEMPTS_FIELD)
        crop.requireValidImage()
        if (!engine.isAvailable()) {
            throw AiProviderException.notConfigured("El OCR matemático no está configurado en el servidor")
        }

        val result = engine.recognize(crop)
        val decision = policy.decide(region, MathTranscription(result.latex, result.confidence))
        val response = MathOcrResponse(
            pageIndex = pageIndex,
            regionId = regionId,
            latex = result.latex,
            confidence = result.confidence,
            latexCompiles = decision.latexCompiles == true,
            status = decision.status.name,
            resolvedAt = decision.status.resolvedAt,
            escalationReasons = decision.escalationReasons.map { it.name }.sorted(),
            engine = result.engine,
            cached = false
        )

        return store.update(noteId, pageIndex) { regions, freshPage ->
            val current = mathNode(regions, regionId)
            storedResult(current, pageIndex, regionId)?.let { return@update it to false }
            requirePending(current)
            current.put(ATTEMPTS_FIELD, store.attempts(current, ATTEMPTS_FIELD) + 1)
            val accepted = response.status == FormulaStatus.KEEP_N1_5.name
            if (accepted) {
                current.put("latex", response.latex)
                current.put("confidence", response.confidence.toDouble())
                current.put("resolvedAt", response.resolvedAt)
                current.put("isUncertain", false)
                current.put("latexCompiles", true)
                current.put("engine", response.engine)
                PageRegionStore.raiseLevel(freshPage, "N1_5")
            }
            response to accepted
        }
    }

    private fun requirePending(node: JsonNode) {
        if (node.path("resolvedAt").asString("").uppercase() != "UNRESOLVED") {
            throw ConflictException("La región ya fue resuelta en otro nivel", "REGION_ALREADY_RESOLVED")
        }
    }

    private fun mathNode(regions: List<ObjectNode>, regionId: String) =
        store.uniqueRegion(regions, regionId)
            ?.takeIf { it.path("kind").asString("") == "MATH" }
            ?: throw ResourceNotFoundException("Región MATH no encontrada")

    private fun storedResult(node: JsonNode, pageIndex: Int, regionId: String): MathOcrResponse? {
        if (node.path("resolvedAt").asString("") != FormulaStatus.KEEP_N1_5.resolvedAt) return null
        val latex = node.path("latex").asString("").trim()
        if (latex.isEmpty()) return null
        return MathOcrResponse(
            pageIndex = pageIndex,
            regionId = regionId,
            latex = latex,
            confidence = node.path("confidence").asDouble(0.0).toFloat(),
            latexCompiles = node.path("latexCompiles").asBoolean(true),
            status = FormulaStatus.KEEP_N1_5.name,
            resolvedAt = FormulaStatus.KEEP_N1_5.resolvedAt,
            escalationReasons = emptyList(),
            engine = node.path("engine").takeIf { it.isString }?.asString(),
            cached = true
        )
    }
}

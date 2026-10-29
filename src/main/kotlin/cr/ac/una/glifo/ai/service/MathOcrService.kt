package cr.ac.una.glifo.ai.service

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.engine.MathOcrEngine
import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.validation.FormulaEscalationPolicy
import cr.ac.una.glifo.ai.validation.FormulaStatus
import cr.ac.una.glifo.ai.validation.MathTranscription
import cr.ac.una.glifo.ai.validation.mathRegions
import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
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

private val ALLOWED_MEDIA_TYPES = setOf("image/png", "image/jpeg", "image/webp")

/**
 * N1.5 (issue #38): transcribe a LaTeX el recorte de una región MATH de una página propia,
 * aplica la compuerta de #39 y guarda el resultado en la región, donde IA-01 lo lee.
 * La llamada al motor ocurre fuera de cualquier transacción de base de datos.
 */
@Service
class MathOcrService(
    private val noteRepository: NoteRepository,
    private val notePageRepository: NotePageRepository,
    private val engine: MathOcrEngine,
    private val policy: FormulaEscalationPolicy,
    private val jsonMapper: JsonMapper
) {

    fun transcribeRegion(noteId: Long, userId: Long, pageIndex: Int, regionId: String, crop: RegionCrop): MathOcrResponse {
        val note = noteRepository.findById(noteId).orElseThrow { ResourceNotFoundException("Apunte no encontrado") }
        if (note.user.id != userId) throw ResourceNotFoundException("Apunte no encontrado")
        val page = notePageRepository.findByNoteIdAndPageIndex(noteId, pageIndex)
            ?: throw ResourceNotFoundException("Página no encontrada")
        val region = page.mathRegions().firstOrNull { it.regionId == regionId }
            ?: throw ResourceNotFoundException("Región MATH no encontrada")

        storedResult(page.regions, pageIndex, regionId)?.let { return it }

        validateCrop(crop)
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

        val freshPage = notePageRepository.findByNoteIdAndPageIndex(noteId, pageIndex)
            ?: throw ResourceNotFoundException("Página no encontrada")
        freshPage.regions = updatedRegions(freshPage.regions, regionId, response)
        notePageRepository.save(freshPage)
        return response
    }

    private fun validateCrop(crop: RegionCrop) {
        if (crop.bytes.isEmpty()) throw BadRequestException("El recorte está vacío")
        val mediaType = crop.mediaType.substringBefore(';').trim().lowercase()
        if (mediaType !in ALLOWED_MEDIA_TYPES || !matchesSignature(mediaType, crop.bytes)) {
            throw BadRequestException("El recorte debe ser una imagen PNG, JPEG o WEBP")
        }
    }

    private fun matchesSignature(mediaType: String, bytes: ByteArray): Boolean {
        fun startsWith(vararg signature: Int) =
            bytes.size >= signature.size && signature.indices.all { bytes[it] == signature[it].toByte() }
        return when (mediaType) {
            "image/png" -> startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            "image/jpeg" -> startsWith(0xFF, 0xD8, 0xFF)
            "image/webp" -> bytes.size >= 12 && startsWith(0x52, 0x49, 0x46, 0x46) &&
                String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
            else -> false
        }
    }

    private fun storedResult(regionsJson: String?, pageIndex: Int, regionId: String): MathOcrResponse? {
        val node = regionNode(regionsJson, regionId) ?: return null
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

    private fun regionNode(regionsJson: String?, regionId: String): ObjectNode? {
        val root = jsonMapper.readTree(regionsJson ?: return null)
        if (!root.isArray) return null
        return root.firstOrNull { it.isObject && it.path("id").asString("") == regionId } as ObjectNode?
    }

    private fun updatedRegions(regionsJson: String?, regionId: String, response: MathOcrResponse): String {
        val root = jsonMapper.readTree(regionsJson ?: "[]")
        val node = (if (root.isArray) root.firstOrNull { it.isObject && it.path("id").asString("") == regionId } else null) as ObjectNode?
            ?: throw ResourceNotFoundException("Región MATH no encontrada")
        node.put("latex", response.latex)
        node.put("confidence", response.confidence.toDouble())
        node.put("resolvedAt", response.resolvedAt)
        node.put("isUncertain", response.status != FormulaStatus.KEEP_N1_5.name)
        node.put("latexCompiles", response.latexCompiles)
        node.put("engine", response.engine)
        return jsonMapper.writeValueAsString(root)
    }
}

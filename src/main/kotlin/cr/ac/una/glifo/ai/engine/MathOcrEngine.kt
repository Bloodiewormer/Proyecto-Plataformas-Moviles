package cr.ac.una.glifo.ai.engine

import cr.ac.una.glifo.common.exception.BadRequestException

private val ALLOWED_MEDIA_TYPES = setOf("image/png", "image/jpeg", "image/webp")

/** Recorte de una región tal como lo envía la app: bytes de la imagen y su tipo MIME. */
class RegionCrop(
    val bytes: ByteArray,
    val mediaType: String
) {
    /** Rechaza recortes vacíos o que no sean realmente PNG, JPEG o WEBP (tipo declarado y firma). */
    fun normalizedMediaType(): String = mediaType.substringBefore(';').trim().lowercase()

    fun requireValidImage() {
        if (bytes.isEmpty()) throw BadRequestException("El recorte está vacío")
        val type = normalizedMediaType()
        if (type !in ALLOWED_MEDIA_TYPES || !matchesSignature(type)) {
            throw BadRequestException("El recorte debe ser una imagen PNG, JPEG o WEBP")
        }
    }

    private fun matchesSignature(type: String): Boolean {
        fun startsWith(vararg signature: Int) =
            bytes.size >= signature.size && signature.indices.all { bytes[it] == signature[it].toByte() }
        return when (type) {
            "image/png" -> startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            "image/jpeg" -> startsWith(0xFF, 0xD8, 0xFF)
            "image/webp" -> bytes.size >= 12 && startsWith(0x52, 0x49, 0x46, 0x46) &&
                String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
            else -> false
        }
    }
}

/**
 * Resultado de N1.5 para un recorte.
 * - [latex]: LaTeX sin delimitadores; vacío si el motor no encontró matemática legible.
 * - [confidence]: confianza derivada del modelo en 0..1, no autodeclarada.
 */
data class MathResult(
    val latex: String,
    val confidence: Float,
    val engine: String,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val latencyMs: Long
)

/** N1.5: transcribe una región manuscrita de matemática a LaTeX (Strategy, decisión D-06). */
interface MathOcrEngine {
    fun name(): String
    fun isAvailable(): Boolean
    fun recognize(crop: RegionCrop): MathResult
}

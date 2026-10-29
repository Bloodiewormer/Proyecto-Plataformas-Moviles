package cr.ac.una.glifo.ai.engine

/** Recorte de una región MATH tal como lo envía la app: bytes de la imagen y su tipo MIME. */
class RegionCrop(
    val bytes: ByteArray,
    val mediaType: String
)

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

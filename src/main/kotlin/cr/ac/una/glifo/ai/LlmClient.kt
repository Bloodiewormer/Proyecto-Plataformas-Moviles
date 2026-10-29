package cr.ac.una.glifo.ai

interface LlmClient {
    fun completeJson(request: LlmRequest): LlmResult
}

data class LlmRequest(
    val systemPrompt: String,
    val userPrompt: String,
    /** Data URLs ("data:image/jpeg;base64,...") para visión multimodal (N2, issue #41). */
    val images: List<String> = emptyList(),
    val temperature: Double = 0.4,
    val maxOutputTokens: Int? = null,
    /** false: respuesta en texto plano, sin response_format json_object. */
    val jsonResponse: Boolean = true,
    /** Pide al proveedor la log-probabilidad de cada token de salida. */
    val logprobs: Boolean = false,
    /** Nivel de detalle de las imágenes ("low", "high" o "auto"); null usa el del proveedor. */
    val imageDetail: String? = null
)

data class LlmResult(
    val content: String,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val latencyMs: Long,
    /** Log-probabilidad de cada token de salida; vacía si no se pidió o el proveedor no la dio. */
    val tokenLogprobs: List<Double> = emptyList()
)

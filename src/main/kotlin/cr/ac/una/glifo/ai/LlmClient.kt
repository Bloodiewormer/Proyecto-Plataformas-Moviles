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
    /** Modelo que reportó el proveedor en la respuesta. */
    val model: String,
    /** Tokens del `usage` del proveedor; null si no los informó (no es lo mismo que 0). */
    val inputTokens: Int?,
    val outputTokens: Int?,
    val latencyMs: Long,
    /** Cada token de salida con su log-probabilidad; vacía si no se pidió o el proveedor no la dio. */
    val tokenLogprobs: List<TokenLogprob> = emptyList(),
    /** finish_reason del proveedor ("stop", "length"...); "length" indica una salida truncada. */
    val finishReason: String? = null
)

/** [bytes]: UTF-8 exacto del token; un carácter multibyte puede quedar repartido entre tokens. */
data class TokenLogprob(val token: String, val logprob: Double, val bytes: List<Int> = emptyList()) {
    fun utf8(): ByteArray =
        if (bytes.isNotEmpty()) ByteArray(bytes.size) { bytes[it].toByte() } else token.toByteArray(Charsets.UTF_8)
}

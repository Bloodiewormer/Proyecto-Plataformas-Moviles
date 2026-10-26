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
    val maxOutputTokens: Int? = null
)

data class LlmResult(
    val content: String,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val latencyMs: Long
)

package cr.ac.una.glifo.ai.openai

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.LlmResult
import cr.ac.una.glifo.ai.TokenLogprob
import cr.ac.una.glifo.ai.config.OpenAiProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@Component
class OpenAiLlmClient @Autowired constructor(
    private val properties: OpenAiProperties,
    private val jsonMapper: JsonMapper
) : LlmClient {

    constructor(properties: OpenAiProperties) : this(
        properties = properties,
        jsonMapper = JsonMapper.builder().findAndAddModules().build()
    )

    private val log = LoggerFactory.getLogger(OpenAiLlmClient::class.java)

    private val restClient: RestClient by lazy {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(15))
            setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds.coerceAtLeast(1)))
        }
        RestClient.builder()
            .baseUrl(properties.baseUrl.trimEnd('/'))
            .requestFactory(requestFactory)
            .build()
    }

    override fun completeJson(request: LlmRequest): LlmResult {
        val apiKey = properties.apiKey.trim()
        if (apiKey.isEmpty()) {
            throw AiProviderException.notConfigured("La IA no está configurada en el servidor")
        }

        val body = buildRequestBody(request)
        val startNanos = System.nanoTime()

        val rawResponse = try {
            restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String::class.java)
        } catch (ex: RestClientException) {
            log.warn("Error al invocar OpenAI chat/completions (modelo={}): {}", properties.model, ex.javaClass.simpleName)
            throw AiProviderException(
                message = "No se pudo obtener respuesta del proveedor de IA",
                cause = ex
            )
        } catch (ex: Exception) {
            log.warn("Fallo inesperado al invocar OpenAI (modelo={}): {}", properties.model, ex.javaClass.simpleName)
            throw AiProviderException(
                message = "Error de comunicación con el proveedor de IA",
                cause = ex
            )
        }

        val latencyMs = ((System.nanoTime() - startNanos) / 1_000_000L).coerceAtLeast(0L)

        if (rawResponse.isNullOrBlank()) {
            throw AiProviderException("El proveedor de IA devolvió una respuesta vacía")
        }

        val root = try {
            jsonMapper.readTree(rawResponse)
        } catch (ex: Exception) {
            throw AiProviderException("La respuesta del proveedor de IA no es un JSON válido", cause = ex)
        }

        val contentNode = root.path("choices").path(0).path("message").path("content")
        val content = if (contentNode.isMissingNode || contentNode.isNull) "" else contentNode.asString("").trim()
        if (content.isEmpty()) {
            throw AiProviderException("El proveedor de IA devolvió un contenido vacío")
        }

        val modelNode = root.path("model")
        val model = if (modelNode.isMissingNode || modelNode.isNull) {
            properties.model
        } else {
            modelNode.asString(properties.model).ifBlank { properties.model }
        }

        val usageNode = root.path("usage")
        val inputTokens = usageNode.path("prompt_tokens").asInt(0)
        val outputTokens = usageNode.path("completion_tokens").asInt(0)

        val logprobsNode = root.path("choices").path(0).path("logprobs").path("content")
        val tokenLogprobs = if (logprobsNode.isArray) {
            logprobsNode.mapNotNull { node ->
                val logprob = node.get("logprob")?.takeIf { it.isNumber }?.asDouble() ?: return@mapNotNull null
                val bytesNode = node.path("bytes")
                val bytes = if (bytesNode.isArray) (0 until bytesNode.size()).map { bytesNode[it].asInt() } else emptyList()
                TokenLogprob(node.path("token").asString(""), logprob, bytes)
            }
        } else {
            emptyList()
        }

        return LlmResult(
            content = content,
            model = model,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            latencyMs = latencyMs,
            tokenLogprobs = tokenLogprobs,
            finishReason = root.path("choices").path(0).path("finish_reason").takeIf { it.isString }?.asString()
        )
    }

    internal fun buildRequestBody(request: LlmRequest): Map<String, Any> {
        val userContent: Any = if (request.images.isEmpty()) {
            request.userPrompt
        } else {
            buildList {
                add(mapOf("type" to "text", "text" to request.userPrompt))
                for (imageUrl in request.images) {
                    add(
                        mapOf(
                            "type" to "image_url",
                            "image_url" to buildMap {
                                put("url", imageUrl)
                                if (request.imageDetail != null) put("detail", request.imageDetail)
                            }
                        )
                    )
                }
            }
        }

        val messages = listOf(
            mapOf("role" to "system", "content" to request.systemPrompt),
            mapOf("role" to "user", "content" to userContent)
        )

        return buildMap {
            put("model", properties.model)
            put("messages", messages)
            if (request.jsonResponse) {
                put("response_format", mapOf("type" to "json_object"))
            }
            put("temperature", request.temperature)
            if (request.logprobs) {
                put("logprobs", true)
            }
            if (request.maxOutputTokens != null) {
                put("max_tokens", request.maxOutputTokens)
            }
        }
    }
}

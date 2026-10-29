package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.OpenAiProperties
import cr.ac.una.glifo.ai.openai.OpenAiLlmClient
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenAiLlmClientTest {

    @Test
    fun `a - key vacia lanza AiProviderException`() {
        val client = OpenAiLlmClient(OpenAiProperties(apiKey = "   "))

        val ex = assertThrows<AiProviderException> {
            client.completeJson(
                LlmRequest(
                    systemPrompt = "Sistema",
                    userPrompt = "Usuario"
                )
            )
        }

        assertEquals("AI_NOT_CONFIGURED", ex.code)
        assertTrue(ex.isNotConfigured)
    }

    @Test
    fun `b - sin imagenes content del user es string y con imagenes es arreglo text mas image_url`() {
        val client = OpenAiLlmClient(OpenAiProperties(apiKey = "test-key", model = "gpt-4o-mini"))

        // Caso 1: sin imágenes -> user content es String
        val textOnlyBody = client.buildRequestBody(
            LlmRequest(
                systemPrompt = "Sos un asistente",
                userPrompt = "Generá preguntas",
                images = emptyList()
            )
        )

        @Suppress("UNCHECKED_CAST")
        val textOnlyMessages = textOnlyBody["messages"] as List<Map<String, Any>>
        assertEquals(2, textOnlyMessages.size)
        assertEquals("system", textOnlyMessages[0]["role"])
        assertEquals("user", textOnlyMessages[1]["role"])
        assertTrue(textOnlyMessages[1]["content"] is String)
        assertEquals("Generá preguntas", textOnlyMessages[1]["content"])

        // Caso 2: con imágenes -> user content es List con bloques text + image_url
        val dataUrl1 = "data:image/jpeg;base64,/9j/4AAQSkZJRg=="
        val dataUrl2 = "data:image/png;base64,iVBORw0KGgoAAA=="
        val multimodalBody = client.buildRequestBody(
            LlmRequest(
                systemPrompt = "Sos un asistente de visión",
                userPrompt = "Repará estas regiones",
                images = listOf(dataUrl1, dataUrl2),
                maxOutputTokens = 500
            )
        )

        @Suppress("UNCHECKED_CAST")
        val multimodalMessages = multimodalBody["messages"] as List<Map<String, Any>>
        @Suppress("UNCHECKED_CAST")
        val userContentParts = multimodalMessages[1]["content"] as List<Map<String, Any>>

        assertEquals(3, userContentParts.size)
        assertEquals("text", userContentParts[0]["type"])
        assertEquals("Repará estas regiones", userContentParts[0]["text"])

        assertEquals("image_url", userContentParts[1]["type"])
        @Suppress("UNCHECKED_CAST")
        val image1Map = userContentParts[1]["image_url"] as Map<String, String>
        assertEquals(dataUrl1, image1Map["url"])

        assertEquals("image_url", userContentParts[2]["type"])
        @Suppress("UNCHECKED_CAST")
        val image2Map = userContentParts[2]["image_url"] as Map<String, String>
        assertEquals(dataUrl2, image2Map["url"])

        assertEquals(500, multimodalBody["max_tokens"])
        assertEquals(mapOf("type" to "json_object"), multimodalBody["response_format"])
        assertFalse(multimodalBody.containsKey("logprobs"))
        assertFalse(image1Map.containsKey("detail"))
    }

    @Test
    fun `c - texto plano con logprobs y detalle de imagen arma el cuerpo sin response_format`() {
        val client = OpenAiLlmClient(OpenAiProperties(apiKey = "test-key", model = "gpt-4o-mini"))

        val body = client.buildRequestBody(
            LlmRequest(
                systemPrompt = "OCR",
                userPrompt = "Transcribí",
                images = listOf("data:image/png;base64,iVBORw0KGgo="),
                jsonResponse = false,
                logprobs = true,
                imageDetail = "low"
            )
        )

        assertFalse(body.containsKey("response_format"))
        assertEquals(true, body["logprobs"])
        @Suppress("UNCHECKED_CAST")
        val parts = (body["messages"] as List<Map<String, Any>>)[1]["content"] as List<Map<String, Any>>
        @Suppress("UNCHECKED_CAST")
        val image = parts[1]["image_url"] as Map<String, String>
        assertEquals("low", image["detail"])
    }

    @Test
    fun `d - la respuesta con logprobs se lee token por token`() {
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/v1/chat/completions") { exchange ->
            val response = """
                {"model": "gpt-4o-mini-2024-07-18",
                 "choices": [{"message": {"content": "x^2"}, "finish_reason": "stop",
                   "logprobs": {"content": [{"token": "x", "logprob": -0.01, "bytes": [120]}, {"token": "^", "logprob": -0.2}, {"token": "2", "logprob": 0}]}}],
                 "usage": {"prompt_tokens": 2900, "completion_tokens": 3}}
            """.trimIndent().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            val client = OpenAiLlmClient(
                OpenAiProperties(apiKey = "test-key", baseUrl = "http://127.0.0.1:${server.address.port}/v1")
            )

            val result = client.completeJson(LlmRequest(systemPrompt = "OCR", userPrompt = "x", jsonResponse = false, logprobs = true))

            assertEquals("x^2", result.content)
            assertEquals(listOf(TokenLogprob("x", -0.01, listOf(120)), TokenLogprob("^", -0.2), TokenLogprob("2", 0.0)), result.tokenLogprobs)
            assertEquals(2900, result.inputTokens)
            assertEquals("stop", result.finishReason)
        } finally {
            server.stop(0)
        }
    }
}

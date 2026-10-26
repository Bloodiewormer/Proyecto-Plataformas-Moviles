package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.OpenAiProperties
import cr.ac.una.glifo.ai.openai.OpenAiLlmClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
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
    }
}

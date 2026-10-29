package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.OpenAiProperties
import cr.ac.una.glifo.ai.engine.RegionCrop
import cr.ac.una.glifo.ai.engine.VisionMathEngine
import org.junit.jupiter.api.Test
import kotlin.math.exp
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas UNITARIAS del motor N1.5 por visión: el LlmClient es un doble en memoria, sin
 * llamadas reales a OpenAI.
 */
class VisionMathEngineTest {

    private class FakeLlmClient(private val content: String, private val logprobs: List<Double>) : LlmClient {
        var lastRequest: LlmRequest? = null
        override fun completeJson(request: LlmRequest): LlmResult {
            lastRequest = request
            return LlmResult(content, "gpt-4o-mini", 2900, logprobs.size, 120, logprobs)
        }
    }

    private val png = RegionCrop(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), "image/png")

    private fun engine(client: LlmClient, apiKey: String = "key") = VisionMathEngine(client, OpenAiProperties(apiKey = apiKey))

    @Test
    fun `pide texto plano con logprobs, detalle bajo y la imagen como data URL`() {
        val client = FakeLlmClient("x^2", listOf(-0.1, -0.1))

        engine(client).recognize(png)

        val request = client.lastRequest!!
        assertFalse(request.jsonResponse)
        assertTrue(request.logprobs)
        assertEquals("low", request.imageDetail)
        assertEquals(0.0, request.temperature)
        assertEquals("data:image/png;base64,iVBORw==", request.images.single())
    }

    @Test
    fun `la confianza es la media geometrica de la probabilidad de los tokens`() {
        val result = engine(FakeLlmClient("\\frac{a}{b}", listOf(-0.1, -0.3))).recognize(png)

        assertEquals("\\frac{a}{b}", result.latex)
        assertEquals(exp(-0.2).toFloat(), result.confidence, 1e-6f)
        assertEquals("VisionMath", result.engine)
    }

    @Test
    fun `sin logprobs la confianza es cero para que la region escale`() {
        assertEquals(0f, engine(FakeLlmClient("x", emptyList())).recognize(png).confidence)
    }

    @Test
    fun `NO_MATH se convierte en LaTeX vacio con confianza cero`() {
        val result = engine(FakeLlmClient("NO_MATH", listOf(-0.01))).recognize(png)

        assertEquals("", result.latex)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun `se quitan delimitadores y bloques de codigo`() {
        val vision = engine(FakeLlmClient("", emptyList()))

        assertEquals("x+1", vision.cleanLatex("$$ x+1 $$"))
        assertEquals("x+1", vision.cleanLatex("\$x+1\$"))
        assertEquals("x+1", vision.cleanLatex("\\[x+1\\]"))
        assertEquals("x+1", vision.cleanLatex("\\(x+1\\)"))
        assertEquals("\\int_0^1 x\\,dx", vision.cleanLatex("```latex\n\\int_0^1 x\\,dx\n```"))
    }

    @Test
    fun `sin api key el motor no esta disponible`() {
        assertFalse(engine(FakeLlmClient("x", emptyList()), apiKey = " ").isAvailable())
    }
}

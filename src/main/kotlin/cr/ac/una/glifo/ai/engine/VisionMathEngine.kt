package cr.ac.una.glifo.ai.engine

import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.config.OpenAiProperties
import org.springframework.stereotype.Component
import java.util.Base64
import kotlin.math.exp

private const val NO_MATH = "NO_MATH"

private val SYSTEM_PROMPT = """
Sos un motor de OCR matemático. Recibís el recorte de una fórmula manuscrita de un apunte universitario en español.
Respondé únicamente con el LaTeX de la fórmula, en una sola línea, sin delimitadores ($, $$, \(, \[), sin bloques de código y sin explicaciones.
Reglas para notación manuscrita en español:
- Límites e integrales: distinguí cuidadosamente el símbolo de infinito (\infty, +\infty, -\infty) del dígito 2 u 8; conservá los límites de integración (\int_{a}^{b}, \int_{-\infty}^{\infty}), corchetes de evaluación (\left[...\right]_{a}^{b}) y diferenciales (\,dx, \,dt).
- Trigonometría en español: normalizá a comandos LaTeX estándar compatibles con JLaTeXMath: sen -> \sin, tg -> \tan, arctg / arc tg -> \arctan, ctg / cotg -> \cot, cosec -> \csc, sec -> \sec.
- Conservá un '=' inicial si la línea manuscrita empieza con '='.
Transcribí exactamente lo escrito: no corrijas errores matemáticos ni completes partes faltantes.
Si el recorte no contiene una expresión matemática legible, respondé exactamente $NO_MATH.
""".trimIndent()

/**
 * Implementación de [MathOcrEngine] con un modelo de visión del proveedor configurado.
 * La confianza es la media geométrica de la probabilidad de los tokens generados
 * (exp de la media de sus logprobs): la reporta el modelo, no el texto de la respuesta.
 */
@Component
class VisionMathEngine(
    private val llmClient: LlmClient,
    private val properties: OpenAiProperties
) : MathOcrEngine {

    override fun name(): String = "VisionMath"

    override fun isAvailable(): Boolean = properties.apiKey.isNotBlank()

    override fun recognize(crop: RegionCrop): MathResult {
        val dataUrl = "data:${crop.normalizedMediaType()};base64," + Base64.getEncoder().encodeToString(crop.bytes)
        val result = llmClient.completeJson(
            LlmRequest(
                systemPrompt = SYSTEM_PROMPT,
                userPrompt = "Transcribí la fórmula de la imagen a LaTeX.",
                images = listOf(dataUrl),
                temperature = 0.0,
                maxOutputTokens = 300,
                jsonResponse = false,
                logprobs = true,
                imageDetail = "low"
            )
        )

        val latex = cleanLatex(result.content)
        val confidence = if (latex.isEmpty() || result.tokenLogprobs.isEmpty() || result.finishReason == "length") {
            0f
        } else {
            exp(result.tokenLogprobs.map { it.logprob }.average()).toFloat().coerceIn(0f, 1f)
        }

        return MathResult(
            latex = latex,
            confidence = confidence,
            engine = name(),
            model = result.model,
            inputTokens = result.inputTokens,
            outputTokens = result.outputTokens,
            latencyMs = result.latencyMs
        )
    }

    internal fun cleanLatex(raw: String): String = stripLatexDelimiters(raw).let { if (it == NO_MATH) "" else it }
}

/** Quita un bloque de código y un par externo de delimitadores ($$, $, \[ \], \( \)) de un LaTeX. */
fun stripLatexDelimiters(raw: String): String {
    var text = raw.trim()
    if (text.startsWith("```")) {
        text = text.removePrefix("```").substringAfter('\n', "").substringBeforeLast("```").trim()
    }
    for ((open, close) in listOf("$$" to "$$", "$" to "$", "\\[" to "\\]", "\\(" to "\\)")) {
        if (text.length >= open.length + close.length && text.startsWith(open) && text.endsWith(close)) {
            text = text.substring(open.length, text.length - close.length).trim()
            break
        }
    }
    return text
}

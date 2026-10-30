package cr.ac.una.glifo.ai.ledger

import cr.ac.una.glifo.ai.AiProviderException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.core.JacksonException
import java.net.SocketTimeoutException

/** Largo de `ai_calls.error_message`. */
const val ERROR_MESSAGE_MAX_LENGTH = 255

/** Código estable y mensaje ya saneado de un fallo. */
data class AiErrorDiagnostic(val code: String, val message: String)

/**
 * Traduce excepciones a códigos estables para `ai_calls.error_code` y a un resumen corto
 * para `error_message`. Nunca copia el cuerpo de la respuesta del proveedor: de un error
 * HTTP solo toma el código de estado, porque el mensaje de RestClientResponseException trae
 * el cuerpo crudo.
 */
object AiErrorDiagnostics {

    /** Fallo de la llamada al proveedor (success = false). */
    fun providerFailure(ex: Throwable): AiErrorDiagnostic {
        val chain = generateSequence(ex) { it.cause }.take(10).toList()
        val http = chain.firstNotNullOfOrNull { it as? RestClientResponseException }
        return when {
            http != null -> AiErrorDiagnostic("PROVIDER_HTTP_${http.statusCode.value()}", "El proveedor respondió HTTP ${http.statusCode.value()}")
            chain.any { it is SocketTimeoutException } -> AiErrorDiagnostic("PROVIDER_TIMEOUT", "Se agotó el tiempo de espera del proveedor")
            chain.any { it is ResourceAccessException } -> AiErrorDiagnostic("PROVIDER_UNREACHABLE", "No se pudo conectar con el proveedor")
            // El cliente recibió algo pero no es una respuesta utilizable (vacía, sin JSON, sin contenido).
            ex is AiProviderException && (ex.cause == null || ex.cause is JacksonException) ->
                AiErrorDiagnostic("PROVIDER_INVALID_RESPONSE", sanitize(ex.message))
            else -> AiErrorDiagnostic("PROVIDER_ERROR", sanitize(rootSummary(chain)))
        }
    }

    /** La respuesta llegó (success = true) pero el servicio rechazó su contenido. */
    fun outputRejected(ex: Throwable): AiErrorDiagnostic =
        if (ex is AiProviderException) {
            AiErrorDiagnostic("OUTPUT_REJECTED", sanitize(ex.message))
        } else {
            AiErrorDiagnostic("OUTPUT_PROCESSING_FAILED", sanitize(ex.javaClass.simpleName))
        }

    private fun rootSummary(chain: List<Throwable>): String {
        val root = chain.last()
        return if (root is AiProviderException) root.message else root.javaClass.simpleName
    }

    private val SECRET_PATTERNS = listOf(
        Regex("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+") to "Bearer [REDACTED]",
        Regex("sk-[A-Za-z0-9_-]{8,}") to "[REDACTED]",
        // JWT: tres segmentos base64url separados por puntos, el primero empieza en eyJ.
        Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+") to "[REDACTED]",
        Regex("(?i)(api[_-]?key|password|secret|token)\\s*[=:]\\s*\\S+") to "$1=[REDACTED]",
        Regex("data:[a-z]+/[a-z0-9.+-]+;base64,[A-Za-z0-9+/=]+") to "[DATA_URL]"
    )

    /** Quita secretos y datos binarios, colapsa espacios y recorta a [ERROR_MESSAGE_MAX_LENGTH]. */
    fun sanitize(text: String?): String {
        var clean = text.orEmpty()
        for ((pattern, replacement) in SECRET_PATTERNS) clean = pattern.replace(clean, replacement)
        clean = clean.replace(Regex("\\s+"), " ").trim()
        return if (clean.length <= ERROR_MESSAGE_MAX_LENGTH) clean else clean.take(ERROR_MESSAGE_MAX_LENGTH - 1) + "…"
    }
}

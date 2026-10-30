package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.ledger.AiErrorDiagnostics
import cr.ac.una.glifo.ai.ledger.ERROR_MESSAGE_MAX_LENGTH
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiErrorDiagnosticsTest {

    /** Lo mismo que hace OpenAiLlmClient: envuelve la excepción de RestClient. */
    private fun wrapped(cause: Exception) = AiProviderException("No se pudo obtener respuesta del proveedor de IA", cause = cause)

    @Test
    fun `un error HTTP guarda solo el estado, nunca el cuerpo del proveedor`() {
        val body = """{"error":{"message":"Rate limit for key sk-proj-ABCDEFGHIJKLMNOP reached","prompt":"texto del apunte"}}"""
        val http = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders(), body.toByteArray(), StandardCharsets.UTF_8
        )

        val diagnostic = AiErrorDiagnostics.providerFailure(wrapped(http))

        assertEquals("PROVIDER_HTTP_429", diagnostic.code)
        assertEquals("El proveedor respondió HTTP 429", diagnostic.message)
        assertFalse(diagnostic.message.contains("sk-"))
        assertFalse(diagnostic.message.contains("apunte"))
    }

    @Test
    fun `timeout y conexion fallida tienen codigos propios`() {
        val timeout = ResourceAccessException("I/O error on POST", SocketTimeoutException("Read timed out"))
        assertEquals("PROVIDER_TIMEOUT", AiErrorDiagnostics.providerFailure(wrapped(timeout)).code)

        val refused = ResourceAccessException("I/O error on POST: Connection refused")
        assertEquals("PROVIDER_UNREACHABLE", AiErrorDiagnostics.providerFailure(wrapped(refused)).code)
    }

    @Test
    fun `respuesta recibida pero inutilizable es PROVIDER_INVALID_RESPONSE`() {
        val diagnostic = AiErrorDiagnostics.providerFailure(AiProviderException("El proveedor de IA devolvió un contenido vacío"))
        assertEquals("PROVIDER_INVALID_RESPONSE", diagnostic.code)
        assertEquals("El proveedor de IA devolvió un contenido vacío", diagnostic.message)
    }

    @Test
    fun `una excepcion desconocida guarda su clase, no su mensaje`() {
        val diagnostic = AiErrorDiagnostics.providerFailure(
            AiProviderException("Error al generar ítems de estudio con la IA", cause = IllegalStateException("Bearer sk-secret123456789"))
        )
        assertEquals("PROVIDER_ERROR", diagnostic.code)
        assertEquals("IllegalStateException", diagnostic.message)
    }

    @Test
    fun `contenido rechazado distingue la validacion de negocio de un error local`() {
        val rejected = AiErrorDiagnostics.outputRejected(AiProviderException("El JSON devuelto por la IA no contiene el arreglo 'items'"))
        assertEquals("OUTPUT_REJECTED", rejected.code)
        assertEquals("El JSON devuelto por la IA no contiene el arreglo 'items'", rejected.message)

        assertEquals("OUTPUT_PROCESSING_FAILED", AiErrorDiagnostics.outputRejected(NullPointerException("x")).code)
    }

    @Test
    fun `sanitize quita claves, tokens, JWT e imagenes`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJh"
        val text = "Authorization: Bearer abc.def-123 key sk-proj-ABCDEFGHIJ jwt $jwt api_key=xyz img data:image/png;base64,iVBORw0KGgo="

        val clean = AiErrorDiagnostics.sanitize(text)

        assertFalse(clean.contains("abc.def-123"))
        assertFalse(clean.contains("sk-proj"))
        assertFalse(clean.contains(jwt))
        assertFalse(clean.contains("xyz"))
        assertFalse(clean.contains("iVBORw0KGgo"))
        assertTrue(clean.contains("[REDACTED]"))
        assertTrue(clean.contains("[DATA_URL]"))
    }

    @Test
    fun `sanitize colapsa espacios y recorta al largo de la columna`() {
        assertEquals("a b c", AiErrorDiagnostics.sanitize("  a\n\n b\t c "))
        val long = AiErrorDiagnostics.sanitize("x".repeat(1000))
        assertEquals(ERROR_MESSAGE_MAX_LENGTH, long.length)
        assertTrue(long.endsWith("…"))
        assertEquals("", AiErrorDiagnostics.sanitize(null))
    }
}

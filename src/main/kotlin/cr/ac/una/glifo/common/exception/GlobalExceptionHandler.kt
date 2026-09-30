package cr.ac.una.glifo.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

/**
 * Convierte cada excepción en una respuesta HTTP con el mismo formato de error que usa el
 * mock de Mockoon del frontend: un JSON con `message` (más `code` y `timestamp` como
 * información extra, que el cliente ignora).
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiError> =
        error(HttpStatus.NOT_FOUND, ex.code, ex.message!!)

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResource(ex: DuplicateResourceException): ResponseEntity<ApiError> =
        error(HttpStatus.CONFLICT, ex.code, ex.message!!)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiError> {
        val status = if (ex.code == "UNAUTHORIZED") HttpStatus.UNAUTHORIZED else HttpStatus.BAD_REQUEST
        return error(status, ex.code, ex.message!!)
    }

    /** Falla de @Valid: el mensaje es el que declara cada anotación del DTO. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val messages = ex.bindingResult.fieldErrors.mapNotNull { it.defaultMessage }.distinct()
        return error(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            messages.joinToString(". ").ifBlank { "Datos inválidos" },
            details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        )
    }

    /** JSON mal formado o con un tipo de dato incorrecto. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> =
        error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "El cuerpo de la solicitud no es válido")

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ResponseEntity<ApiError> =
        error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Credenciales inválidas")

    /** @PreAuthorize rechazó la operación (por ejemplo, un estudiante creando un curso). */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiError> =
        error(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tenés permiso para realizar esta acción")

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiError> =
        error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Ocurrió un error inesperado: ${ex.message}")

    private fun error(
        status: HttpStatus,
        code: String,
        message: String,
        details: List<Any>? = null
    ): ResponseEntity<ApiError> =
        ResponseEntity(ApiError(code = code, message = message, timestamp = Instant.now(), details = details), status)
}

data class ApiError(
    val code: String,
    val message: String,
    val timestamp: Instant,
    val path: String? = null,
    val details: List<Any>? = null
)

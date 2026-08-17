package cr.ac.una.glifo.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiError> {
        return ResponseEntity(
            ApiError(code = ex.code, message = ex.message!!, timestamp = Instant.now()),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResource(ex: DuplicateResourceException): ResponseEntity<ApiError> {
        return ResponseEntity(
            ApiError(code = ex.code, message = ex.message!!, timestamp = Instant.now()),
            HttpStatus.CONFLICT
        )
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiError> {
        val status = if (ex.code == "UNAUTHORIZED") HttpStatus.UNAUTHORIZED else HttpStatus.BAD_REQUEST
        return ResponseEntity(
            ApiError(code = ex.code, message = ex.message!!, timestamp = Instant.now()),
            status
        )
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException::class)
    fun handleValidationException(ex: org.springframework.web.bind.MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity(
            ApiError(code = "VALIDATION_FAILED", message = "Invalid request payload", timestamp = Instant.now(), details = details),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException::class)
    fun handleBadCredentials(ex: org.springframework.security.authentication.BadCredentialsException): ResponseEntity<ApiError> {
        return ResponseEntity(
            ApiError(code = "UNAUTHORIZED", message = "Invalid email or password", timestamp = Instant.now()),
            HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiError> {
        val error = ApiError(
            code = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred: ${ex.message}",
            timestamp = Instant.now()
        )
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}

data class ApiError(
    val code: String,
    val message: String,
    val timestamp: Instant,
    val path: String? = null,
    val details: List<Any>? = null
)

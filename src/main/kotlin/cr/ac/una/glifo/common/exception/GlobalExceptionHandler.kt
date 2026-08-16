package cr.ac.una.glifo.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiError> {
        val error = ApiError(
            code = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred",
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

package cr.ac.una.glifo.common.exception

import cr.ac.una.glifo.ai.AiProviderException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AccountStatusException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException
import tools.jackson.core.JacksonException
import java.time.Instant

/**
 * Manejo global de errores de la Web API (el "RestExceptionHandler" de la clase).
 *
 * Hereda de [ResponseEntityExceptionHandler], que ya traduce cada error estándar de Spring
 * MVC a su código HTTP semántico (400 tipo inválido, 404 ruta inexistente, 405 método no
 * permitido, 415 Content-Type no soportado…). Aquí solo se reemplaza el cuerpo de esas
 * respuestas por un [ApiError], y se agregan los errores propios del negocio.
 *
 * El campo `message` va en español y siempre presente: es el que muestra la app Android
 * (y el que devolvía el mock de Mockoon).
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ---------------------------------------------------------------------------------
    // Errores de la capa web que ya resuelve Spring: se conserva su código HTTP y se
    // cambia el cuerpo por un ApiError.
    // ---------------------------------------------------------------------------------

    /** @Valid sobre un @RequestBody: un ApiSubError por cada campo que no pasó. */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val subErrors = ex.bindingResult.fieldErrors.map {
            ApiSubError(field = it.field, rejectedValue = it.rejectedValue, message = it.defaultMessage ?: "Valor inválido")
        }
        return respond(
            HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", validationMessage(subErrors), request.path(), subErrors, headers
        )
    }

    /** Restricciones en @PathVariable o @RequestParam (por ejemplo, @Positive en un id). */
    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val subErrors = ex.parameterValidationResults.flatMap { result ->
            result.resolvableErrors.map {
                ApiSubError(
                    field = result.methodParameter.parameterName ?: "parámetro",
                    rejectedValue = result.argument,
                    message = it.defaultMessage ?: "Valor inválido"
                )
            }
        }
        return respond(
            HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", validationMessage(subErrors), request.path(), subErrors, headers
        )
    }

    /** Punto común de todos los demás errores estándar de Spring MVC. */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val (code, message) = when (ex) {
            is HttpMessageNotReadableException -> "MALFORMED_REQUEST" to unreadableMessage(ex)
            is TypeMismatchException -> {
                val name = (ex as? MethodArgumentTypeMismatchException)?.name ?: ex.propertyName ?: "el parámetro"
                "TYPE_MISMATCH" to "El valor '${ex.value}' no es válido para '$name'"
            }
            is MissingServletRequestParameterException ->
                "MISSING_PARAMETER" to "Falta el parámetro obligatorio '${ex.parameterName}'"
            is MissingPathVariableException -> "MISSING_PARAMETER" to "Falta '${ex.variableName}' en la ruta"
            is NoResourceFoundException -> "NOT_FOUND" to "La ruta ${request.path() ?: ""} no existe"
            is HttpRequestMethodNotSupportedException ->
                "METHOD_NOT_ALLOWED" to "El método ${ex.method} no está permitido en esta ruta"
            is HttpMediaTypeNotSupportedException ->
                "UNSUPPORTED_MEDIA_TYPE" to "Content-Type no soportado: se espera application/json"
            is HttpMediaTypeNotAcceptableException ->
                "NOT_ACCEPTABLE" to "Esta ruta solo responde application/json"
            else -> "REQUEST_ERROR" to "No se pudo procesar la solicitud"
        }
        return respond(HttpStatus.valueOf(statusCode.value()), code, message, request.path(), headers = headers)
    }

    // ---------------------------------------------------------------------------------
    // Errores propios del negocio y de las capas inferiores.
    // ---------------------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException, request: HttpServletRequest): ResponseEntity<Any>? =
        respond(HttpStatus.NOT_FOUND, ex.code, ex.message!!, request.requestURI)

    @ExceptionHandler(DuplicateResourceException::class, ConflictException::class)
    fun handleConflict(ex: BusinessException, request: HttpServletRequest): ResponseEntity<Any>? =
        respond(HttpStatus.CONFLICT, ex.code, ex.message!!, request.requestURI)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException, request: HttpServletRequest): ResponseEntity<Any>? {
        val status = if (ex.code == "UNAUTHORIZED") HttpStatus.UNAUTHORIZED else HttpStatus.BAD_REQUEST
        return respond(status, ex.code, ex.message!!, request.requestURI)
    }

    /** Validación de métodos de servicio anotados con restricciones (fuera de un controller). */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: HttpServletRequest): ResponseEntity<Any>? {
        val subErrors = ex.constraintViolations.map {
            ApiSubError(field = it.propertyPath.toString().substringAfterLast('.'), rejectedValue = it.invalidValue, message = it.message)
        }
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", validationMessage(subErrors), request.requestURI, subErrors)
    }

    /** Un UNIQUE, CHECK o FOREIGN KEY de PostgreSQL rechazó la operación. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException, request: HttpServletRequest): ResponseEntity<Any>? {
        log.warn("Restricción de base de datos en {}: {}", request.requestURI, ex.mostSpecificCause.message)
        return respond(
            HttpStatus.CONFLICT, "DATA_INTEGRITY",
            "La operación entra en conflicto con datos existentes", request.requestURI
        )
    }

    /** Valores que el servicio no acepta (por ejemplo, un enum desconocido). */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<Any>? =
        respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.message ?: "Solicitud inválida", request.requestURI)

    @ExceptionHandler(BadCredentialsException::class, AccountStatusException::class)
    fun handleAuthenticationFailure(ex: AuthenticationException, request: HttpServletRequest): ResponseEntity<Any>? =
        respond(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Credenciales inválidas", request.requestURI)

    /** Otra petición tiene bloqueada la misma página; el cliente puede reintentar. */
    @ExceptionHandler(PessimisticLockingFailureException::class)
    fun handleLockFailure(ex: PessimisticLockingFailureException, request: HttpServletRequest): ResponseEntity<Any>? =
        respond(HttpStatus.CONFLICT, "RESOURCE_LOCKED", "El recurso está siendo modificado. Intentá de nuevo.", request.requestURI)

    @ExceptionHandler(AiProviderException::class)
    fun handleAiProvider(ex: AiProviderException, request: HttpServletRequest): ResponseEntity<Any>? {
        val status = if (ex.isNotConfigured || ex.code == "AI_NOT_CONFIGURED") {
            HttpStatus.SERVICE_UNAVAILABLE
        } else {
            HttpStatus.BAD_GATEWAY
        }
        return respond(status, ex.code, ex.message, request.requestURI)
    }

    /** @PreAuthorize rechazó la operación (por ejemplo, un estudiante creando un curso). */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException, request: HttpServletRequest): ResponseEntity<Any>? =
        respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tenés permiso para realizar esta acción", request.requestURI)

    /**
     * Cualquier error no previsto. El detalle técnico va al log del servidor, no al
     * cliente: exponer mensajes internos da pistas sobre la implementación.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<Any>? {
        log.error("Error inesperado en {}", request.requestURI, ex)
        return respond(
            HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
            "Ocurrió un error inesperado. Intentá de nuevo más tarde.", request.requestURI
        )
    }

    // ---------------------------------------------------------------------------------

    private fun respond(
        status: HttpStatus,
        code: String,
        message: String,
        path: String?,
        subErrors: List<ApiSubError>? = null,
        headers: HttpHeaders = HttpHeaders()
    ): ResponseEntity<Any> =
        ResponseEntity.status(status).headers(headers).body(
            ApiError(
                status = status.value(),
                error = status.reasonPhrase,
                code = code,
                message = message,
                timestamp = Instant.now(),
                path = path,
                subErrors = subErrors?.ifEmpty { null }
            )
        )

    /** Mensaje principal de una validación: los mensajes de cada campo, sin repetir. */
    private fun validationMessage(subErrors: List<ApiSubError>): String =
        subErrors.map { it.message }.distinct().joinToString(". ").ifBlank { "Datos inválidos" }

    /**
     * Cuando Jackson no puede construir el DTO (falta un campo obligatorio o trae un tipo
     * incorrecto) indica en qué campo falló; ese nombre es más útil que un mensaje genérico.
     */
    private fun unreadableMessage(ex: HttpMessageNotReadableException): String {
        val field = generateSequence(ex.cause) { it.cause }
            .filterIsInstance<JacksonException>()
            .firstOrNull()
            ?.path
            ?.mapNotNull { it.propertyName }
            ?.joinToString(".")
            ?.ifBlank { null }
        return if (field != null) {
            "Falta el campo '$field' o tiene un valor inválido"
        } else {
            "El cuerpo de la solicitud no es un JSON válido"
        }
    }

    private fun WebRequest.path(): String? = (this as? ServletWebRequest)?.request?.requestURI
}

/**
 * Cuerpo de todas las respuestas de error. Sigue la estructura que se vio en clase
 * (estado, mensaje, fecha y la lista de sub-errores) y conserva `code` y `message`, que
 * son los que consume la app.
 */
data class ApiError(
    /** Código HTTP numérico (400, 404, 409…). */
    val status: Int,
    /** Nombre estándar del código HTTP ("Bad Request", "Not Found"…). */
    val error: String,
    /** Código propio de la API, estable para que el cliente decida qué hacer. */
    val code: String,
    /** Mensaje para mostrar al usuario, en español. */
    val message: String,
    val timestamp: Instant,
    /** Ruta que se pidió. */
    val path: String? = null,
    /** Detalle por campo cuando el error es de validación. */
    @get:com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    val subErrors: List<ApiSubError>? = null
)

/** Un campo que no pasó la validación: cuál, con qué valor y por qué. */
data class ApiSubError(
    val field: String,
    val rejectedValue: Any?,
    val message: String
)

package cr.ac.una.glifo.common.exception

open class BusinessException(message: String, val code: String) : RuntimeException(message)

/** 404. El mensaje llega tal cual al frontend, así que va en español. */
class ResourceNotFoundException(message: String) : BusinessException(message, "NOT_FOUND") {
    constructor(resource: String, id: Any) : this("$resource with id $id not found")
}

/** 409. El mensaje llega tal cual al frontend, así que va en español. */
class DuplicateResourceException(message: String) : BusinessException(message, "DUPLICATE") {
    constructor(resource: String, field: String) : this("$resource with this $field already exists")
}

/**
 * 400. Los datos tienen la forma correcta pero no cumplen una regla que solo se puede
 * revisar en el servicio (por ejemplo, una fecha que no existe en el calendario).
 */
class BadRequestException(message: String) : BusinessException(message, "VALIDATION_FAILED")

/**
 * 409. La petición es válida, pero choca con el estado actual del recurso (por ejemplo,
 * pasar un apunte de READY a DRAFT).
 */
class ConflictException(message: String, code: String = "CONFLICT") : BusinessException(message, code)

class UnauthorizedException(message: String = "Unauthorized") : BusinessException(message, "UNAUTHORIZED")

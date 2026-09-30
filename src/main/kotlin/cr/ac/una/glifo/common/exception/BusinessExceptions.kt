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

class UnauthorizedException(message: String = "Unauthorized") : BusinessException(message, "UNAUTHORIZED")

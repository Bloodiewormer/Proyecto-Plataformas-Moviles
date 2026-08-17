package cr.ac.una.glifo.common.exception

open class BusinessException(message: String, val code: String) : RuntimeException(message)

class ResourceNotFoundException(resource: String, id: Any) : BusinessException("$resource with id $id not found", "NOT_FOUND")

class DuplicateResourceException(resource: String, field: String) : BusinessException("$resource with this $field already exists", "DUPLICATE")

class UnauthorizedException(message: String = "Unauthorized") : BusinessException(message, "UNAUTHORIZED")

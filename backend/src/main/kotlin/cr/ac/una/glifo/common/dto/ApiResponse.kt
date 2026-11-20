package cr.ac.una.glifo.common.dto

data class ApiResponse<T>(
    val data: T,
    val meta: Map<String, Any>? = null
)

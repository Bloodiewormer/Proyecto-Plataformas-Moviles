package cr.ac.una.glifo.common.dto

data class PageResponse<T>(
    val data: List<T>,
    val meta: PageMeta
)

data class PageMeta(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

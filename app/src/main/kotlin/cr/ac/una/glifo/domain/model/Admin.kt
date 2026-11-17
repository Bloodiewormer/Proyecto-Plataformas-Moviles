package cr.ac.una.glifo.domain.model

data class ApiHealth(
    val status: String,
    val service: String,
    val timestamp: String
)

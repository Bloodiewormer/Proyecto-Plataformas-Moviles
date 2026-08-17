package cr.ac.una.glifo.core.common

sealed interface AppError {
    data object NoConnection : AppError
    data object Unauthorized : AppError
    data class Validation(val field: String, val issue: String) : AppError
    data class Server(val code: String, val message: String) : AppError
    data class Database(val message: String) : AppError
    data object Unknown : AppError
}

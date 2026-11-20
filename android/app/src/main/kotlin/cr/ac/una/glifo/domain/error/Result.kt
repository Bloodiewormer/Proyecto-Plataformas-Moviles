package cr.ac.una.glifo.domain.error

import cr.ac.una.glifo.core.common.AppError

/**
 * Domain-level outcome wrapper, per Glifo_Arquitectura_Estandares.md §9.2. Every domain
 * repository method returns this instead of throwing — a network/database/validation
 * failure is a value, not an uncontrolled exception. [AppError] itself lives in
 * `core/common` (cross-cutting, framework-free) since both `data/` and `domain/` need it;
 * `Result` is declared here because it is the domain's own error-handling contract.
 */
sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val error: AppError) : Result<Nothing>
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(value)
    return this
}

inline fun <T> Result<T>.onFailure(action: (AppError) -> Unit): Result<T> {
    if (this is Result.Failure) action(error)
    return this
}

fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.value

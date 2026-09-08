package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result

/** User-facing (Spanish) message for an [AppError] — shared by every ViewModel's error UI. */
fun AppError.toMessage(): String = when (this) {
    is AppError.NoConnection -> "Sin conexión. Intentá de nuevo."
    is AppError.Unauthorized -> "Tu sesión expiró. Iniciá sesión de nuevo."
    is AppError.Validation -> issue
    is AppError.Server -> message
    is AppError.Database -> message
    is AppError.Unknown -> "Ocurrió un error inesperado."
}

/** Straight [Result] → [UiState] mapping for screens that don't need a distinct Empty case. */
fun <T> Result<T>.toUiState(): UiState<T> = when (this) {
    is Result.Success -> UiState.Success(value)
    is Result.Failure -> UiState.Error(error)
}

/** Same, but an empty collection maps to [UiState.Empty] instead of an empty Success. */
fun <T> Result<List<T>>.toUiStateOrEmpty(): UiState<List<T>> = when (this) {
    is Result.Success -> if (value.isEmpty()) UiState.Empty else UiState.Success(value)
    is Result.Failure -> UiState.Error(error)
}

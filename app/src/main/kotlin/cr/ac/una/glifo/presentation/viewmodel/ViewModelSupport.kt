package cr.ac.una.glifo.presentation.viewmodel

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result

/**
 * User-facing message for an [AppError] — shared by every ViewModel's error UI. Takes a
 * [Context] rather than being `@Composable` + `stringResource()`: some call sites resolve
 * this inside a `coroutineScope.launch { }` block (e.g. login/register submit handlers),
 * where a `@Composable` function cannot legally be called. [Validation]/[Server]/[Database]
 * carry messages that already came from the mock/backend or were built elsewhere — those
 * stay as-is, only the fixed cases and AI/permission status codes are localized here.
 */
fun AppError.toMessage(context: Context): String = when (this) {
    is AppError.NoConnection -> context.getString(R.string.error_no_connection)
    is AppError.Unauthorized -> context.getString(R.string.error_unauthorized)
    is AppError.Validation -> issue
    is AppError.Server -> when (code) {
        "502", "503", "AI_PROVIDER_ERROR", "AI_NOT_CONFIGURED" ->
            context.getString(R.string.study_error_ai_unavailable)
        "403" ->
            context.getString(R.string.study_error_forbidden)
        else -> message
    }
    is AppError.Database -> message
    is AppError.Unknown -> context.getString(R.string.error_unknown)
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

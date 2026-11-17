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
    is AppError.Validation -> issue.ifBlank { context.getString(R.string.error_invalid_request) }
    is AppError.Server -> when (code) {
        "502", "503", "AI_PROVIDER_ERROR", "AI_NOT_CONFIGURED" ->
            context.getString(R.string.study_error_ai_unavailable)
        "NOTE_HAS_NO_PAGES" ->
            context.getString(R.string.note_reconstruct_error_no_pages)
        "409" ->
            message.ifBlank { context.getString(R.string.error_conflict) }
        "404" ->
            message.ifBlank { context.getString(R.string.error_not_found) }
        "403" ->
            context.getString(R.string.study_error_forbidden)
        else ->
            message.ifBlank { context.getString(R.string.error_server_code, code) }
    }
    is AppError.Database -> message.ifBlank { context.getString(R.string.error_unknown) }
    is AppError.Unknown -> context.getString(R.string.error_unknown)
}

/** Mensajes de error específicos para el flujo de autenticación/login. */
fun AppError.toLoginMessage(context: Context): String = when (this) {
    is AppError.Unauthorized -> context.getString(R.string.login_error_invalid_credentials)
    else -> toMessage(context)
}

/** Mensajes en español específicos para la reconstrucción IA-01 de apuntes (#42). */
fun AppError.toReconstructMessage(context: Context): String = when (this) {
    is AppError.NoConnection -> context.getString(R.string.note_reconstruct_error_no_connection)
    is AppError.Validation -> if (field == "serverId") {
        context.getString(R.string.note_reconstruct_error_not_synced)
    } else {
        issue
    }
    is AppError.Server -> when (code) {
        "502", "503", "AI_PROVIDER_ERROR", "AI_NOT_CONFIGURED" ->
            context.getString(R.string.note_reconstruct_error_ai_unavailable)
        "409", "NOTE_HAS_NO_PAGES" ->
            context.getString(R.string.note_reconstruct_error_no_pages)
        else -> toMessage(context)
    }
    else -> toMessage(context)
}

fun AppError.toTopicUpdateMessage(context: Context): String = when (this) {
    is AppError.NoConnection -> context.getString(R.string.note_topic_error_no_connection)
    is AppError.Validation -> if (field == "serverId") {
        context.getString(R.string.note_topic_error_not_synced)
    } else {
        issue
    }
    else -> toMessage(context)
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

package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.ui.component.GlifoBanner
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoField
import cr.ac.una.glifo.core.ui.component.GlifoTopAppBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.presentation.viewmodel.ChangePasswordViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Mínimo igual al del registro en la app (el backend acepta desde 6). */
private const val MIN_PASSWORD_LENGTH = 8

@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    var currentError by remember { mutableStateOf<String?>(null) }
    var newError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val errorCurrentEmpty = stringResource(R.string.change_password_error_current_empty)
    val errorNewShort = stringResource(R.string.register_error_password_short)
    val errorSameAsCurrent = stringResource(R.string.change_password_error_same)
    val errorConfirmMismatch = stringResource(R.string.register_error_confirm_mismatch)

    fun validate(): Boolean {
        currentError = if (current.isEmpty()) errorCurrentEmpty else null
        newError = when {
            newPassword.length < MIN_PASSWORD_LENGTH -> errorNewShort
            newPassword == current -> errorSameAsCurrent
            else -> null
        }
        confirmError = if (confirm != newPassword) errorConfirmMismatch else null
        return listOf(currentError, newError, confirmError).all { it == null }
    }

    fun submit() {
        if (!validate()) return
        isSubmitting = true
        scope.launch {
            when (val result = viewModel.changePassword(current, newPassword)) {
                is Result.Success -> {
                    isSubmitting = false
                    done = true
                    // Se deja ver la confirmación un momento y se vuelve al perfil.
                    delay(1_500)
                    onNavigateBack()
                }
                is Result.Failure -> {
                    isSubmitting = false
                    val error = result.error
                    // El backend responde 400 con el motivo (por ejemplo, la actual no coincide).
                    currentError = if (error is AppError.Validation) error.issue else error.toMessage(context)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.change_password_title),
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            if (done) {
                GlifoBanner(
                    text = stringResource(R.string.change_password_success),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                text = stringResource(R.string.change_password_description),
                fontSize = 14.sp,
                color = colors.textSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GlifoField(
                label = stringResource(R.string.change_password_current),
                value = current,
                onValueChange = { current = it; currentError = null },
                isError = currentError != null,
                isPassword = true
            )
            ChangePasswordFieldError(currentError)
            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = stringResource(R.string.change_password_new),
                value = newPassword,
                placeholder = stringResource(R.string.register_field_password_placeholder),
                onValueChange = { newPassword = it; newError = null; confirmError = null },
                isError = newError != null,
                isPassword = true
            )
            ChangePasswordFieldError(newError)
            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = stringResource(R.string.register_field_confirm_password),
                value = confirm,
                onValueChange = { confirm = it; confirmError = null },
                isError = confirmError != null,
                isPassword = true
            )
            ChangePasswordFieldError(confirmError)
            Spacer(modifier = Modifier.height(16.dp))

            GlifoButton(
                text = if (isSubmitting) {
                    stringResource(R.string.change_password_saving)
                } else {
                    stringResource(R.string.change_password_save)
                },
                enabled = !isSubmitting && !done,
                onClick = { submit() }
            )
        }
    }
}

@Composable
private fun ChangePasswordFieldError(message: String?) {
    if (message == null) return
    val colors = GlifoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = colors.alert, modifier = Modifier.size(14.dp))
        Text(text = message, color = colors.alert, fontSize = 12.sp)
    }
}

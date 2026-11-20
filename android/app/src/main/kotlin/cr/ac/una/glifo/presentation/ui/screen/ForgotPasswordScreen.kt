package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.presentation.viewmodel.ForgotPasswordViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FORGOT_EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/** "Olvidé mi contraseña": el correo de la cuenta y la contraseña nueva, sin enlace por correo. */
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var newError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val errorEmailEmpty = stringResource(R.string.register_error_email_empty)
    val errorEmailInvalid = stringResource(R.string.register_error_email_invalid)
    val errorEmailNotFound = stringResource(R.string.forgot_password_error_not_found)
    val errorNewShort = stringResource(R.string.register_error_password_short)
    val errorConfirmMismatch = stringResource(R.string.register_error_confirm_mismatch)

    fun validate(): Boolean {
        emailError = when {
            email.isBlank() -> errorEmailEmpty
            !FORGOT_EMAIL_REGEX.matches(email.trim()) -> errorEmailInvalid
            else -> null
        }
        newError = if (newPassword.length < MIN_PASSWORD_LENGTH) errorNewShort else null
        confirmError = if (confirm != newPassword) errorConfirmMismatch else null
        return listOf(emailError, newError, confirmError).all { it == null }
    }

    fun submit() {
        if (!validate()) return
        isSubmitting = true
        scope.launch {
            when (val result = viewModel.forgotPassword(email, newPassword)) {
                is Result.Success -> {
                    isSubmitting = false
                    done = true
                    delay(1_500)
                    onNavigateBack()
                }
                is Result.Failure -> {
                    isSubmitting = false
                    val error = result.error
                    when {
                        error is AppError.Server && error.code == "404" -> emailError = errorEmailNotFound
                        error is AppError.Validation -> newError = error.issue
                        else -> emailError = error.toMessage(context)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopBar(
            title = stringResource(R.string.forgot_password_title),
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (done) {
                GlifoBanner(
                    text = stringResource(R.string.forgot_password_success),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                text = stringResource(R.string.forgot_password_description),
                fontSize = 14.sp,
                color = colors.textSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GlifoField(
                label = stringResource(R.string.register_field_email),
                value = email,
                placeholder = stringResource(R.string.register_field_email_placeholder),
                onValueChange = { email = it; emailError = null },
                isError = emailError != null
            )
            ChangePasswordFieldError(emailError)
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

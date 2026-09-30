package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoField
import cr.ac.una.glifo.core.ui.component.GlifoTopAppBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.RegisterViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val errorNameEmpty = stringResource(R.string.register_error_name_empty)
    val errorNameInvalid = stringResource(R.string.register_error_name_invalid)
    val errorEmailEmpty = stringResource(R.string.register_error_email_empty)
    val errorEmailInvalid = stringResource(R.string.register_error_email_invalid)
    val errorPasswordEmpty = stringResource(R.string.register_error_password_empty)
    val errorPasswordShort = stringResource(R.string.register_error_password_short)
    val errorConfirmEmpty = stringResource(R.string.register_error_confirm_empty)
    val errorConfirmMismatch = stringResource(R.string.register_error_confirm_mismatch)
    val errorEmailExists = stringResource(R.string.register_error_email_exists)

    fun validateFieldsSync(): Boolean {
        nameError = when {
            fullName.trim().isEmpty() -> errorNameEmpty
            fullName.trim().length < 3 || !fullName.trim().contains(" ") -> errorNameInvalid
            else -> null
        }
        emailError = when {
            email.trim().isEmpty() -> errorEmailEmpty
            !EMAIL_REGEX.matches(email.trim()) -> errorEmailInvalid
            else -> null
        }
        passwordError = when {
            password.isEmpty() -> errorPasswordEmpty
            password.length < 8 -> errorPasswordShort
            else -> null
        }
        confirmError = when {
            confirmPassword.isEmpty() -> errorConfirmEmpty
            confirmPassword != password -> errorConfirmMismatch
            else -> null
        }
        return listOf(nameError, emailError, passwordError, confirmError).all { it == null }
    }

    fun submit() {
        if (!validateFieldsSync()) return
        isSubmitting = true
        coroutineScope.launch {
            // El servidor es quien sabe si el correo ya existe: responde 409 al registrar.
            when (val result = viewModel.register(fullName, email, password)) {
                is Result.Success -> {
                    isSubmitting = false
                    onRegisterSuccess()
                }
                is Result.Failure -> {
                    isSubmitting = false
                    val error = result.error
                    emailError = if (error is AppError.Server && error.code == "409") errorEmailExists else error.toMessage(context)
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
            title = stringResource(R.string.register_title),
            isBack = true,
            onNavigationClick = onNavigateToLogin
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            GlifoField(
                label = stringResource(R.string.register_field_full_name),
                value = fullName,
                onValueChange = { fullName = it; nameError = null }
            )
            FieldError(nameError)

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = stringResource(R.string.register_field_email),
                value = email,
                placeholder = stringResource(R.string.register_field_email_placeholder),
                onValueChange = { email = it; emailError = null },
                isError = emailError != null
            )
            FieldError(emailError)

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = stringResource(R.string.register_field_password),
                value = password,
                placeholder = stringResource(R.string.register_field_password_placeholder),
                onValueChange = { password = it; passwordError = null; confirmError = null },
                isError = passwordError != null
            )
            FieldError(passwordError)

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = stringResource(R.string.register_field_confirm_password),
                value = confirmPassword,
                placeholder = stringResource(R.string.register_field_confirm_placeholder),
                onValueChange = { confirmPassword = it; confirmError = null },
                isError = confirmError != null
            )
            FieldError(confirmError, paddingBottom = 4.dp)

            Spacer(modifier = Modifier.height(10.dp))

            GlifoButton(
                text = if (isSubmitting) {
                    stringResource(R.string.register_button_submitting)
                } else {
                    stringResource(R.string.register_button_submit)
                },
                enabled = !isSubmitting,
                onClick = { submit() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.register_role_note),
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun FieldError(message: String?, paddingBottom: androidx.compose.ui.unit.Dp = 0.dp) {
    val colors = GlifoTheme.colors
    AnimatedVisibility(visible = message != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = paddingBottom),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.alert,
                modifier = Modifier.size(14.dp)
            )
            Text(text = message.orEmpty(), color = colors.alert, fontSize = 12.sp)
        }
    }
}

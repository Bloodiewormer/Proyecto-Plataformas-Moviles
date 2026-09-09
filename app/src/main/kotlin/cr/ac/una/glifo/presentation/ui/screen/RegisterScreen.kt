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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    viewModel: RegisterViewModel
) {
    val colors = GlifoTheme.colors
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

    fun validateFieldsSync(): Boolean {
        nameError = when {
            fullName.trim().isEmpty() -> "Ingresá tu nombre completo"
            fullName.trim().length < 3 || !fullName.trim().contains(" ") -> "Ingresá nombre y apellido"
            else -> null
        }
        emailError = when {
            email.trim().isEmpty() -> "Ingresá tu correo"
            !EMAIL_REGEX.matches(email.trim()) -> "El correo no es válido"
            else -> null
        }
        passwordError = when {
            password.isEmpty() -> "Ingresá una contraseña"
            password.length < 8 -> "Mínimo 8 caracteres"
            else -> null
        }
        confirmError = when {
            confirmPassword.isEmpty() -> "Confirmá tu contraseña"
            confirmPassword != password -> "Las dos contraseñas deben coincidir"
            else -> null
        }
        return listOf(nameError, emailError, passwordError, confirmError).all { it == null }
    }

    fun submit() {
        if (!validateFieldsSync()) return
        isSubmitting = true
        coroutineScope.launch {
            if (viewModel.emailExists(email)) {
                isSubmitting = false
                emailError = "Ya existe una cuenta con ese correo"
                return@launch
            }
            when (val result = viewModel.register(fullName, email, password)) {
                is Result.Success -> {
                    isSubmitting = false
                    onRegisterSuccess()
                }
                is Result.Failure -> {
                    isSubmitting = false
                    emailError = result.error.toMessage()
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
            title = "Crear cuenta",
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
                label = "Nombre completo",
                value = fullName,
                onValueChange = { fullName = it; nameError = null }
            )
            FieldError(nameError)

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = "Correo",
                value = email,
                placeholder = "usuario@est.una.ac.cr",
                onValueChange = { email = it; emailError = null },
                isError = emailError != null
            )
            FieldError(emailError)

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = "Contraseña",
                value = password,
                placeholder = "Mínimo 8 caracteres",
                onValueChange = { password = it; passwordError = null; confirmError = null },
                isError = passwordError != null
            )
            FieldError(passwordError)

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = "Confirmar contraseña",
                value = confirmPassword,
                placeholder = "••••••",
                onValueChange = { confirmPassword = it; confirmError = null },
                isError = confirmError != null
            )
            FieldError(confirmError, paddingBottom = 4.dp)

            Spacer(modifier = Modifier.height(10.dp))

            GlifoButton(
                text = if (isSubmitting) "Creando cuenta…" else "Crear cuenta",
                enabled = !isSubmitting,
                onClick = { submit() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Vas a entrar como estudiante. Si sos docente, pedile el cambio de rol al administrador.",
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

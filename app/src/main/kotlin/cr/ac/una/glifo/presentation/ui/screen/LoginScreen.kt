package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.R
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.DemoAccountHint
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoTextField
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.LoginViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: (GlifoRole) -> Unit,
    viewModel: LoginViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    val demoAccountHints by viewModel.demoAccountHints.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }

    fun attemptLogin() {
        when {
            email.isBlank() || password.isBlank() -> error = "Llená todos los campos"
            else -> {
                isLoading = true
                error = null
                coroutineScope.launch {
                    when (val result = viewModel.login(email, password)) {
                        is Result.Success -> {
                            isLoading = false
                            onLoginSuccess(result.value.role)
                        }
                        is Result.Failure -> {
                            isLoading = false
                            error = result.error.toMessage()
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlifoTheme.colors.background)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 8 },
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_glifo_mark),
                    contentDescription = null,
                    tint = GlifoTheme.colors.textPrimary,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Entrar a Glifo",
                    color = GlifoTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Usá tu correo institucional.",
                    color = GlifoTheme.colors.textSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                GlifoTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = "Correo",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlifoTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = "Contraseña",
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = GlifoTheme.colors.alert,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Crossfade(targetState = isLoading, label = "login-action") { loading ->
                    if (loading) {
                        Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GlifoTheme.colors.accent)
                        }
                    } else {
                        Column {
                            GlifoButton(
                                text = "Iniciar sesión",
                                onClick = { attemptLogin() }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable { onNavigateToForgotPassword() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Olvidé mi contraseña",
                                    color = GlifoTheme.colors.accentText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                DemoAccountHints(
                    hints = demoAccountHints,
                    onPick = { hint ->
                        email = hint.email
                        password = hint.password
                        error = null
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿Todavía no tenés cuenta?",
                color = GlifoTheme.colors.textSecondary,
                fontSize = 13.sp
            )
            Text(
                text = "Crear cuenta",
                color = GlifoTheme.colors.accentText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

/** Dummy-phase affordance: tap a role to autofill its seeded credentials. */
@Composable
private fun DemoAccountHints(hints: List<DemoAccountHint>, onPick: (DemoAccountHint) -> Unit) {
    val colors = GlifoTheme.colors
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = "PROBAR CON UN ROL DE PRUEBA",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            hints.forEach { hint ->
                val label = when (hint.role) {
                    GlifoRole.Student -> "Estudiante"
                    GlifoRole.Teacher -> "Docente"
                    GlifoRole.Admin -> "Admin"
                }
                Box(
                    modifier = Modifier
                        .background(colors.neutralSoft, RoundedCornerShape(999.dp))
                        .clickable { onPick(hint) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(text = label, fontSize = 13.sp, color = colors.textSecondary)
                }
            }
        }
    }
}

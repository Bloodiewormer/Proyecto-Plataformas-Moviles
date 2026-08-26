package cr.ac.una.glifo.feature.auth.presentation

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
import cr.ac.una.glifo.core.data.dummy.DummyAuthProvider
import cr.ac.una.glifo.core.data.dummy.DummyUser
import cr.ac.una.glifo.core.data.session.SessionManager
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoRole
import cr.ac.una.glifo.core.ui.component.GlifoTextField
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: (GlifoRole) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }

    fun attemptLogin() {
        when {
            email.isBlank() || password.isBlank() -> error = "Llená todos los campos"
            else -> {
                isLoading = true
                error = null
                coroutineScope.launch {
                    delay(500)
                    val user = DummyAuthProvider.findUser(email, password)
                    isLoading = false
                    if (user != null) {
                        SessionManager.login(user)
                        onLoginSuccess(user.role)
                    } else {
                        error = "Correo o contraseña incorrectos"
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
                    onPick = { user ->
                        email = user.email
                        password = user.password
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
private fun DemoAccountHints(onPick: (DummyUser) -> Unit) {
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
            DummyAuthProvider.demoAccounts.forEach { user ->
                val label = when (user.role) {
                    GlifoRole.Student -> "Estudiante"
                    GlifoRole.Teacher -> "Docente"
                    GlifoRole.Admin -> "Admin"
                }
                Box(
                    modifier = Modifier
                        .background(colors.neutralSoft, RoundedCornerShape(999.dp))
                        .clickable { onPick(user) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(text = label, fontSize = 13.sp, color = colors.textSecondary)
                }
            }
        }
    }
}

package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.core.network.GlifoApi
import cr.ac.una.glifo.core.network.dto.RegisterRequest
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoTextField
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    glifoApi: GlifoApi? = null,
    tokenManager: TokenManager? = null
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlifoTheme.colors.background)
    ) {
        GlifoTopBar(title = "Crear cuenta", onNavigateBack = onNavigateToLogin)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            GlifoTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre completo"
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlifoTextField(
                value = email,
                onValueChange = { email = it },
                label = "Correo"
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlifoTextField(
                value = password,
                onValueChange = { password = it },
                label = "Contraseña"
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlifoTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirmar contraseña",
                isError = error != null,
                errorMessage = error
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlifoTheme.colors.accent)
                }
            } else {
                GlifoButton(
                    text = "Crear cuenta",
                    onClick = {
                        if (name.isBlank() || email.isBlank() || password.isBlank()) {
                            error = "Todos los campos son requeridos."
                        } else if (password != confirmPassword) {
                            error = "Las dos contraseñas deben coincidir."
                        } else {
                            error = null
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    if (glifoApi != null && tokenManager != null) {
                                        val response = glifoApi.register(
                                            RegisterRequest(
                                                email = email.trim(),
                                                password = password,
                                                firstName = name.trim(),
                                                lastName = ""
                                            )
                                        )
                                        val authData = response.data
                                        tokenManager.saveToken(
                                            token = authData.token,
                                            userId = authData.user.id,
                                            userEmail = authData.user.email,
                                            roles = authData.user.roles
                                        )
                                    } else {
                                        tokenManager?.saveToken(
                                            token = "mock-token",
                                            userId = 1L,
                                            userEmail = email.trim(),
                                            roles = listOf("ROLE_STUDENT")
                                        )
                                    }
                                    isLoading = false
                                    onRegisterSuccess()
                                } catch (e: Exception) {
                                    isLoading = false
                                    error = e.message ?: "Error al registrar la cuenta"
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Ya tienes cuenta?",
                    color = GlifoTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Inicia sesión",
                    color = GlifoTheme.colors.accentText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}

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
import cr.ac.una.glifo.core.network.dto.LoginRequest
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoTextField
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    glifoApi: GlifoApi? = null,
    tokenManager: TokenManager? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlifoTheme.colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Entrar a Glifo",
            color = GlifoTheme.colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Usá tu correo institucional.",
            color = GlifoTheme.colors.textSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

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

        if (error != null) {
            Text(
                text = error!!,
                color = GlifoTheme.colors.alert,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = GlifoTheme.colors.accent)
        } else {
            GlifoButton(
                text = "Iniciar sesión",
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        error = "Llená todos los campos"
                    } else {
                        isLoading = true
                        error = null
                        coroutineScope.launch {
                            try {
                                if (glifoApi != null && tokenManager != null) {
                                    val response = glifoApi.login(LoginRequest(email = email.trim(), password = password))
                                    val authData = response.data
                                    tokenManager.saveToken(
                                        token = authData.token,
                                        userId = authData.user.id,
                                        userEmail = authData.user.email,
                                        roles = authData.user.roles
                                    )
                                } else {
                                    // Fallback for previews/mocking
                                    tokenManager?.saveToken(
                                        token = "mock-token",
                                        userId = 1L,
                                        userEmail = email.trim(),
                                        roles = listOf("ROLE_STUDENT")
                                    )
                                }
                                isLoading = false
                                onLoginSuccess()
                            } catch (e: Exception) {
                                isLoading = false
                                error = e.message ?: "Credenciales inválidas"
                            }
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿No tienes cuenta?",
                color = GlifoTheme.colors.textSecondary,
                fontSize = 14.sp
            )
            Text(
                text = "Regístrate",
                color = GlifoTheme.colors.accentText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

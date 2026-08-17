package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoTextField
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

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

            GlifoButton(
                text = "Crear cuenta",
                onClick = {
                    if (password != confirmPassword) {
                        error = "Las dos contraseñas deben coincidir."
                    } else {
                        error = null
                        onRegisterSuccess()
                    }
                }
            )

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

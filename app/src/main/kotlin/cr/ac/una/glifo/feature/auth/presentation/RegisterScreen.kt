package cr.ac.una.glifo.feature.auth.presentation

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
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoField
import cr.ac.una.glifo.core.ui.component.GlifoTopAppBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val colors = GlifoTheme.colors
    var fullName by remember { mutableStateOf("Brandon Brenes") }
    var email by remember { mutableStateOf("bbrenes@est.una.ac.cr") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

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
                onValueChange = { fullName = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = "Correo",
                value = email,
                onValueChange = { email = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = "Contraseña",
                value = password,
                placeholder = "Mínimo 8 caracteres",
                onValueChange = { password = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(
                label = "Confirmar contraseña",
                value = confirmPassword,
                placeholder = "••••••",
                onValueChange = { confirmPassword = it },
                isError = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.alert,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Las dos contraseñas deben coincidir.",
                    color = colors.alert,
                    fontSize = 12.sp
                )
            }

            GlifoButton(
                text = "Crear cuenta",
                onClick = onRegisterSuccess
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

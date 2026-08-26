package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoButtonVariant
import cr.ac.una.glifo.core.ui.component.GlifoField
import cr.ac.una.glifo.core.ui.component.GlifoLogo
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginStudentSuccess: () -> Unit,
    onLoginTeacherSuccess: () -> Unit = onLoginStudentSuccess,
    onLoginAdminSuccess: () -> Unit = onLoginStudentSuccess
) {
    val colors = GlifoTheme.colors
    var email by remember { mutableStateOf("bbrenes@est.una.ac.cr") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Small Logo without ring
        GlifoLogo(size = 44.dp, showRing = false)

        Text(
            text = "Entrar a Glifo",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 18.dp, bottom = 5.dp)
        )

        Text(
            text = "Usá tu correo institucional.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 22.dp)
        )

        GlifoField(
            label = "Correo",
            value = email,
            onValueChange = { email = it },
            isFocused = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        GlifoField(
            label = "Contraseña",
            value = password,
            placeholder = "••••••••",
            onValueChange = { password = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        GlifoButton(
            text = "Entrar como Estudiante",
            onClick = onLoginStudentSuccess
        )

        Spacer(modifier = Modifier.height(6.dp))

        GlifoButton(
            text = "Entrar como Docente",
            variant = GlifoButtonVariant.Secondary,
            onClick = onLoginTeacherSuccess
        )

        Spacer(modifier = Modifier.height(6.dp))

        GlifoButton(
            text = "Entrar como Admin",
            variant = GlifoButtonVariant.Secondary,
            onClick = onLoginAdminSuccess
        )

        Spacer(modifier = Modifier.height(6.dp))

        GlifoButton(
            text = "Olvidé mi contraseña",
            variant = GlifoButtonVariant.Text,
            onClick = onNavigateToForgotPassword
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿Todavía no tenés cuenta?",
                fontSize = 13.sp,
                color = colors.textSecondary
            )
            Text(
                text = "Crear cuenta",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.accentText,
                modifier = Modifier.clickable(onClick = onNavigateToRegister)
            )
        }
    }
}

package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoTextField
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onSendLink: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlifoTheme.colors.background)
    ) {
        GlifoTopBar(
            title = "Recuperar contraseña",
            onNavigateBack = onNavigateBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Ingresá tu correo institucional y te enviaremos un enlace para restablecer tu contraseña.",
                color = GlifoTheme.colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            GlifoTextField(
                value = email,
                onValueChange = { email = it },
                label = "Correo institucional",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlifoButton(
                text = "Enviar enlace",
                onClick = onSendLink
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = GlifoTheme.colors.accent.copy(alpha = 0.16f), // accentSoft
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = GlifoTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Si la dirección está registrada, recibirás el correo en menos de 5 minutos. Revisá también la carpeta de spam.",
                    color = GlifoTheme.colors.textPrimary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

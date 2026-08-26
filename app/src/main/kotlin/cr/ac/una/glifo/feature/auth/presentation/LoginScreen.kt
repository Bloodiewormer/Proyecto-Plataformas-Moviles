package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoTextField
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
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
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            val accentText = GlifoTheme.colors.accentText
            Canvas(modifier = Modifier.size(44.dp)) {
                val scale = size.width / 88f
                val path = Path().apply {
                    moveTo(30f * scale, 58f * scale)
                    cubicTo(
                        30f * scale, 34f * scale,
                        58f * scale, 34f * scale,
                        58f * scale, 48f * scale
                    )
                    cubicTo(
                        58f * scale, 58f * scale,
                        42f * scale, 60f * scale,
                        38f * scale, 50f * scale
                    )
                    cubicTo(
                        35f * scale, 43f * scale,
                        46f * scale, 38f * scale,
                        52f * scale, 44f * scale
                    )
                }
                drawPath(
                    path = path,
                    color = accentText,
                    style = Stroke(width = 4.2f * scale, cap = StrokeCap.Round)
                )
            }

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
                onValueChange = { email = it },
                label = "Correo",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            GlifoTextField(
                value = password,
                onValueChange = { password = it },
                label = "Contraseña",
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = GlifoTheme.colors.alert,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlifoTheme.colors.accent)
                }
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
                                delay(1000)
                                isLoading = false
                                onLoginSuccess()
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { /* Handle forgot password */ },
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

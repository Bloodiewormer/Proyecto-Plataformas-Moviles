package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoButtonVariant
import cr.ac.una.glifo.core.ui.component.GlifoLogo
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val colors = GlifoTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Center content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlifoLogo(size = 88.dp, showRing = true)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Glifo",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "El glifo es la unidad de la escritura.",
                fontSize = 13.sp,
                color = colors.textSecondary
            )
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlifoButton(
                text = "Entrar",
                onClick = onNavigateToLogin
            )
            GlifoButton(
                text = "Crear cuenta",
                variant = GlifoButtonVariant.Secondary,
                onClick = onNavigateToRegister
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Grupo X-Ray · EIF411",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

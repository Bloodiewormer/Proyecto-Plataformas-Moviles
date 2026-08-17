package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit
) {
    LaunchedEffect(key1 = true) {
        delay(2000L)
        onNavigateToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlifoTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Glifo",
            color = GlifoTheme.colors.accent,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

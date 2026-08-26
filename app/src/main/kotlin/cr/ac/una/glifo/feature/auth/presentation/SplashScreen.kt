package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit
) {
    LaunchedEffect(key1 = true) {
        delay(3000L)
        onNavigateToLogin()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlifoTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_glifo_mark),
            contentDescription = null,
            tint = GlifoTheme.colors.textPrimary,
            modifier = Modifier.size(88.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Glifo",
            color = GlifoTheme.colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "El glifo es la unidad de la escritura.",
            color = GlifoTheme.colors.textSecondary,
            fontSize = 13.sp
        )
    }
}

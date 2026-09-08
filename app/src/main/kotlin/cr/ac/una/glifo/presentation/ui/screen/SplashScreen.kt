package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
    var animateIn by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "splash-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(500),
        label = "splash-alpha"
    )

    LaunchedEffect(key1 = true) {
        animateIn = true
        delay(2200L)
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
            modifier = Modifier
                .size(88.dp)
                .scale(scale)
                .alpha(alpha)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Glifo",
            color = GlifoTheme.colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.alpha(alpha)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "El glifo es la unidad de la escritura.",
            color = GlifoTheme.colors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.alpha(alpha)
        )
    }
}

package cr.ac.una.glifo.feature.auth.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val accentText = GlifoTheme.colors.accentText
        Canvas(modifier = Modifier.size(88.dp)) {
            val scale = size.width / 88f
            
            drawCircle(
                color = accentText.copy(alpha = 0.45f),
                radius = 39f * scale,
                center = Offset(44f * scale, 44f * scale),
                style = Stroke(width = 1.2f * scale)
            )

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
                style = Stroke(width = 3.4f * scale, cap = StrokeCap.Round)
            )

            drawCircle(
                color = accentText,
                radius = 2.8f * scale,
                center = Offset(56.5f * scale, 29.5f * scale)
            )
        }
        
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

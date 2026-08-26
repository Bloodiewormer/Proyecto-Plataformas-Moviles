package cr.ac.una.glifo.feature.study.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun QuizResultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCoverage: () -> Unit,
    onNavigateToGlossary: () -> Unit
) {
    val colors = GlifoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Resultado",
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // Score circle presentation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(128.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 12.dp.toPx()
                        val radius = (size.minDimension - strokeW) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Background ring
                        drawCircle(
                            color = colors.surfaceHigh,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeW)
                        )

                        // Progress ring (70%)
                        drawArc(
                            color = colors.accentText,
                            startAngle = -90f,
                            sweepAngle = 0.70f * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "7",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "de 10",
                            fontSize = 15.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Series de potencias sigue floja",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Los 3 fallos son del mismo tema.",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "REVISIÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                listOf(
                    Pair(true, "1 · Definición de sucesión"),
                    Pair(true, "2 · Serie geométrica"),
                    Pair(false, "3 · Radio de convergencia"),
                    Pair(false, "4 · Serie exponencial")
                ).forEachIndexed { i, (ok, label) ->
                    GlifoItem(
                        compact = true,
                        showBottomBorder = i < 3,
                        backgroundColor = if (ok) androidx.compose.ui.graphics.Color.Transparent else colors.alertFaint
                    ) {
                        Icon(
                            imageVector = if (ok) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (ok) colors.verified else colors.alert,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlifoCard(
                backgroundColor = colors.accentFaint,
                borderColor = colors.accentLine
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                    Column {
                        Text("3 ítems reprogramados para mañana", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("Los aciertos vuelven en 6 días.", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlifoButton(
                text = "Ver la cobertura actualizada",
                icon = Icons.Default.DonutLarge,
                onClick = onNavigateToCoverage
            )

            Spacer(modifier = Modifier.height(6.dp))

            GlifoButton(
                text = "Explicame el radio de convergencia",
                variant = GlifoButtonVariant.Secondary,
                icon = Icons.Default.MenuBook,
                onClick = onNavigateToGlossary
            )
        }
    }
}

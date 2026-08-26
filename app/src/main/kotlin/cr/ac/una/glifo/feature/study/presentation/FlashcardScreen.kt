package cr.ac.una.glifo.feature.study.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun FlashcardScreen(
    onNavigateBack: () -> Unit
) {
    val colors = GlifoTheme.colors

    var flipped by remember { mutableStateOf(false) }
    var cardIdx by remember { mutableIntStateOf(0) }

    val cards = listOf(
        Triple(
            "¿Qué condición deben cumplir las series para aplicar el criterio de comparación directa?",
            "Ambas series deben ser de términos positivos y debe cumplirse aₙ ≤ bₙ para todo n a partir de un índice.",
            "De tu apunte del 7 ago"
        ),
        Triple(
            "¿Cuándo converge una serie geométrica ∑ arⁿ?",
            "Cuando |r| < 1. La suma es a/(1−r).",
            "De tu apunte del 3 ago"
        ),
        Triple(
            "Enunciá el criterio de la integral para series de términos positivos.",
            "La serie ∑f(n) converge si y solo si la integral ∫f(x)dx de 1 a ∞ converge.",
            "De tu apunte del 5 ago"
        )
    )

    val currentCard = cards[cardIdx % cards.size]
    val total = 12
    val current = cardIdx + 1

    fun advance() {
        flipped = false
        cardIdx++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            isBack = true,
            onNavigationClick = onNavigateBack,
            customTitle = {
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surfaceHigh)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(current / total.toFloat())
                                .background(colors.accent)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$current / $total",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Unidad 3 · ${if (flipped) "Respuesta" else "Pregunta"} — tocá la tarjeta para voltear",
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Flashcard Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, if (flipped) colors.accentLine else colors.border, RoundedCornerShape(12.dp))
                    .clickable { flipped = !flipped }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlifoBadge(
                        text = if (flipped) "RESPUESTA" else "PREGUNTA",
                        backgroundColor = if (flipped) colors.accentSoft else colors.neutralSoft,
                        textColor = if (flipped) colors.accentText else colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (flipped) currentCard.second else currentCard.first,
                        fontSize = if (flipped) 16.sp else 19.sp,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        color = if (flipped) colors.textSecondary else colors.textPrimary
                    )

                    if (flipped) {
                        Spacer(modifier = Modifier.height(20.dp))
                        GlifoBadge(
                            text = currentCard.third.uppercase(),
                            backgroundColor = colors.neutralSoft,
                            textColor = colors.textSecondary
                        )
                    }
                }
            }

            if (flipped) {
                Text(
                    text = "¿Qué tan bien la recordaste?",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("Otra vez", colors.alert),
                        Pair("Difícil", colors.escalated),
                        Pair("Bien", colors.repaired),
                        Pair("Fácil", colors.verified)
                    ).forEach { (label, col) ->
                        Surface(
                            onClick = { advance() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, col)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = col)
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

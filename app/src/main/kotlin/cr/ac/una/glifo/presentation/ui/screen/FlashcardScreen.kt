package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.FlashcardViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun FlashcardScreen(
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val density = LocalDensity.current
    val uiState by viewModel.state.collectAsState()

    when (val state = uiState) {
        is UiState.Loading -> GlifoLoadingBox()
        is UiState.Empty -> GlifoLoadingBox()
        is UiState.Error -> GlifoErrorBox(state.error.toMessage(), onRetry = viewModel::load)
        is UiState.Success -> {
            val cards = state.data
            FlashcardContent(cards, onNavigateBack, colors, density)
        }
    }
}

@Composable
private fun FlashcardContent(
    cards: List<cr.ac.una.glifo.domain.model.Flashcard>,
    onNavigateBack: () -> Unit,
    colors: cr.ac.una.glifo.core.ui.theme.GlifoColorScheme,
    density: androidx.compose.ui.unit.Density
) {
    var flipped by remember { mutableStateOf(false) }
    var cardIdx by remember { mutableIntStateOf(0) }

    val currentCard = cards[cardIdx % cards.size]
    val total = cards.size
    val current = (cardIdx % cards.size) + 1

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(420),
        label = "flashcard-rotation"
    )

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

            // Flashcard Box — real 3D flip via rotationY, swapping content past the
            // 90° mark so the back face isn't shown mirrored.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density.density
                    }
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, if (flipped) colors.accentLine else colors.border, RoundedCornerShape(12.dp))
                    .clickable { flipped = !flipped }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val showingBack = rotation > 90f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationY = if (showingBack) 180f else 0f }
                ) {
                    GlifoBadge(
                        text = if (showingBack) "RESPUESTA" else "PREGUNTA",
                        backgroundColor = if (showingBack) colors.accentSoft else colors.neutralSoft,
                        textColor = if (showingBack) colors.accentText else colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (showingBack) currentCard.answer else currentCard.question,
                        fontSize = if (showingBack) 16.sp else 19.sp,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        color = if (showingBack) colors.textSecondary else colors.textPrimary
                    )

                    if (showingBack) {
                        Spacer(modifier = Modifier.height(20.dp))
                        GlifoBadge(
                            text = currentCard.source.uppercase(),
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
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, col)
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

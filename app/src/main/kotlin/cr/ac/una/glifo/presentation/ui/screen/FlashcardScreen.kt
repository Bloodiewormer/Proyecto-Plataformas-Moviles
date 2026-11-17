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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
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
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    val title = stringResource(R.string.study_state_flashcards_title)
    when (val state = uiState) {
        is UiState.Loading -> GlifoStatusScreen(title, onNavigateBack) { GlifoLoadingBox() }
        is UiState.Empty -> GlifoStatusScreen(title, onNavigateBack) {
            GlifoEmptyBox(
                message = stringResource(R.string.study_empty_flashcards),
                actionText = stringResource(R.string.common_back),
                onAction = onNavigateBack
            )
        }
        is UiState.Error -> GlifoStatusScreen(title, onNavigateBack) {
            GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
        }
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
    // Cola de la sesión: "Otra vez" manda la tarjeta al final; al vaciarse, la sesión termina.
    // Antes el índice daba la vuelta (cardIdx % size) y la última tarjeta se repetía sin fin.
    var queue by remember(cards) { mutableStateOf(cards.indices.toList()) }
    var reviewed by remember(cards) { mutableIntStateOf(0) }
    var againCount by remember(cards) { mutableIntStateOf(0) }

    if (queue.isEmpty()) {
        FlashcardSessionDone(
            reviewed = reviewed,
            againCount = againCount,
            colors = colors,
            onRestart = {
                queue = cards.indices.toList()
                reviewed = 0
                againCount = 0
                flipped = false
            },
            onNavigateBack = onNavigateBack
        )
        return
    }

    val currentCard = cards[queue.first()]
    val total = cards.size
    val current = (total - queue.distinct().size + 1).coerceIn(1, total)

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(420),
        label = "flashcard-rotation"
    )

    fun advance(again: Boolean) {
        flipped = false
        reviewed++
        val head = queue.first()
        queue = queue.drop(1) + if (again) listOf(head) else emptyList()
        if (again) againCount++
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
                        text = stringResource(R.string.flashcard_progress_counter, current, total),
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
                text = stringResource(
                    R.string.flashcard_unit_label,
                    if (flipped) stringResource(R.string.flashcard_answer_label) else stringResource(R.string.flashcard_question_label)
                ),
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
                        text = if (showingBack) stringResource(R.string.flashcard_badge_answer) else stringResource(R.string.flashcard_badge_question),
                        backgroundColor = if (showingBack) colors.accentSoft else colors.neutralSoft,
                        textColor = if (showingBack) colors.accentText else colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (showingBack) currentCard.answer else currentCard.question,
                        fontSize = if (showingBack) 16.sp else 19.sp,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        // La respuesta iba en gris sobre fondo oscuro y se leía poco.
                        color = colors.textPrimary
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
                    text = stringResource(R.string.flashcard_how_well_remembered),
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
                        Triple(stringResource(R.string.flashcard_rating_again), colors.alert, true),
                        Triple(stringResource(R.string.flashcard_rating_hard), colors.escalated, false),
                        Triple(stringResource(R.string.flashcard_rating_good), colors.repaired, false),
                        Triple(stringResource(R.string.flashcard_rating_easy), colors.verified, false)
                    ).forEach { (label, col, again) ->
                        Surface(
                            onClick = { advance(again) },
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

/** Cierre de la sesión: cuántas se repasaron y la opción de repetir o volver. */
@Composable
private fun FlashcardSessionDone(
    reviewed: Int,
    againCount: Int,
    colors: cr.ac.una.glifo.core.ui.theme.GlifoColorScheme,
    onRestart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.flashcard_done_title),
            isBack = true,
            onNavigationClick = onNavigateBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.flashcard_done_headline),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.flashcard_done_summary, reviewed, againCount),
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            GlifoButton(text = stringResource(R.string.flashcard_done_restart), onClick = onRestart)
            Spacer(modifier = Modifier.height(8.dp))
            GlifoButton(
                text = stringResource(R.string.flashcard_done_back),
                variant = GlifoButtonVariant.Secondary,
                onClick = onNavigateBack
            )
        }
    }
}

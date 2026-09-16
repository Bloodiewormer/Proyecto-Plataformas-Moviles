package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.NoteContentViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun CorrectFragmentScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    onSaveCorrection: () -> Unit,
    viewModel: NoteContentViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()
    var contributeToGlossary by remember { mutableStateOf(true) }
    var selectedSuggestion by remember(noteId) { mutableStateOf(0) }

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.correct_fragment_title),
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                TextButton(onClick = {
                    val correction = ((uiState as? UiState.Success)?.data)?.correction
                    if (correction != null) {
                        val text = correction.suggestions.getOrElse(selectedSuggestion) { correction.recognizedWord }
                        viewModel.saveCorrection(noteId, correction.regionIndex, text)
                    }
                    onSaveCorrection()
                }) {
                    Text(stringResource(R.string.correct_fragment_save), color = colors.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(noteId) })
            is UiState.Success -> {
                val correction = state.data.correction

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlifoBadge(
                            text = stringResource(R.string.correct_fragment_uncertain_badge),
                            backgroundColor = colors.uncertainSoft,
                            textColor = colors.uncertain
                        )
                        Text(
                            stringResource(R.string.correct_fragment_confidence_threshold, correction.confidence, correction.threshold),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Original crop placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.correct_fragment_original_crop_placeholder), fontSize = 12.sp, color = colors.textSecondary)
                    }

                    Text(
                        text = stringResource(R.string.correct_fragment_region_counter, correction.regionIndex, correction.regionTotal),
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Text(
                        text = stringResource(R.string.correct_fragment_engine_read_section),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Recognition box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 80.dp)
                            .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                            .border(2.dp, colors.accentLine, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(stringResource(R.string.correct_fragment_recognized_text_label), fontSize = 11.sp, color = colors.accentText, modifier = Modifier.padding(bottom = 4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(correction.recognizedPrefix, fontSize = 14.sp, color = colors.textPrimary)
                                Text(
                                    correction.recognizedWord,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.background(colors.uncertainSoft, RoundedCornerShape(3.dp)).padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.correct_fragment_glossary_suggestions_section),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        correction.suggestions.forEachIndexed { i, s ->
                            val selected = i == selectedSuggestion
                            val bg by animateColorAsState(
                                targetValue = if (selected) colors.accentSoft else colors.neutralSoft,
                                animationSpec = tween(200),
                                label = "suggestion-bg"
                            )
                            val fg by animateColorAsState(
                                targetValue = if (selected) colors.accentText else colors.textSecondary,
                                animationSpec = tween(200),
                                label = "suggestion-fg"
                            )
                            Box(
                                modifier = Modifier
                                    .background(bg, RoundedCornerShape(999.dp))
                                    .clickable { selectedSuggestion = i }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(text = s, fontSize = 14.sp, color = fg)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Contribute to glossary Card
                    GlifoCard(
                        backgroundColor = colors.accentFaint,
                        borderColor = colors.accentLine
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(colors.accent, RoundedCornerShape(4.dp))
                                    .clickable { contributeToGlossary = !contributeToGlossary },
                                contentAlignment = Alignment.Center
                            ) {
                                if (contributeToGlossary) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
                                }
                            }
                            Column {
                                Text(stringResource(R.string.correct_fragment_contribute_glossary_title), fontSize = 15.sp, color = colors.textPrimary)
                                Text(stringResource(R.string.correct_fragment_contribute_glossary_description), fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.confidence.GlossaryMatcher
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.NoteContentViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

/**
 * Corrección manual de un fragmento (#20). Se abre con el fragmento tocado o, si no se pidió
 * ninguno, con el más dudoso del apunte. Las sugerencias salen del glosario del curso
 * guardado en el teléfono (#27), así que funcionan sin conexión.
 */
@Composable
fun CorrectFragmentScreen(
    noteId: Long,
    fragmentId: String?,
    onNavigateBack: () -> Unit,
    onSaveCorrection: () -> Unit,
    viewModel: NoteContentViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val data = (uiState as? UiState.Success)?.data
    val fragment = data?.content?.let { it.fragment(fragmentId) ?: it.weakestFragment }
    var edited by remember(fragment?.id) { mutableStateOf(fragment?.displayText.orEmpty()) }

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
                if (fragment != null) {
                    TextButton(
                        enabled = edited.isNotBlank() && edited.trim() != fragment.displayText,
                        onClick = { viewModel.saveCorrection(noteId, fragment.id, edited, onSaveCorrection) }
                    ) {
                        Text(stringResource(R.string.correct_fragment_save), color = colors.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(noteId, force = true) })
            is UiState.Success -> {
                if (fragment == null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.correct_fragment_nothing_title), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(
                            stringResource(R.string.correct_fragment_nothing_description),
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    return@Column
                }

                val content = state.data.content
                val page = content.page(fragment.pageIndex)
                val suggestions = remember(fragment.id, state.data.glossaryTerms) {
                    GlossaryMatcher.suggestions(fragment.text, state.data.glossaryTerms)
                }

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
                        GlifoConfidenceChip(state = fragment.state)
                        Text(
                            stringResource(
                                R.string.correct_fragment_confidence_threshold,
                                formatConfidence(fragment.confidence),
                                formatConfidence(content.threshold)
                            ),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    RegionCropImage(
                        photoPath = page?.photoPath,
                        bounds = fragment.bounds,
                        analyzedWidth = page?.analyzedWidth ?: 0,
                        analyzedHeight = page?.analyzedHeight ?: 0,
                        placeholder = stringResource(R.string.correct_fragment_original_crop_placeholder)
                    )

                    Text(
                        text = stringResource(
                            R.string.correct_fragment_region_counter,
                            content.fragments.indexOf(fragment) + 1,
                            content.fragments.size
                        ),
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )

                    SectionLabel(stringResource(R.string.correct_fragment_engine_read_section))
                    GlifoCard {
                        Text(stringResource(R.string.correct_fragment_recognized_text_label), fontSize = 11.sp, color = colors.accentText, modifier = Modifier.padding(bottom = 4.dp))
                        Text(fragment.text.ifBlank { stringResource(R.string.note_detail_unreadable_fragment) }, fontSize = 14.sp, color = colors.textPrimary)
                    }

                    SectionLabel(stringResource(R.string.correct_fragment_input_label), top = 18.dp)
                    OutlinedTextField(
                        value = edited,
                        onValueChange = { edited = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    saveError?.let { error ->
                        Text(error.toMessage(context), fontSize = 12.sp, color = colors.alert, modifier = Modifier.padding(top = 6.dp))
                    }

                    SectionLabel(stringResource(R.string.correct_fragment_glossary_suggestions_section), top = 18.dp)
                    if (suggestions.isEmpty()) {
                        Text(stringResource(R.string.correct_fragment_no_suggestions), fontSize = 12.sp, color = colors.textSecondary)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestions.forEach { term ->
                                Box(
                                    modifier = Modifier
                                        .background(colors.accentSoft, RoundedCornerShape(999.dp))
                                        .clickable { edited = replaceClosestWord(edited, term) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(text = term, fontSize = 14.sp, color = colors.accentText)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(stringResource(R.string.correct_fragment_saved_locally), fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = GlifoTheme.colors.textSecondary,
        modifier = Modifier.padding(top = top, bottom = 8.dp)
    )
}

/** Reemplaza la palabra del texto que más se parece al término elegido. */
private fun replaceClosestWord(text: String, term: String): String {
    val words = text.split(" ")
    if (words.isEmpty()) return term
    val closest = words.indices.maxByOrNull { GlossaryMatcher.similarity(words[it].lowercase(), term.lowercase()) } ?: return term
    return words.toMutableList().also { it[closest] = term }.joinToString(" ")
}

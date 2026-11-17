package cr.ac.una.glifo.presentation.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.common.asString
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.NoteContentSource
import cr.ac.una.glifo.presentation.viewmodel.NoteContentViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.delay

/**
 * Visor de fórmulas (#40): muestra el recorte original y la transcripción LaTeX cuando
 * la región ya fue procesada por N1.5/N2 o proviene de un bloque FORMULA reconstruido,
 * junto con el indicador `latexCompiles` (validado en el backend con JLaTeXMath).
 * Solo muestra el aviso de conversión pendiente si realmente aún no existe LaTeX.
 */
@Composable
fun FormulaViewerScreen(
    noteId: Long,
    fragmentId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToCorrect: (fragmentId: String?) -> Unit,
    viewModel: NoteContentViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val content = (uiState as? UiState.Success)?.data?.content
    val formula = content?.let { it.fragment(fragmentId) ?: it.firstFormula }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.formula_viewer_title, formula?.id ?: ""),
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                if (formula != null) {
                    GlifoConfidenceChip(state = formula.state)
                }
            }
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(noteId, force = true) })
            is UiState.Success -> {
                if (formula == null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.formula_viewer_none_title), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(
                            stringResource(R.string.formula_viewer_none_description),
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    return@Column
                }

                val page = state.data.content.page(formula.pageIndex)
                val resolvedLatex = formula.correctedText?.takeIf {
                    it.isNotBlank() && (formula.hasLatex || LatexHtmlBuilder.containsLatex(it))
                }
                    ?: formula.latex?.takeIf { it.isNotBlank() }
                    ?: formula.displayText.takeIf {
                        state.data.content.source == NoteContentSource.ServerReconstruction && it.isNotBlank()
                    }
                val hasLatex = !resolvedLatex.isNullOrBlank()
                val compilesValue: Boolean? = formula.latexCompiles ?: if (hasLatex) true else null
                var showRawLatex by remember(formula.id) { mutableStateOf(false) }
                var copied by remember(formula.id) { mutableStateOf(false) }
                LaunchedEffect(copied) {
                    if (copied) {
                        delay(2000)
                        copied = false
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Label(stringResource(R.string.formula_viewer_crop_section))
                    RegionCropImage(
                        photoPath = page?.photoPath,
                        bounds = formula.bounds,
                        analyzedWidth = page?.analyzedWidth ?: 0,
                        analyzedHeight = page?.analyzedHeight ?: 0,
                        placeholder = stringResource(R.string.formula_viewer_photo_placeholder),
                        height = 120.dp
                    )

                    Label(
                        text = stringResource(
                            if (hasLatex) R.string.formula_viewer_transcription_section
                            else R.string.formula_viewer_ocr_read_section
                        ),
                        top = 16.dp
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(vertical = 16.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasLatex) {
                            val latexSource = resolvedLatex
                            val toggleDescription = stringResource(R.string.formula_viewer_toggle_latex_description)
                            val copyDescription = stringResource(R.string.formula_viewer_copy_latex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.formula_viewer_latex_generated),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (showRawLatex) colors.accentSoft else colors.surface)
                                        .border(
                                            1.dp,
                                            if (showRawLatex) colors.accentLine else colors.border,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { showRawLatex = !showRawLatex }
                                        .semantics { contentDescription = toggleDescription }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = if (showRawLatex) colors.accentText else colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.formula_viewer_toggle_latex),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (showRawLatex) colors.accentText else colors.textSecondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.surface)
                                        .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                                        .clickable {
                                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("LaTeX", latexSource))
                                            copied = true
                                        }
                                        .semantics { contentDescription = copyDescription }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = if (copied) colors.verified else colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    if (copied) {
                                        Text(
                                            text = stringResource(R.string.formula_viewer_copied),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.verified
                                        )
                                    }
                                }
                            }
                            LatexFormula(
                                latex = latexSource,
                                mode = LatexRenderMode.Formula,
                                fontSize = 20.sp,
                                textColor = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                allowHorizontalScroll = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (showRawLatex) {
                                HorizontalDivider(
                                    color = colors.border,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                SelectionContainer {
                                    Text(
                                        text = latexSource,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = formula.displayText
                                    .ifBlank { stringResource(R.string.note_detail_unreadable_fragment) },
                                fontFamily = FontFamily.Default,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (!hasLatex) {
                        Spacer(modifier = Modifier.height(12.dp))

                        GlifoCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.formula_viewer_latex_pending), fontSize = 13.sp, color = colors.textSecondary, lineHeight = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlifoCard(tight = true) {
                        if (compilesValue != null) {
                            GlifoItem(compact = true) {
                                Icon(
                                    imageVector = if (compilesValue) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (compilesValue) colors.verified else colors.uncertain,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.formula_viewer_compiles_jlatex),
                                    fontSize = 15.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = compilesValue.toString(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    color = if (compilesValue) colors.verified else colors.uncertain
                                )
                            }
                        }
                        GlifoItem(compact = true) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.formula_viewer_engine_confidence), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(formatConfidence(formula.confidence), fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = colors.textSecondary)
                        }
                        formula.kindConfidence?.let { kindConfidence ->
                            GlifoItem(compact = true) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.formula_viewer_kind_confidence), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                Text(formatConfidence(kindConfidence), fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = colors.textSecondary)
                            }
                        }
                        GlifoItem(compact = true, showBottomBorder = false) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.formula_viewer_route), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(formula.routeText.asString(), fontSize = 13.sp, color = colors.textSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlifoButton(
                            text = stringResource(R.string.formula_viewer_correct),
                            variant = GlifoButtonVariant.Secondary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToCorrect(formula.id) }
                        )
                        GlifoButton(
                            text = stringResource(R.string.formula_viewer_looks_good),
                            icon = Icons.Default.Check,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateBack
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = GlifoTheme.colors.textSecondary,
        modifier = Modifier.padding(top = top, bottom = 8.dp)
    )
}

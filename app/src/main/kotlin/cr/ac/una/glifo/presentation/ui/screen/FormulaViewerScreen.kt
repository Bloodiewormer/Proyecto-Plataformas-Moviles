package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import cr.ac.una.glifo.presentation.viewmodel.NoteContentViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun FormulaViewerScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCorrect: () -> Unit,
    viewModel: NoteContentViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var copied by remember(noteId) { mutableStateOf(false) }

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val formula = (uiState as? UiState.Success)?.data?.formula
        GlifoTopAppBar(
            title = stringResource(R.string.formula_viewer_title, formula?.regionLabel ?: ""),
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                if (formula != null) {
                    GlifoBadge(
                        text = formula.tier,
                        backgroundColor = colors.repairedSoft,
                        textColor = colors.repaired
                    )
                }
            }
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(noteId) })
            is UiState.Success -> {
                val f = state.data.formula

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.formula_viewer_crop_section),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.formula_viewer_photo_placeholder), fontSize = 12.sp, color = colors.textSecondary)
                    }

                    Text(
                        text = stringResource(R.string.formula_viewer_transcription_section),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(vertical = 22.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = f.display,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // LaTeX Card
                    GlifoCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (copied) stringResource(R.string.formula_viewer_copied) else stringResource(R.string.formula_viewer_latex_generated),
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.formula_viewer_copy_description),
                                tint = colors.accentText,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        clipboard.setText(AnnotatedString(f.latex))
                                        copied = true
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = f.latex,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlifoCard(tight = true) {
                        GlifoItem(compact = true) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.formula_viewer_compiles_jlatex), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        }
                        GlifoItem(compact = true) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.formula_viewer_engine_confidence), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text("${f.confidence}", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = colors.textSecondary)
                        }
                        GlifoItem(compact = true, showBottomBorder = false) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.formula_viewer_resolved_by), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(f.engine, fontSize = 14.sp, color = colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlifoButton(
                            text = stringResource(R.string.formula_viewer_correct),
                            variant = GlifoButtonVariant.Secondary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCorrect
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

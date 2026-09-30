package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import cr.ac.una.glifo.domain.model.NoteFragment
import cr.ac.una.glifo.domain.model.RegionKind
import cr.ac.una.glifo.presentation.viewmodel.NoteContentViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

private enum class LadderState { Done, Pending, Skipped }

private data class LadderStep(val label: String, val state: LadderState)

/**
 * De dónde salió un fragmento: su confianza contra el umbral, las señales que la
 * produjeron y por qué niveles de la escalera pasó (o por cuáles todavía no).
 */
@Composable
fun RegionProvenanceScreen(
    noteId: Long,
    fragmentId: String?,
    onNavigateBack: () -> Unit,
    viewModel: NoteContentViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.region_provenance_title),
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(noteId, force = true) })
            is UiState.Success -> {
                val content = state.data.content
                val fragment = content.fragment(fragmentId) ?: content.weakestFragment ?: content.fragments.firstOrNull()
                if (fragment == null) {
                    Text(
                        stringResource(R.string.note_detail_unavailable_title),
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                    return@Column
                }
                val page = content.page(fragment.pageIndex)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    RegionCropImage(
                        photoPath = page?.photoPath,
                        bounds = fragment.bounds,
                        analyzedWidth = page?.analyzedWidth ?: 0,
                        analyzedHeight = page?.analyzedHeight ?: 0,
                        placeholder = stringResource(R.string.region_provenance_crop_placeholder),
                        height = 96.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GlifoCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlifoConfidenceChip(state = fragment.state)
                            Text(formatConfidence(fragment.confidence), fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = colors.textPrimary)
                        }
                        Text(
                            stringResource(R.string.region_provenance_threshold_configured, formatConfidence(content.threshold)),
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            fragment.displayText.ifBlank { stringResource(R.string.note_detail_unreadable_fragment) },
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    SectionTitle(stringResource(R.string.region_provenance_how_calculated_section), top = 16.dp)

                    val factors = factorsFor(fragment, content.threshold)
                    if (factors.isEmpty()) {
                        Text(stringResource(R.string.region_provenance_server_note), fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp)
                    } else {
                        GlifoCard {
                            factors.forEach { (label, value, strong) -> FactorRow(label, value, strong) }
                        }
                    }

                    SectionTitle(stringResource(R.string.region_provenance_pipeline_section), top = 18.dp)
                    val ladder = ladderFor(fragment, content.threshold)
                    GlifoCard(tight = true) {
                        ladder.forEachIndexed { i, step ->
                            GlifoItem(compact = true, showBottomBorder = i < ladder.size - 1) {
                                val (icon, tint) = when (step.state) {
                                    LadderState.Done -> Icons.Default.Check to colors.verified
                                    LadderState.Pending -> Icons.Default.HourglassTop to colors.escalated
                                    LadderState.Skipped -> Icons.Default.Close to colors.textSecondary
                                }
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                                Text(
                                    step.label,
                                    fontSize = 15.sp,
                                    color = if (step.state == LadderState.Skipped) colors.textSecondary else colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    stringResource(
                                        when (step.state) {
                                            LadderState.Done -> R.string.region_provenance_state_done
                                            LadderState.Pending -> R.string.region_provenance_state_pending
                                            LadderState.Skipped -> R.string.region_provenance_state_skipped
                                        }
                                    ),
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun factorsFor(fragment: NoteFragment, threshold: Float): List<Triple<String, Float, Boolean>> {
    // Solo las páginas procesadas en el teléfono traen las señales por región.
    val metrics = fragment.metrics ?: return emptyList()
    val kindConfidence = fragment.kindConfidence ?: return emptyList()
    return listOf(
        Triple(stringResource(R.string.region_provenance_factor_ocr), fragment.confidence, fragment.confidence >= threshold),
        Triple(stringResource(R.string.region_provenance_factor_kind), kindConfidence, kindConfidence >= 0.5f),
        Triple(stringResource(R.string.region_provenance_factor_ink), metrics.inkDensity.coerceIn(0f, 1f), metrics.inkDensity > 0.02f)
    )
}

@Composable
private fun ladderFor(fragment: NoteFragment, threshold: Float): List<LadderStep> {
    val belowGate = fragment.kind == RegionKind.Text && fragment.confidence < threshold && !fragment.isCorrected
    return listOf(
        LadderStep(stringResource(R.string.region_provenance_step_n0), LadderState.Done),
        LadderStep(stringResource(R.string.region_provenance_step_n1), LadderState.Done),
        LadderStep(stringResource(R.string.region_provenance_step_classifier, fragment.kind.label), LadderState.Done),
        LadderStep(
            stringResource(R.string.region_provenance_step_n15),
            if (fragment.kind == RegionKind.Math) LadderState.Pending else LadderState.Skipped
        ),
        LadderStep(
            stringResource(R.string.region_provenance_step_n2),
            if (fragment.kind == RegionKind.Drawing || belowGate) LadderState.Pending else LadderState.Skipped
        )
    )
}

@Composable
private fun FactorRow(label: String, value: Float, strong: Boolean) {
    val colors = GlifoTheme.colors
    val animated by animateFloatAsState(targetValue = value, animationSpec = tween(500), label = "provenance-factor")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.surfaceHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .background(if (strong) colors.verified else colors.uncertain)
            )
        }
        Text(
            text = formatConfidence(value),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Right
        )
    }
}

@Composable
private fun SectionTitle(text: String, top: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = GlifoTheme.colors.textSecondary,
        modifier = Modifier.padding(top = top, bottom = 8.dp)
    )
}

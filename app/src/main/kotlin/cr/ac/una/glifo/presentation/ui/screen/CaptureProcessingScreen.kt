package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.pipeline.RegionClassifier
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionKind
import cr.ac.una.glifo.domain.model.routeRes
import cr.ac.una.glifo.presentation.viewmodel.CaptureProcessingViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CaptureProcessingScreen(
    photoPath: String? = null,
    courseId: Long? = null,
    onNavigateBack: () -> Unit,
    onViewResults: (noteId: Long) -> Unit,
    viewModel: CaptureProcessingViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(photoPath, courseId) { viewModel.load(photoPath, courseId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.capture_processing_title),
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                GlifoBadge(
                    text = stringResource(R.string.capture_processing_zero_paid_calls),
                    backgroundColor = colors.verifiedSoft,
                    textColor = colors.verified
                )
            }
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::retry)
            is UiState.Success -> {
                val steps = state.data.steps
                val analysis = state.data.analysis
                val revealTarget = steps.count { it.state != "pending" }

                // Steps reveal themselves over time instead of appearing pre-finished, so the
                // "processing" screen actually reads as processing.
                var revealedCount by remember { mutableStateOf(0) }
                LaunchedEffect(steps) {
                    while (revealedCount < revealTarget) {
                        delay(450)
                        revealedCount++
                    }
                }

                val progress by animateFloatAsState(
                    targetValue = revealedCount.toFloat() / steps.size,
                    animationSpec = tween(400),
                    label = "capture-progress"
                )

                fun stateFor(index: Int): String = when {
                    index < revealedCount - 1 -> "done"
                    index == revealedCount - 1 -> steps[index].state
                    else -> "pending"
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    // Processing Summary Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp, 74.dp)
                                .background(colors.surfaceHigh, RoundedCornerShape(6.dp))
                                .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.capture_processing_page_label), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text(
                                text = if (analysis != null) {
                                    stringResource(
                                        R.string.capture_processing_summary,
                                        analysis.regionCount,
                                        analysis.totalMillis
                                    )
                                } else {
                                    stringResource(R.string.capture_processing_no_photo)
                                },
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.surfaceHigh)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .background(colors.accent)
                                )
                            }
                        }
                    }

                    // Steps Timeline — the full path is always visible; each dot lights up in
                    // turn as `revealedCount` advances, so upcoming steps read as "pending"
                    // rather than popping in out of nowhere.
                    steps.forEachIndexed { i, step ->
                        val stepState = stateFor(i)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(22.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (stepState == "done") colors.verified else Color.Transparent)
                                        .border(
                                            2.dp,
                                            if (stepState == "done") colors.verified else if (stepState == "now") colors.accent else colors.border,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (stepState == "done") {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.background, modifier = Modifier.size(13.dp))
                                    } else {
                                        Text(
                                            text = "${i + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (stepState == "now") colors.accentText else colors.textSecondary
                                        )
                                    }
                                }
                                if (i < steps.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(36.dp)
                                            .background(if (stepState == "done") colors.verified else colors.border)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = 14.dp)
                            ) {
                                Text(
                                    text = step.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (stepState == "now") colors.accentText else if (stepState == "pending") colors.textSecondary else colors.textPrimary
                                )
                                Text(
                                    text = step.description,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Real output of the local pipeline: what each fragment turned out to be
                    // and where the router sent it. Shown once the classifier step (4th, after
                    // dedup, N0 and N1) has been revealed.
                    if (analysis != null && analysis.regions.isNotEmpty() && revealedCount >= 4) {
                        RegionRoutingSection(analysis)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ver resultados guarda la captura como apunte (#25) y la encola (#26).
                    val saving by viewModel.saving.collectAsState()
                    val saveError by viewModel.saveError.collectAsState()
                    saveError?.let { error ->
                        Text(error.toMessage(context), fontSize = 12.sp, color = colors.alert, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    GlifoButton(
                        text = stringResource(R.string.capture_processing_view_results),
                        enabled = revealedCount >= revealTarget && !saving && photoPath != null && courseId != null,
                        onClick = {
                            if (photoPath != null && courseId != null) viewModel.saveAsNote(photoPath, courseId, onViewResults)
                        }
                    )
                }
            }
        }
    }
}

/** Desglose por región: tipo asignado, destino del enrutador y lo que leyó N1. */
@Composable
private fun RegionRoutingSection(analysis: PageAnalysis) {
    val colors = GlifoTheme.colors

    Text(
        text = stringResource(R.string.capture_processing_regions_section),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GlifoBadge(
            text = stringResource(R.string.capture_processing_text_count, analysis.textCount),
            backgroundColor = colors.verifiedSoft,
            textColor = colors.verified
        )
        GlifoBadge(
            text = stringResource(R.string.capture_processing_math_count, analysis.mathCount),
            backgroundColor = colors.repairedSoft,
            textColor = colors.repaired
        )
        GlifoBadge(
            text = stringResource(R.string.capture_processing_drawing_count, analysis.drawingCount),
            backgroundColor = colors.escalatedSoft,
            textColor = colors.escalated
        )
    }

    analysis.regions.forEach { region ->
        GlifoCard(modifier = Modifier.padding(bottom = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlifoConfidenceChip(state = region.confidenceState)
                Text(
                    text = stringResource(R.string.capture_processing_region_label, region.index + 1, stringResource(region.kind.labelRes)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(region.ocrConfidence * 100).roundToInt()}%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = confidenceColor(region)
                )
            }

            Text(
                text = stringResource(region.routeRes(RegionClassifier.TEXT_CONFIDENCE_GATE)),
                fontSize = 10.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (region.text.isNotBlank()) {
                Text(
                    text = region.text,
                    fontSize = 12.sp,
                    color = colors.textPrimary,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.capture_processing_no_legible_text,
                        region.metrics.componentCount,
                        (region.metrics.inkDensity * 100).roundToInt()
                    ),
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun confidenceColor(region: RecognizedRegion): Color {
    val colors = GlifoTheme.colors
    return when (region.kind) {
        RegionKind.Math -> colors.repaired
        RegionKind.Drawing -> colors.escalated
        RegionKind.Text -> if (region.ocrConfidence >= 0.75f) colors.verified else colors.alert
    }
}

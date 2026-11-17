package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.NoteContentSource
import cr.ac.una.glifo.domain.model.NoteFragment
import cr.ac.una.glifo.domain.model.RegionKind
import cr.ac.una.glifo.presentation.viewmodel.NoteContentViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import cr.ac.una.glifo.presentation.viewmodel.toReconstructMessage

/**
 * Resultado de una captura: cada fragmento que leyó el pipeline (o la reconstrucción del
 * servidor), marcado con su estado de confianza. Tocar un fragmento abre de dónde salió;
 * las fórmulas abren el visor y los dudosos, la corrección.
 */
@Composable
fun NoteDetailScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCorrect: (fragmentId: String?) -> Unit,
    onNavigateToFormula: (fragmentId: String?) -> Unit,
    onNavigateToProvenance: (fragmentId: String?) -> Unit,
    viewModel: NoteContentViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(noteId) { viewModel.load(noteId, force = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val content = (uiState as? UiState.Success)?.data?.content
        GlifoTopAppBar(
            title = content?.note?.title ?: stringResource(R.string.note_detail_default_title),
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                if (content != null && content.fragments.isNotEmpty()) {
                    IconButton(onClick = { onNavigateToProvenance(content.weakestFragment?.id) }) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = stringResource(R.string.note_detail_provenance_description), tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { onNavigateToCorrect(content.weakestFragment?.id) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.note_detail_edit_description), tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(noteId, force = true) })
            is UiState.Success -> {
                val uiData = state.data
                val data = uiData.content
                val errorText = uiData.reconstructError?.toReconstructMessage(context)
                val topicSection: @Composable () -> Unit = {
                    NoteTopicSection(
                        linkedTopicId = data.note.syllabusTopicId,
                        suggestedTopicId = data.suggestedTopicId,
                        courseTopics = uiData.courseTopics,
                        isTopicsLoading = uiData.isTopicsLoading,
                        isUpdatingTopic = uiData.isUpdatingTopic,
                        topicError = uiData.topicError,
                        onSelectTopic = { topicId -> viewModel.updateTopic(noteId, topicId) },
                        onAcceptSuggestion = { viewModel.acceptSuggestedTopic(noteId) },
                        onRefreshTopics = { viewModel.loadCourseTopics(data.note.courseId, force = true) },
                        onClearError = { viewModel.clearTopicError() }
                    )
                }
                if (data.source == NoteContentSource.Unavailable) {
                    UnavailableContent(
                        pendingSync = data.hasPendingSync,
                        canReconstruct = data.canReconstruct,
                        isReconstructing = uiData.isReconstructing,
                        reconstructError = errorText,
                        onReconstruct = { viewModel.reconstructNote(noteId) },
                        topicSection = topicSection
                    )
                } else {
                    NoteContentBody(
                        content = data,
                        isReconstructing = uiData.isReconstructing,
                        reconstructError = errorText,
                        onReconstruct = { viewModel.reconstructNote(noteId) },
                        topicSection = topicSection,
                        onFragmentClick = { fragment ->
                            when {
                                fragment.kind == RegionKind.Math -> onNavigateToFormula(fragment.id)
                                fragment.state == ConfidenceState.Uncertain && !fragment.isCorrected -> onNavigateToCorrect(fragment.id)
                                else -> onNavigateToProvenance(fragment.id)
                            }
                        },
                        onReviewWeakest = { onNavigateToCorrect(data.weakestFragment?.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.NoteContentBody(
    content: NoteContent,
    isReconstructing: Boolean,
    reconstructError: String?,
    onReconstruct: () -> Unit,
    topicSection: @Composable () -> Unit,
    onFragmentClick: (NoteFragment) -> Unit,
    onReviewWeakest: () -> Unit
) {
    val colors = GlifoTheme.colors

    GlifoBanner(
        text = "",
        content = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                GlifoConfidenceChip(state = ConfidenceState.Verified)
                GlifoConfidenceChip(state = ConfidenceState.Repaired)
                GlifoConfidenceChip(state = ConfidenceState.Escalated)
                GlifoConfidenceChip(state = ConfidenceState.Uncertain)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(
                    R.string.note_detail_summary_counts,
                    content.verifiedCount,
                    content.repairedCount,
                    content.escalatedCount,
                    content.uncertainCount
                ),
                fontSize = 12.sp,
                color = colors.textPrimary
            )
        }
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Text(
                text = when (content.source) {
                    NoteContentSource.LocalPipeline -> stringResource(
                        R.string.note_detail_page_label_local,
                        content.pages.size,
                        content.fragments.size
                    )
                    else -> stringResource(R.string.note_detail_page_label_server)
                },
                fontSize = 11.sp,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            if (content.hasPendingSync) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.note_detail_pending_sync), fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(start = 4.dp))
            }
        }

        topicSection()
        Spacer(modifier = Modifier.height(12.dp))

        ReconstructAiControls(
            canReconstruct = content.canReconstruct,
            isReconstructing = isReconstructing,
            reconstructError = reconstructError,
            onReconstruct = onReconstruct
        )

        // Si ninguna región tiene texto, la lista de "[no se reconoció texto]" no dice qué hacer.
        if (content.fragments.isNotEmpty() && content.fragments.all { it.displayText.isBlank() }) {
            GlifoBanner(
                text = stringResource(R.string.note_detail_all_unreadable_hint),
                isWarning = true,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        content.fragments.forEach { fragment ->
            if (fragment.kind == RegionKind.Math) {
                FormulaFragment(
                    fragment = fragment,
                    resolved = content.source == NoteContentSource.ServerReconstruction || fragment.hasLatex,
                    onClick = { onFragmentClick(fragment) }
                )
            } else {
                TextFragment(fragment, onClick = { onFragmentClick(fragment) })
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        val pendingReview = content.fragments.count { it.state == ConfidenceState.Uncertain && !it.isCorrected }
        if (pendingReview > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            GlifoCard(
                backgroundColor = colors.uncertainFaint,
                borderColor = colors.uncertainLine,
                onClick = onReviewWeakest
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                    Text(
                        pluralStringResource(R.plurals.note_detail_fragments_to_review, pendingReview, pendingReview),
                        fontSize = 13.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReconstructAiControls(
    canReconstruct: Boolean,
    isReconstructing: Boolean,
    reconstructError: String?,
    onReconstruct: () -> Unit
) {
    val colors = GlifoTheme.colors
    if (canReconstruct || isReconstructing) {
        GlifoButton(
            text = if (isReconstructing) {
                stringResource(R.string.note_detail_reconstructing_ai)
            } else {
                stringResource(R.string.note_detail_reconstruct_button)
            },
            enabled = !isReconstructing,
            onClick = onReconstruct,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }

    if (isReconstructing) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colors.accent
            )
            Text(
                text = stringResource(R.string.note_detail_reconstructing_ai),
                fontSize = 14.sp,
                color = colors.textSecondary
            )
        }
    }

    if (reconstructError != null) {
        GlifoBanner(
            text = reconstructError,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

/**
 * Codificación doble del mapa de confianza (Glifo_Alcance.md §1): además del color, el
 * subrayado es continuo o punteado y el estado aparece como texto en el chip.
 */
@Composable
private fun TextFragment(fragment: NoteFragment, onClick: () -> Unit) {
    val colors = GlifoTheme.colors
    val (lineColor, fill) = when (fragment.state) {
        ConfidenceState.Verified -> colors.verified to null
        ConfidenceState.Repaired -> colors.repaired to colors.repairedSoft
        ConfidenceState.Escalated -> colors.escalated to colors.escalatedFaint
        ConfidenceState.Uncertain -> colors.uncertain to colors.uncertainSoft
    }
    val dotted = fragment.state == ConfidenceState.Uncertain

    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(
            text = fragment.displayText.ifBlank { stringResource(R.string.note_detail_unreadable_fragment) },
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = colors.textPrimary,
            modifier = Modifier
                .then(if (fill != null) Modifier.background(fill, RoundedCornerShape(3.dp)) else Modifier)
                .drawBehind {
                    val y = size.height
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = if (dotted) PathEffect.dashPathEffect(floatArrayOf(4f, 6f)) else null
                    )
                }
                .padding(horizontal = 2.dp, vertical = 2.dp)
        )
        if (fragment.state != ConfidenceState.Verified || fragment.isCorrected) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                GlifoConfidenceChip(state = fragment.state)
                Text(
                    text = if (fragment.isCorrected) stringResource(R.string.note_detail_corrected_badge) else fragment.routeText.asString(),
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FormulaFragment(fragment: NoteFragment, resolved: Boolean, onClick: () -> Unit) {
    val colors = GlifoTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
            .border(1.dp, colors.border, RoundedCornerShape(7.dp))
            .drawBehind {
                drawLine(colors.repaired, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 6.dp.toPx())
            }
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                GlifoBadge(
                    text = stringResource(if (resolved) R.string.note_detail_formula_badge_resolved else R.string.note_detail_formula_badge),
                    backgroundColor = colors.repairedSoft,
                    textColor = colors.repaired
                )
                Text(
                    text = stringResource(R.string.note_detail_confidence_label, formatConfidence(fragment.confidence)),
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
            Text(
                text = fragment.displayText.ifBlank { stringResource(R.string.note_detail_unreadable_fragment) },
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            if (fragment.bounds != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                    Text(stringResource(R.string.note_detail_view_original_crop), fontSize = 11.sp, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun UnavailableContent(
    pendingSync: Boolean,
    canReconstruct: Boolean,
    isReconstructing: Boolean,
    reconstructError: String?,
    onReconstruct: () -> Unit,
    topicSection: @Composable () -> Unit
) {
    val colors = GlifoTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        topicSection()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.note_detail_unavailable_title), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, textAlign = TextAlign.Center)
            Text(
                stringResource(if (pendingSync) R.string.note_detail_pending_sync else R.string.note_detail_unavailable_description),
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            ReconstructAiControls(
                canReconstruct = canReconstruct,
                isReconstructing = isReconstructing,
                reconstructError = reconstructError,
                onReconstruct = onReconstruct
            )
        }
    }
}

internal fun formatConfidence(value: Float): String = String.format(java.util.Locale.US, "%.2f", value)

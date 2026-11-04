package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.presentation.viewmodel.CoverageDeltaViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun CoverageDeltaScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotes: () -> Unit,
    viewModel: CoverageDeltaViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.course_detail_whats_changed),
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
            is UiState.Success -> {
                val delta = state.data
                val solidCount = delta.changedCounts["SOLID"] ?: 0
                val partialCount = delta.changedCounts["PARTIAL"] ?: 0
                val absentCount = delta.changedCounts["ABSENT"] ?: 0
                val uncertainCount = delta.changedCounts["UNCERTAIN"] ?: 0

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    GlifoCard {
                        Text(delta.dateRange, fontSize = 12.sp, color = colors.textSecondary)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.coverage_delta_changed_count, delta.changedTopics.size),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )

                        if (delta.changedTopics.isNotEmpty()) {
                            CovBar(
                                v = delta.toBar.v,
                                r = delta.toBar.r,
                                e = delta.toBar.e,
                                u = delta.toBar.u,
                                height = 8.dp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GlifoConfidenceChip(state = ConfidenceState.Verified)
                                        Text(stringResource(R.string.coverage_solid_count, solidCount), fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GlifoConfidenceChip(state = ConfidenceState.Escalated)
                                        Text(stringResource(R.string.coverage_absent_count, absentCount), fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GlifoConfidenceChip(state = ConfidenceState.Repaired)
                                        Text(stringResource(R.string.coverage_partial_count, partialCount), fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                                        Text(stringResource(R.string.coverage_doubtful_count, uncertainCount), fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }

                    if (delta.changedTopics.isEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        GlifoBanner(
                            text = stringResource(R.string.coverage_delta_empty_since, delta.sinceDisplay)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.coverage_delta_changed_topics_section),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        GlifoCard(tight = true) {
                            delta.changedTopics.forEachIndexed { i, topic ->
                                GlifoItem(compact = true, showBottomBorder = i < delta.changedTopics.size - 1) {
                                    GlifoConfidenceChip(state = topic.state)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = topic.name, fontSize = 14.sp, color = colors.textPrimary)
                                        if (topic.note != null) {
                                            Text(text = topic.note, fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                    }
                                    Text(
                                        text = topic.score,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    GlifoButton(
                        text = stringResource(R.string.course_detail_my_notes),
                        variant = GlifoButtonVariant.Secondary,
                        onClick = onNavigateToNotes
                    )
                }
            }
        }
    }
}

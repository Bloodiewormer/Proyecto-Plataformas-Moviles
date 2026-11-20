package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import cr.ac.una.glifo.core.common.asString
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.TopicDetailViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun TopicDetailScreen(
    topicName: String,
    onNavigateBack: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
    viewModel: TopicDetailViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(topicName) { viewModel.load(topicName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = (uiState as? UiState.Success)?.data?.name ?: topicName,
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(topicName) })
            is UiState.Success -> {
                val detail = state.data

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    GlifoCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlifoConfidenceChip(state = detail.state)
                            GlifoBadge(text = detail.stateLabelText.asString(), backgroundColor = colors.repairedSoft, textColor = colors.repaired)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(detail.confidence, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = colors.textSecondary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = detail.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp,
                            color = colors.textPrimary
                        )
                    }

                    // La sección de checklist por subcriterio se muestra solo si el backend provee ítems reales.
                    if (detail.checklist.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.topic_detail_why_section),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        GlifoCard {
                            detail.checklist.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.ok) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (item.ok) colors.verified else colors.escalated,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(text = item.label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text(text = item.meta, fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.topic_detail_appears_in_section),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    if (detail.noteRefs.isEmpty()) {
                        GlifoBanner(text = stringResource(R.string.topic_detail_no_notes))
                    } else {
                        GlifoCard(tight = true) {
                            detail.noteRefs.forEachIndexed { i, ref ->
                                GlifoItem(
                                    compact = true,
                                    showBottomBorder = i < detail.noteRefs.size - 1,
                                    onClick = { onNavigateToNoteDetail(ref.noteId) }
                                ) {
                                    GlifoThumb(size = 36.dp) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ref.title, fontSize = 14.sp, color = colors.textPrimary)
                                        Text(ref.meta, fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (cr.ac.una.glifo.core.ui.component.LocalSessionRole.current == cr.ac.una.glifo.domain.model.GlifoRole.Student) {
                        Spacer(modifier = Modifier.height(16.dp))

                        GlifoButton(
                            text = stringResource(R.string.topic_detail_generate_reinforcement),
                            icon = Icons.Default.Psychology,
                            onClick = onNavigateToStudy
                        )
                    }
                }
            }
        }
    }
}

package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
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
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.CourseDetailViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun CourseDetailScreen(
    courseId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToCoverage: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToDelta: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToSync: () -> Unit,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(courseId) { viewModel.load(courseId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = (uiState as? UiState.Success)?.data?.course?.name ?: stringResource(R.string.capture_default_course_name),
            subtitle = stringResource(R.string.course_detail_subtitle),
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                IconButton(onClick = onNavigateToCapture) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = stringResource(R.string.course_detail_capture_description), tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(courseId) })
            is UiState.Success -> {
                val course = state.data.course
                val stats = state.data.stats

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    GlifoStaggeredEntry(index = 0) {
                        // Main Course Summary Card
                        GlifoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(text = course?.name ?: stringResource(R.string.capture_default_course_name), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Text(text = course?.code ?: "—", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                }
                                Text(text = "${stats.coveragePercent}%", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                            }
                            CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 10.dp))
                            // Al estudiante no le sirve ver "0 estudiantes"; y el backend todavía no manda el nombre del docente.
                            val viewerRole = cr.ac.una.glifo.core.ui.component.LocalSessionRole.current
                            val showsStudents = viewerRole != null && viewerRole != cr.ac.una.glifo.domain.model.GlifoRole.Student && stats.studentCount > 0
                            if (stats.instructorName.isNotBlank() && showsStudents) {
                                Text(text = stringResource(R.string.course_detail_instructor_students, stats.instructorName, stats.studentCount), fontSize = 12.sp, color = colors.textSecondary)
                            } else if (showsStudents) {
                                Text(text = stringResource(R.string.teacher_gaps_student_count, stats.studentCount), fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2x2 Grid of Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlifoStaggeredEntry(index = 1, modifier = Modifier.weight(1f)) {
                            GlifoCard(
                                onClick = onNavigateToNotes,
                                modifier = Modifier.height(100.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = stringResource(R.string.course_detail_my_notes), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text(text = stringResource(R.string.course_detail_note_count, stats.noteCount), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        GlifoStaggeredEntry(index = 2, modifier = Modifier.weight(1f)) {
                            GlifoCard(
                                onClick = onNavigateToCoverage,
                                modifier = Modifier.height(100.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DonutLarge, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = stringResource(R.string.course_detail_coverage), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text(text = stringResource(R.string.course_detail_topic_count, stats.topicCount), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlifoStaggeredEntry(index = 3, modifier = Modifier.weight(1f)) {
                            GlifoCard(
                                onClick = onNavigateToStudy,
                                modifier = Modifier.height(100.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = stringResource(R.string.course_detail_study), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text(text = stringResource(R.string.course_detail_pending_count, stats.pendingCount), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        GlifoStaggeredEntry(index = 4, modifier = Modifier.weight(1f)) {
                            GlifoCard(
                                onClick = onNavigateToDelta,
                                modifier = Modifier.height(100.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = stringResource(R.string.course_detail_whats_changed), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text(text = stringResource(R.string.course_detail_weekly_delta, stats.weeklyDeltaPoints), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }

                    // Acceso Rápido
                    Text(
                        text = stringResource(R.string.course_detail_quick_access_section),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )

                    GlifoCard(tight = true) {
                        GlifoItem(compact = true, onClick = onNavigateToNotes) {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.course_detail_confidence_map), fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                        GlifoItem(compact = true, onClick = onNavigateToCapture) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.course_detail_new_capture), fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                        GlifoItem(compact = true, showBottomBorder = false, onClick = onNavigateToSync) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.settings_sync_queue), fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

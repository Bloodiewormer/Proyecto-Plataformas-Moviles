package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.CoverageViewModel
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

@Composable
fun CoverageScreen(
    courseId: Long,
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToTopicDetail: (String) -> Unit,
    onNavigateToDelta: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CoverageViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(courseId) { viewModel.load(courseId) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary,
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            ) {
                GlifoDrawerContent(
                    role = GlifoRole.Student,
                    currentScreen = "coverage",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
                    },
                    onProfileClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    onLogoutClick = {
                        scope.launch { drawerState.close() }
                        sessionViewModel.logout()
                        onLogout()
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            GlifoTopAppBar(
                title = (uiState as? UiState.Success)?.data?.course?.name ?: stringResource(R.string.capture_default_course_name),
                subtitle = stringResource(R.string.coverage_subtitle),
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = onNavigateToDelta) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = stringResource(R.string.coverage_calendar_description), tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(courseId, force = true) })
                is UiState.Success -> {
                    val data = state.data
                    val summary = data.summary
                    val topics = data.topics

                    // Banner delta
                    GlifoBanner(
                        text = "",
                        content = {
                            Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            Text(summary.deltaLabel, fontSize = 12.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.clickable(onClick = onNavigateToDelta)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        GlifoButton(
                            text = if (data.isEvaluating) {
                                stringResource(R.string.coverage_evaluating_ai)
                            } else {
                                stringResource(R.string.coverage_evaluate_button)
                            },
                            enabled = !data.isEvaluating,
                            onClick = { viewModel.evaluateCoverage(courseId) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (data.isEvaluating) {
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
                                    text = stringResource(R.string.coverage_evaluating_ai),
                                    fontSize = 14.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (data.evaluationError != null) {
                            GlifoBanner(
                                text = data.evaluationError.toMessage(context),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        GlifoStaggeredEntry(index = 0) {
                            GlifoCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.coverage_topic_count, summary.topicCount), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Text("${summary.coveragePercent}%", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                                }

                                CovBar(
                                    v = summary.v,
                                    r = summary.r,
                                    e = summary.e,
                                    u = summary.u,
                                    height = 10.dp,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            GlifoConfidenceChip(state = ConfidenceState.Verified)
                                            Text(stringResource(R.string.coverage_solid_count, summary.solidCount), fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            GlifoConfidenceChip(state = ConfidenceState.Escalated)
                                            Text(stringResource(R.string.coverage_absent_count, summary.absentCount), fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            GlifoConfidenceChip(state = ConfidenceState.Repaired)
                                            Text(stringResource(R.string.coverage_partial_count, summary.partialCount), fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                                            Text(stringResource(R.string.coverage_doubtful_count, summary.doubtfulCount), fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = summary.unitLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        GlifoStaggeredEntry(index = 1) {
                            GlifoCard(tight = true) {
                                topics.forEachIndexed { i, topic ->
                                    GlifoItem(
                                        compact = true,
                                        showBottomBorder = i < topics.size - 1,
                                        onClick = { onNavigateToTopicDetail(topic.name) }
                                    ) {
                                        GlifoConfidenceChip(state = topic.state)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = topic.name, fontSize = 13.sp, color = colors.textPrimary)
                                            if (topic.note != null) {
                                                Text(text = topic.note, fontSize = 11.sp, color = colors.textSecondary)
                                            }
                                        }
                                        Text(
                                            text = topic.score,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

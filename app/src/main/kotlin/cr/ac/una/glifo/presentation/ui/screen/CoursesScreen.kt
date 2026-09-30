package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.CoursesViewModel
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

@Composable
fun CoursesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToCourseDetail: (Long) -> Unit,
    onNavigateToJoinCourse: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CoursesViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

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
                    currentScreen = "courses",
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
                title = stringResource(R.string.courses_title),
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.common_search), tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            )

            AnimatedVisibility(visible = searchOpen) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    GlifoField(label = stringResource(R.string.courses_search_placeholder), value = query, onValueChange = { query = it }, isFocused = true)
                }
            }

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Empty -> GlifoBanner(text = stringResource(R.string.courses_empty_no_courses), modifier = Modifier.padding(14.dp))
                is UiState.Success -> {
                    val courses = state.data.filter {
                        query.isBlank() || it.course.name.contains(query, ignoreCase = true) || it.course.code.contains(query, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        if (courses.isEmpty()) {
                            GlifoBanner(text = stringResource(R.string.courses_empty_no_match))
                        }
                        courses.forEachIndexed { i, entry ->
                            val course = entry.course
                            val stats = entry.stats
                            GlifoStaggeredEntry(index = i) {
                                GlifoCard(
                                    onClick = { onNavigateToCourseDetail(course.id) },
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = course.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                        if (course.id == 1L) GlifoBadge(text = stringResource(R.string.courses_active_badge))
                                    }
                                    Text(
                                        text = stringResource(R.string.courses_code_instructor, course.code, stats.instructorName),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.courses_coverage_percent, stats.coveragePercent), fontSize = 12.sp, color = colors.textSecondary)
                                        Text(
                                            text = if (stats.weeklyDeltaPoints > 0) {
                                                stringResource(R.string.courses_delta_positive, stats.weeklyDeltaPoints)
                                            } else {
                                                stringResource(R.string.courses_delta_none)
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (stats.weeklyDeltaPoints > 0) colors.verified else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        GlifoButton(
                            text = stringResource(R.string.courses_join_with_code),
                            variant = GlifoButtonVariant.Secondary,
                            icon = Icons.Default.Add,
                            onClick = onNavigateToJoinCourse
                        )
                    }
                }
            }
        }
    }
}

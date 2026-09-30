package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.AdminHomeViewModel
import cr.ac.una.glifo.presentation.viewmodel.HomeViewModel
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.TeacherHomeViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

// ── Screen B2: Inicio Estudiante ──────────────────────────────────────────────
@Composable
fun HomeScreen(
    onNavigateToCourse: (Long) -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToCoverage: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAiCost: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToDelta: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val activeCourse = LocalActiveCourse.current
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

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
                    currentScreen = "home",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        when (route) {
                            "home" -> {}
                            "courses" -> onNavigateToCourses()
                            "notes" -> onNavigateToNotes()
                            "coverage" -> onNavigateToCoverage()
                            "study" -> onNavigateToStudy()
                            "sync" -> onNavigateToSync()
                            "ai_cost" -> onNavigateToAiCost()
                        }
                    },
                    onChangeCourseClick = {
                        scope.launch { drawerState.close() }
                        activeCourse.onChangeCourse()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = state.data
                    val course = data.course
                    val stats = data.stats
                    val recentNotes = data.recentNotes
                    val flashcardCount = 12
                    val quizCount = 1
                    val pendingSyncCount = data.pendingSyncCount

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top AppBar
                        GlifoTopAppBar(
                            title = course?.name ?: stringResource(R.string.home_default_active_course),
                            subtitle = stringResource(R.string.home_subtitle_active_course),
                            hasDropdown = true,
                            onTitleClick = activeCourse.onChangeCourse,
                            onNavigationClick = {
                                scope.launch { drawerState.open() }
                            },
                            actions = {
                                IconButton(onClick = onNavigateToDelta) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = stringResource(R.string.course_detail_whats_changed),
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        )

                        // Sync Banner
                        if (pendingSyncCount > 0) {
                            GlifoBanner(
                                text = stringResource(R.string.home_pending_captures_banner, pendingSyncCount),
                                icon = Icons.Default.Sync
                            )
                        }

                        // Body content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            // Cobertura del curso Card (solo si el módulo de cobertura respondió)
                            if (stats != null) GlifoStaggeredEntry(index = 0) {
                                GlifoCard(onClick = onNavigateToCoverage) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.home_coverage_card_title),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "${stats.coveragePercent}%",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.accentText
                                        )
                                    }

                                    CovBar(
                                        v = stats.verified,
                                        r = stats.repaired,
                                        e = stats.escalated,
                                        u = stats.uncertain,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.home_coverage_breakdown),
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                        Text(
                                            text = stringResource(R.string.home_coverage_delta_since, stats.weeklyDeltaPoints),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.verified
                                        )
                                    }
                                }
                            }

                            // Section: Pendientes de hoy
                            Text(
                                text = stringResource(R.string.home_pending_today_section),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Flashcards Box
                                GlifoStaggeredEntry(index = 1, modifier = Modifier.weight(1f)) {
                                    GlifoCard(
                                        onClick = onNavigateToFlashcards,
                                        modifier = Modifier.height(90.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = colors.accentText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.home_flashcard_count, flashcardCount),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = stringResource(R.string.home_flashcard_topic),
                                            fontSize = 11.sp,
                                            color = colors.textSecondary,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }

                                // Quiz Box
                                GlifoStaggeredEntry(index = 2, modifier = Modifier.weight(1f)) {
                                    GlifoCard(
                                        onClick = onNavigateToQuiz,
                                        modifier = Modifier.height(90.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Schedule,
                                                contentDescription = null,
                                                tint = colors.accentText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.home_quiz_count, quizCount),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = stringResource(R.string.home_quiz_description),
                                            fontSize = 11.sp,
                                            color = colors.textSecondary,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            // Section: Últimos apuntes
                            Text(
                                text = stringResource(R.string.home_recent_notes_section),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                            )

                            GlifoStaggeredEntry(index = 3) {
                                GlifoCard(tight = true) {
                                    recentNotes.forEachIndexed { i, meta ->
                                        GlifoItem(
                                            showBottomBorder = i < recentNotes.size - 1,
                                            onClick = { onNavigateToNoteDetail(meta.note.id) }
                                        ) {
                                            GlifoThumb {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                                    contentDescription = null,
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = meta.note.title,
                                                    fontSize = 14.sp,
                                                    color = colors.textPrimary,
                                                    fontWeight = FontWeight.Normal
                                                )
                                                Text(
                                                    text = stringResource(R.string.notes_date_page_count, meta.displayDate, meta.pageCount),
                                                    fontSize = 12.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                meta.chips.forEach { chip ->
                                                    GlifoConfidenceChip(state = chip)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(70.dp))
                        }
                    }
                }
            }

            // Camera Floating Action Button
            GlifoCameraFab(
                onClick = onNavigateToCapture,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 14.dp, bottom = 20.dp)
            )
        }
    }
}

// ── Screen B3: Inicio Docente ──────────────────────────────────────────────────
@Composable
fun TeacherHomeScreen(
    onNavigateToCourses: () -> Unit,
    onNavigateToSyllabus: () -> Unit,
    onNavigateToGlossary: () -> Unit,
    onNavigateToGaps: () -> Unit,
    onNavigateToAiCost: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val activeCourse = LocalActiveCourse.current
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

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
                    role = GlifoRole.Teacher,
                    currentScreen = "home_teacher",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        when (route) {
                            "teacher_courses" -> onNavigateToCourses()
                            "syllabus" -> onNavigateToSyllabus()
                            "glossary" -> onNavigateToGlossary()
                            "gaps" -> onNavigateToGaps()
                            "ai_cost" -> onNavigateToAiCost()
                        }
                    },
                    onChangeCourseClick = {
                        scope.launch { drawerState.close() }
                        activeCourse.onChangeCourse()
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
                title = (uiState as? UiState.Success)?.data?.course?.code ?: stringResource(R.string.teacher_home_default_course_code),
                subtitle = stringResource(R.string.home_subtitle_active_course),
                hasDropdown = true,
                onTitleClick = activeCourse.onChangeCourse,
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = state.data
                    val stats = data.stats
                    val gaps = data.gaps

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        // Group coverage card
                        GlifoStaggeredEntry(index = 0) {
                            GlifoCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.teacher_home_group_coverage_title),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${stats.coveragePercent}%",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accentText
                                    )
                                }

                                CovBar(
                                    v = stats.verified,
                                    r = stats.repaired,
                                    e = stats.escalated,
                                    u = stats.uncertain,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                )

                                Text(
                                    text = stringResource(R.string.teacher_home_enrolled_students, stats.studentCount),
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        // Temas con más vacíos
                        Text(
                            text = stringResource(R.string.teacher_home_gaps_section),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )

                        GlifoStaggeredEntry(index = 1) {
                            GlifoCard(tight = true) {
                                gaps.forEachIndexed { i, gap ->
                                    GlifoItem(compact = true, showBottomBorder = i < gaps.size - 1, onClick = onNavigateToGaps) {
                                        GlifoConfidenceChip(state = gap.state)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = gap.label, fontSize = 13.sp, color = colors.textPrimary)
                                            Text(text = stringResource(R.string.teacher_home_uncovered_fraction, gap.fraction), fontSize = 11.sp, color = colors.textSecondary)
                                        }
                                        Text(text = "${gap.percent.toInt()}%", fontSize = 12.sp, color = colors.textSecondary)
                                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Requiere atención
                        Text(
                            text = stringResource(R.string.teacher_home_attention_section),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )

                        GlifoStaggeredEntry(index = 2) {
                            GlifoCard(
                                backgroundColor = colors.alertFaint,
                                borderColor = colors.alertLine
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = colors.alert, modifier = Modifier.size(18.dp))
                                    Text(text = stringResource(R.string.teacher_home_no_syllabus_title), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                }
                                Text(
                                    text = stringResource(R.string.teacher_home_no_syllabus_description),
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                                )
                                GlifoButton(
                                    text = stringResource(R.string.teacher_home_publish_syllabus),
                                    variant = GlifoButtonVariant.Secondary,
                                    onClick = onNavigateToSyllabus
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Screen B4: Inicio Administrador ────────────────────────────────────────────
@Composable
fun AdminHomeScreen(
    onNavigateToUsers: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToRoles: () -> Unit,
    onNavigateToAiCost: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminHomeViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

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
                    role = GlifoRole.Admin,
                    currentScreen = "home_admin",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        when (route) {
                            "admin_users" -> onNavigateToUsers()
                            "admin_courses" -> onNavigateToCourses()
                            "admin_roles" -> onNavigateToRoles()
                            "ai_cost" -> onNavigateToAiCost()
                        }
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
                title = stringResource(R.string.admin_home_title),
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = onNavigateToUsers) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = stringResource(R.string.admin_home_users_description), tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = state.data
                    val roles = data.roles
                    val recentSignups = data.recentSignups
                    val courseCount = data.courseCount

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlifoStaggeredEntry(index = 0, modifier = Modifier.weight(1f)) {
                                GlifoCard(
                                    onClick = onNavigateToUsers,
                                    modifier = Modifier.height(84.dp)
                                ) {
                                    Text(roles.sumOf { it.userCount }.toString(), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.admin_home_active_users), fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                            GlifoStaggeredEntry(index = 1, modifier = Modifier.weight(1f)) {
                                GlifoCard(
                                    onClick = onNavigateToCourses,
                                    modifier = Modifier.height(84.dp)
                                ) {
                                    Text("$courseCount", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.admin_home_published_courses), fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.admin_home_system_status_section),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )

                        GlifoStaggeredEntry(index = 2) {
                            GlifoCard(tight = true) {
                                GlifoItem(compact = true) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                                    Text(stringResource(R.string.admin_home_api_label), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.admin_home_api_latency), fontSize = 12.sp, color = colors.textSecondary)
                                }
                                GlifoItem(compact = true) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                                    Text(stringResource(R.string.admin_home_postgres_label), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.admin_home_postgres_status), fontSize = 12.sp, color = colors.textSecondary)
                                }
                                GlifoItem(compact = true) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = colors.alert, modifier = Modifier.size(18.dp))
                                    Text(stringResource(R.string.admin_home_simpletex_label), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.admin_home_simpletex_quota), fontSize = 12.sp, color = colors.textSecondary)
                                }
                                GlifoItem(compact = true, showBottomBorder = false) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                                    Text(stringResource(R.string.settings_sync_queue), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.course_detail_pending_count, data.pendingSyncCount), fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.admin_home_role_distribution_section),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )

                        GlifoStaggeredEntry(index = 3) {
                            val total = roles.sumOf { it.userCount }.coerceAtLeast(1)
                            GlifoCard(tight = true) {
                                roles.forEachIndexed { i, role ->
                                    val barColor = if (role.isSensitive) colors.alert else colors.accent
                                    val fraction = role.userCount.toFloat() / total
                                    GlifoItem(showBottomBorder = i < roles.size - 1) {
                                        Text(role.name, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                        Box(
                                            modifier = Modifier
                                                .width(80.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(colors.surfaceHigh)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction)
                                                    .background(barColor)
                                            )
                                        }
                                        Text(
                                            role.userCount.toString(),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.textSecondary,
                                            modifier = Modifier.width(28.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.admin_home_recent_signups_section),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )

                        GlifoStaggeredEntry(index = 4) {
                            GlifoCard(tight = true) {
                                recentSignups.forEachIndexed { i, signup ->
                                    GlifoItem(compact = true, showBottomBorder = i < recentSignups.size - 1) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = signup.email,
                                            fontSize = 14.sp,
                                            color = colors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (i == 0) stringResource(R.string.admin_home_hours_ago_2) else stringResource(R.string.admin_home_hours_ago_5),
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

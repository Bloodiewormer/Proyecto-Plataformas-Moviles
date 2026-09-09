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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: HomeViewModel,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
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
                    activeCourseName = (uiState as? UiState.Success)?.data?.course?.name ?: "Cálculo II",
                    userName = user?.fullName ?: "Brandon Brenes",
                    userEmail = user?.email ?: "estudiante@est.una.ac.cr",
                    userInitials = user?.initials ?: "BB",
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
                        onNavigateToCourses()
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
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(), onRetry = viewModel::load)
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
                            title = course?.name ?: "Cálculo II",
                            subtitle = "Curso activo",
                            hasDropdown = true,
                            onNavigationClick = {
                                scope.launch { drawerState.open() }
                            },
                            actions = {
                                IconButton(onClick = onNavigateToDelta) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "Qué cambió",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        )

                        // Sync Banner
                        if (pendingSyncCount > 0) {
                            GlifoBanner(
                                text = "$pendingSyncCount capturas en cola · se envían al volver la red",
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
                            // Cobertura del curso Card
                            GlifoStaggeredEntry(index = 0) {
                                GlifoCard(onClick = onNavigateToCoverage) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cobertura del curso",
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
                                            text = "18 sólidos · 7 parciales · 5 ausentes",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                        Text(
                                            text = "+${stats.weeklyDeltaPoints} desde el 8 ago",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.verified
                                        )
                                    }
                                }
                            }

                            // Section: Pendientes de hoy
                            Text(
                                text = "PENDIENTES DE HOY",
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
                                                text = "$flashcardCount flashcards",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "Series · Integrales impropias",
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
                                                text = "$quizCount quiz",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "De los vacíos detectados",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            // Section: Últimos apuntes
                            Text(
                                text = "ÚLTIMOS APUNTES",
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
                                                    text = "${meta.displayDate} · ${meta.pageCount} páginas",
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
    viewModel: TeacherHomeViewModel,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
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
                    activeCourseName = (uiState as? UiState.Success)?.data?.course?.code ?: "EIF411",
                    userName = user?.fullName ?: "Maikol Guzmán",
                    userEmail = user?.email ?: "docente@una.ac.cr",
                    userInitials = user?.initials ?: "MG",
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
                        onNavigateToCourses()
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
                title = (uiState as? UiState.Success)?.data?.course?.code ?: "EIF411",
                subtitle = "Curso activo",
                hasDropdown = true,
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(), onRetry = viewModel::load)
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
                                        text = "Cobertura del grupo",
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
                                    text = "${stats.studentCount} estudiantes inscritos · promedio del curso",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        // Temas con más vacíos
                        Text(
                            text = "TEMAS CON MÁS VACÍOS",
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
                                            Text(text = "${gap.fraction} sin cubrir", fontSize = 11.sp, color = colors.textSecondary)
                                        }
                                        Text(text = "${gap.percent.toInt()}%", fontSize = 12.sp, color = colors.textSecondary)
                                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Requiere atención
                        Text(
                            text = "REQUIERE ATENCIÓN",
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
                                    Text(text = "Álgebra Lineal no tiene temario", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                }
                                Text(
                                    text = "Sin temario no se puede calcular la cobertura.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                                )
                                GlifoButton(
                                    text = "Publicar temario",
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
    viewModel: AdminHomeViewModel,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
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
                    userName = user?.fullName ?: "Admin Glifo",
                    userEmail = user?.email ?: "admin@una.ac.cr",
                    userInitials = user?.initials ?: "AD",
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
                title = "Administración",
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = onNavigateToUsers) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = "Usuarios", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(), onRetry = viewModel::load)
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
                                    Text("Usuarios activos", fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                            GlifoStaggeredEntry(index = 1, modifier = Modifier.weight(1f)) {
                                GlifoCard(
                                    onClick = onNavigateToCourses,
                                    modifier = Modifier.height(84.dp)
                                ) {
                                    Text("$courseCount", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("Cursos publicados", fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }

                        Text(
                            text = "ESTADO DEL SISTEMA",
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
                                    Text("API", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text("142 ms", fontSize = 12.sp, color = colors.textSecondary)
                                }
                                GlifoItem(compact = true) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                                    Text("PostgreSQL", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text("operativa", fontSize = 12.sp, color = colors.textSecondary)
                                }
                                GlifoItem(compact = true) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = colors.alert, modifier = Modifier.size(18.dp))
                                    Text("SimpleTex", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text("cuota al 78%", fontSize = 12.sp, color = colors.textSecondary)
                                }
                                GlifoItem(compact = true, showBottomBorder = false) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                                    Text("Cola de sincronización", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Text("${data.pendingSyncCount} pendientes", fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }

                        Text(
                            text = "REPARTO DE ROLES",
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
                            text = "ÚLTIMOS REGISTROS",
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
                                            text = if (i == 0) "hace 2 h" else "hace 5 h",
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

package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.presentation.viewmodel.AdminCoursesViewModel
import cr.ac.una.glifo.presentation.viewmodel.AdminResetPasswordViewModel
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

// ── Screen J1: Usuarios Admin ──────────────────────────────────────────────
@Composable
fun AdminUsersScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val user by sessionViewModel.currentUser.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

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
                    currentScreen = "admin_users",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
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
                title = stringResource(R.string.admin_users_title),
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            Icons.Default.LockReset,
                            contentDescription = stringResource(R.string.admin_reset_password_title),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.admin_users_unavailable),
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GlifoButton(
                        text = stringResource(R.string.admin_reset_password_title),
                        icon = Icons.Default.LockReset,
                        onClick = { showResetDialog = true },
                        small = true
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        ResetPasswordDialog(onDismiss = { showResetDialog = false })
    }
}

/**
 * El admin escribe el correo de una cuenta y recibe una contraseña temporal para pasársela.
 * Es el camino de "Olvidé mi contraseña" mientras la app no pueda mandar correos.
 */
@Composable
private fun ResetPasswordDialog(
    onDismiss: () -> Unit,
    viewModel: AdminResetPasswordViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var temporary by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val invalidEmail = stringResource(R.string.admin_reset_password_error_email)

    Dialog(onDismissRequest = onDismiss) {
        GlifoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.admin_reset_password_title), fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = colors.textSecondary, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Spacer(modifier = Modifier.height(8.dp))

            val result = temporary
            if (result == null) {
                Text(stringResource(R.string.admin_reset_password_description), fontSize = 13.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                GlifoField(
                    label = stringResource(R.string.login_email_label),
                    value = email,
                    placeholder = stringResource(R.string.admin_users_field_email_placeholder),
                    onValueChange = { email = it; error = null }
                )
                error?.let {
                    Text(it, fontSize = 13.sp, color = colors.alert, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(modifier = Modifier.height(14.dp))
                GlifoButton(
                    text = if (working) stringResource(R.string.admin_reset_password_working) else stringResource(R.string.admin_reset_password_button),
                    enabled = !working && email.isNotBlank(),
                    onClick = {
                        // Validación mínima antes de llamar: el backend vuelve a validar.
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            error = invalidEmail
                            return@GlifoButton
                        }
                        working = true
                        scope.launch {
                            when (val r = viewModel.resetPassword(email)) {
                                is Result.Success -> temporary = r.value
                                is Result.Failure -> error = r.error.toMessage(context)
                            }
                            working = false
                        }
                    }
                )
            } else {
                Text(stringResource(R.string.admin_reset_password_done, email.trim()), fontSize = 13.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(result, fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.admin_reset_password_hint), fontSize = 12.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(14.dp))
                GlifoButton(
                    text = if (copied) stringResource(R.string.admin_reset_password_copied) else stringResource(R.string.admin_reset_password_copy),
                    onClick = {
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Glifo", result))
                        copied = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlifoButton(
                    text = stringResource(R.string.common_close),
                    variant = GlifoButtonVariant.Secondary,
                    onClick = onDismiss
                )
            }
        }
    }
}

// ── Screen J2: Detalle Usuario Admin ───────────────────────────────────────
@Composable
fun AdminUserDetailScreen(
    userName: String = "",
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    GlifoStatusScreen(
        title = userName.ifBlank { stringResource(R.string.admin_users_title) },
        onNavigateBack = onNavigateBack
    ) {
        GlifoEmptyBox(
            message = stringResource(R.string.admin_user_detail_unavailable),
            actionText = stringResource(R.string.common_nav_back),
            onAction = onNavigateBack
        )
    }
}

// ── Screen J3: Roles y Privilegios ─────────────────────────────────────────
@Composable
fun AdminRolesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val user by sessionViewModel.currentUser.collectAsState()

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
                    currentScreen = "admin_roles",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
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
                title = stringResource(R.string.admin_roles_title),
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                GlifoEmptyBox(
                    message = stringResource(R.string.admin_roles_unavailable),
                    actionText = stringResource(R.string.common_nav_back),
                    onAction = onNavigateBackOrMenu
                )
            }
        }
    }
}

// ── Screen J4: Cursos (vista de Admin) ──────────────────────────────────────
// Distinct from TeacherCoursesScreen: admin sees every course in the system (not
// "sus" cursos) and keeps the admin drawer/identity instead of borrowing the
// teacher's — entering here used to silently turn the admin into a teacher.
@Composable
fun AdminCoursesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToCourseDetail: (Long) -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminCoursesViewModel = hiltViewModel(),
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
                    currentScreen = "admin_courses",
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
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
                title = stringResource(R.string.admin_courses_title),
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoEmptyBox(
                    message = stringResource(R.string.courses_empty_no_courses),
                    actionText = stringResource(R.string.common_retry),
                    onAction = viewModel::load
                )
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val coursesWithStats = state.data
                    if (coursesWithStats.isEmpty()) {
                        GlifoEmptyBox(
                            message = stringResource(R.string.courses_empty_no_courses),
                            actionText = stringResource(R.string.common_retry),
                            onAction = viewModel::load
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            coursesWithStats.forEachIndexed { i, entry ->
                                val course = entry.course
                                val stats = entry.stats
                                val codeSubtitle = if (stats.instructorName.isBlank()) {
                                    course.code
                                } else {
                                    stringResource(R.string.courses_code_instructor, course.code, stats.instructorName)
                                }
                                GlifoStaggeredEntry(index = i) {
                                    GlifoCard(
                                        onClick = { onNavigateToCourseDetail(course.id) },
                                        modifier = Modifier.padding(bottom = 10.dp),
                                        borderColor = if (stats.hasSyllabus) colors.border else colors.alertLine
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = course.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                            if (!stats.hasSyllabus) GlifoBadge(text = stringResource(R.string.admin_courses_no_syllabus_badge), backgroundColor = colors.alertSoft, textColor = colors.alert)
                                        }
                                        Text(
                                            text = codeSubtitle,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        if (stats.hasSyllabus) {
                                            CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 8.dp))
                                        }
                                        Text(
                                            text = if (stats.hasSyllabus) {
                                                stringResource(R.string.admin_courses_students_with_coverage, stats.studentCount, stats.coveragePercent)
                                            } else {
                                                stringResource(R.string.teacher_gaps_student_count, stats.studentCount)
                                            },
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
    }
}

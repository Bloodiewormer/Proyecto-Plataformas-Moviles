package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import cr.ac.una.glifo.domain.model.AdminUser
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.presentation.viewmodel.AdminCoursesViewModel
import cr.ac.una.glifo.presentation.viewmodel.AdminResetPasswordViewModel
import cr.ac.una.glifo.presentation.viewmodel.AdminRolesViewModel
import cr.ac.una.glifo.presentation.viewmodel.AdminUsersViewModel
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
    viewModel: AdminUsersViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AdminUser?>(null) }

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

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Empty -> GlifoEmptyBox(stringResource(R.string.admin_users_empty), stringResource(R.string.common_retry), viewModel::load)
                is UiState.Success -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    state.data.forEachIndexed { i, account ->
                        val isMe = account.id == user?.id
                        GlifoStaggeredEntry(index = i) {
                            GlifoCard(
                                onClick = if (isMe) null else ({ selected = account }),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(account.fullName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    if (isMe) GlifoBadge(text = stringResource(R.string.admin_users_you), modifier = Modifier.padding(end = 6.dp))
                                    GlifoBadge(text = roleLabel(account.role))
                                }
                                Text(account.email, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { account ->
        ChangeRoleDialog(
            account = account,
            onSave = { role -> viewModel.changeRole(account.id, role) },
            onResetPassword = { resetEmail = account.email; selected = null; showResetDialog = true },
            onDismiss = { selected = null }
        )
    }

    if (showResetDialog) {
        ResetPasswordDialog(initialEmail = resetEmail, onDismiss = { showResetDialog = false; resetEmail = "" })
    }
}

/**
 * El admin escribe el correo de una cuenta y recibe una contraseña temporal para pasársela.
 * Es el camino de "Olvidé mi contraseña" mientras la app no pueda mandar correos.
 */
@Composable
private fun ResetPasswordDialog(
    onDismiss: () -> Unit,
    initialEmail: String = "",
    viewModel: AdminResetPasswordViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(initialEmail) }
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

@Composable
private fun roleLabel(role: GlifoRole): String = when (role) {
    GlifoRole.Student -> stringResource(R.string.login_role_student)
    GlifoRole.Teacher -> stringResource(R.string.login_role_teacher)
    GlifoRole.Admin -> stringResource(R.string.login_role_admin)
}

@Composable
private fun roleNameLabel(name: String): String = when (name) {
    "ROLE_STUDENT" -> roleLabel(GlifoRole.Student)
    "ROLE_TEACHER" -> roleLabel(GlifoRole.Teacher)
    "ROLE_ADMIN" -> roleLabel(GlifoRole.Admin)
    else -> name
}

/** Elegir el rol de otra cuenta. El backend no deja cambiar el propio, por eso esa fila no abre el dialogo. */
@Composable
private fun ChangeRoleDialog(
    account: AdminUser,
    onSave: suspend (GlifoRole) -> Result<AdminUser>,
    onResetPassword: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf(account.role) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlifoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.admin_users_change_role), fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = colors.textSecondary, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Text(account.fullName, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
            Text(account.email, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            GlifoRole.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { role = option; error = null },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = role == option, onClick = { role = option; error = null })
                    Text(roleLabel(option), fontSize = 14.sp, color = colors.textPrimary)
                }
            }
            Text(stringResource(R.string.admin_users_role_hint), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
            error?.let { Text(it, fontSize = 13.sp, color = colors.alert, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(modifier = Modifier.height(14.dp))
            GlifoButton(
                text = if (working) stringResource(R.string.change_password_saving) else stringResource(R.string.admin_users_save_role),
                enabled = !working && role != account.role,
                onClick = {
                    working = true
                    scope.launch {
                        when (val r = onSave(role)) {
                            is Result.Success -> onDismiss()
                            is Result.Failure -> error = r.error.toMessage(context)
                        }
                        working = false
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlifoButton(
                text = stringResource(R.string.admin_reset_password_title),
                icon = Icons.Default.LockReset,
                variant = GlifoButtonVariant.Secondary,
                onClick = onResetPassword
            )
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
    viewModel: AdminRolesViewModel = hiltViewModel(),
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

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Empty -> GlifoEmptyBox(stringResource(R.string.admin_users_empty), stringResource(R.string.common_retry), viewModel::load)
                is UiState.Success -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    state.data.forEachIndexed { i, role ->
                        GlifoStaggeredEntry(index = i) {
                            GlifoCard(modifier = Modifier.padding(bottom = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(roleNameLabel(role.name), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    GlifoBadge(text = stringResource(R.string.admin_roles_user_count, role.userCount))
                                }
                                role.description?.let {
                                    Text(it, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                }
                                Text(
                                    stringResource(R.string.admin_roles_privileges),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                                role.privileges.forEach { privilege ->
                                    Text(privilege, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
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

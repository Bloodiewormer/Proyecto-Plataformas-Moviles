package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.presentation.viewmodel.AdminCoursesViewModel
import cr.ac.una.glifo.presentation.viewmodel.AdminResetPasswordViewModel
import cr.ac.una.glifo.presentation.viewmodel.AdminRolesViewModel
import cr.ac.una.glifo.presentation.viewmodel.AdminUserDetailViewModel
import cr.ac.una.glifo.presentation.viewmodel.AdminUsersViewModel
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

// ── Screen J1: Usuarios Admin ─────────────────────────────────────────────────
@Composable
fun AdminUsersScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
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
    val uiState by viewModel.state.collectAsState()

    val filterAll = stringResource(R.string.admin_users_filter_all)
    val filterTeachers = stringResource(R.string.admin_users_filter_teachers)
    val filterStudents = stringResource(R.string.admin_users_filter_students)
    val filterAdmins = stringResource(R.string.admin_users_filter_admins)

    var filter by remember { mutableStateOf(filterAll) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
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
                    IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search), tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.LockReset, contentDescription = stringResource(R.string.admin_reset_password_title), tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.teacher_glossary_add_description), tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            )

            AnimatedVisibility(visible = searchOpen) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    GlifoField(
                        label = stringResource(R.string.admin_users_search_placeholder),
                        value = query,
                        onValueChange = { query = it },
                        isFocused = true
                    )
                }
            }

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val users = state.data
                    val roleFiltered = when (filter) {
                        filterTeachers -> users.filter { it.role == "DOCENTE" }
                        filterStudents -> users.filter { it.role == "ESTUDIANTE" }
                        filterAdmins -> users.filter { it.role == "ADMIN" }
                        else -> users
                    }
                    val filtered = if (query.isBlank()) roleFiltered else roleFiltered.filter {
                        it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
                    }
                    val counts = mapOf(
                        filterAll to users.size,
                        filterTeachers to users.count { it.role == "DOCENTE" },
                        filterStudents to users.count { it.role == "ESTUDIANTE" },
                        filterAdmins to users.count { it.role == "ADMIN" }
                    )

                    // Chips filters row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(filterAll, filterTeachers, filterStudents, filterAdmins).forEach { label ->
                            val active = filter == label
                            Box(
                                modifier = Modifier
                                    .background(if (active) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                                    .clickable { filter = label }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(text = stringResource(R.string.admin_users_filter_chip, label, counts.getValue(label)), fontSize = 12.sp, color = if (active) colors.accentText else colors.textSecondary)
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        GlifoBanner(text = stringResource(R.string.admin_users_no_match), modifier = Modifier.padding(horizontal = 2.dp))
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        filtered.forEachIndexed { i, u ->
                            GlifoStaggeredEntry(index = i) {
                                GlifoItem(
                                    modifier = Modifier.alpha(if (u.isActive) 1f else 0.55f),
                                    onClick = { onNavigateToUserDetail(u.name) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(if (u.isActive) colors.accentSoft else colors.surfaceHigh, CircleShape)
                                            .border(1.dp, colors.border, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = u.initials, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (u.isActive) colors.accentText else colors.textSecondary)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = u.name, fontSize = 16.sp, color = colors.textPrimary)
                                        Text(text = u.email, fontSize = 14.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                    }
                                    GlifoBadge(
                                        text = u.displayLabel,
                                        backgroundColor = if (!u.isActive) colors.alertSoft else colors.accentSoft,
                                        textColor = if (!u.isActive) colors.alert else colors.accentText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        ResetPasswordDialog(onDismiss = { showResetDialog = false })
    }

    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, email, role ->
                viewModel.addUser(name, email, role)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, email: String, role: String) -> Unit
) {
    val colors = GlifoTheme.colors
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("ESTUDIANTE") }

    Dialog(onDismissRequest = onDismiss) {
        GlifoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.admin_users_add_dialog_title), fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = colors.textSecondary, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Spacer(modifier = Modifier.height(12.dp))
            GlifoField(label = stringResource(R.string.register_field_full_name), value = name, onValueChange = { name = it })
            Spacer(modifier = Modifier.height(8.dp))
            GlifoField(label = stringResource(R.string.login_email_label), value = email, placeholder = stringResource(R.string.admin_users_field_email_placeholder), onValueChange = { email = it })
            Spacer(modifier = Modifier.height(10.dp))
            val studentLabel = stringResource(R.string.admin_users_role_student)
            val teacherLabel = stringResource(R.string.admin_users_role_teacher)
            val adminLabel = stringResource(R.string.admin_users_role_admin)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ESTUDIANTE" to studentLabel, "DOCENTE" to teacherLabel, "ADMIN" to adminLabel).forEach { (value, label) ->
                    val selected = role == value
                    Box(
                        modifier = Modifier
                            .background(if (selected) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                            .clickable { role = value }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 12.sp, color = if (selected) colors.accentText else colors.textSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            GlifoButton(
                text = stringResource(R.string.teacher_add_button),
                enabled = name.isNotBlank() && email.isNotBlank(),
                onClick = { onAdd(name, email, role) }
            )
        }
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

// ── Screen J2: Detalle Usuario Admin ──────────────────────────────────────────
@Composable
fun AdminUserDetailScreen(
    userName: String = "David González",
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AdminUserDetailViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(userName) { viewModel.load(userName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = userName,
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                TextButton(onClick = onSaveSuccess) {
                    Text(stringResource(R.string.correct_fragment_save), color = colors.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        )

        when (val state = uiState) {
            is UiState.Loading -> GlifoLoadingBox()
            is UiState.Empty -> GlifoLoadingBox()
            is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(userName) })
            is UiState.Success -> {
                val user = state.data.user
                val privs = state.data.privileges
                var checkedRole by remember(userName) { mutableStateOf(state.data.roleKey) }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    // User header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(colors.surfaceHigh, CircleShape)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = user.initials, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                        }
                        Column {
                            Text(text = userName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text(text = user.email, fontSize = 13.sp, color = colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlifoField(label = stringResource(R.string.register_field_full_name), value = userName)
                    Spacer(modifier = Modifier.height(10.dp))
                    GlifoField(label = stringResource(R.string.forgot_password_field_email), value = user.email)

                    Text(
                        text = stringResource(R.string.admin_user_detail_roles_section),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )

                    GlifoCard(tight = true) {
                        // roleName doubles as the comparison key against state.data.roleKey and
                        // checkedRole below, so it stays untranslated on purpose — only the
                        // description is UI copy.
                        listOf(
                            Pair("Estudiante", stringResource(R.string.admin_user_detail_role_student_desc)),
                            Pair("Docente", stringResource(R.string.admin_user_detail_role_teacher_desc)),
                            Pair("Administrador", stringResource(R.string.admin_user_detail_role_admin_desc))
                        ).forEachIndexed { i, (roleName, desc) ->
                            GlifoItem(showBottomBorder = i < 2) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = roleName, fontSize = 15.sp, color = colors.textPrimary)
                                    Text(text = desc, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                }
                                Checkbox(
                                    checked = checkedRole == roleName,
                                    onCheckedChange = { checked -> if (checked) checkedRole = roleName },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.accent, checkmarkColor = colors.onAccent)
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.admin_user_detail_privileges_section),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )

                    GlifoCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            privs.take(3).forEach { p ->
                                GlifoBadge(text = p, backgroundColor = colors.neutralSoft, textColor = colors.textSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            privs.drop(3).forEach { p ->
                                GlifoBadge(text = p, backgroundColor = colors.neutralSoft, textColor = colors.textSecondary)
                            }
                        }
                        Text(
                            text = stringResource(R.string.admin_user_detail_privileges_note),
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlifoButton(
                        text = if (user.isActive) stringResource(R.string.admin_user_detail_deactivate) else stringResource(R.string.admin_user_detail_reactivate),
                        variant = GlifoButtonVariant.Danger,
                        onClick = {
                            scope.launch {
                                viewModel.setActive(userName, !user.isActive)
                                onSaveSuccess()
                            }
                        }
                    )
                }
            }
        }
    }
}

// ── Screen J3: Roles y Privilegios ────────────────────────────────────────────
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
                // No "add role" action here on purpose — Estudiante/Docente/Administrador
                // are the platform's fixed roles, not something admins create ad hoc.
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val roles = state.data

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        roles.forEachIndexed { i, role ->
                            GlifoStaggeredEntry(index = i) {
                                GlifoCard(modifier = Modifier.padding(bottom = 10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(role.name, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                        GlifoBadge(
                                            text = stringResource(R.string.admin_roles_user_count_badge, role.userCount),
                                            backgroundColor = if (role.isSensitive) colors.alertSoft else colors.accentSoft,
                                            textColor = if (role.isSensitive) colors.alert else colors.accentText
                                        )
                                    }
                                    Text(role.description, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        role.privileges.forEach {
                                            GlifoBadge(text = it, backgroundColor = colors.neutralSoft, textColor = colors.textSecondary)
                                        }
                                        if (role.extraPrivilegeCount > 0) {
                                            GlifoBadge(text = stringResource(R.string.admin_roles_extra_count, role.extraPrivilegeCount), backgroundColor = colors.neutralSoft, textColor = colors.textSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        GlifoCard(
                            backgroundColor = colors.accentFaint,
                            borderColor = colors.accentLine
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(stringResource(R.string.admin_roles_no_shared_access_title), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Text(stringResource(R.string.admin_roles_no_shared_access_description), fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Screen J4: Cursos (vista de Admin) ────────────────────────────────────────
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
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val coursesWithStats = state.data

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
                                        text = stringResource(R.string.courses_code_instructor, course.code, stats.instructorName),
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

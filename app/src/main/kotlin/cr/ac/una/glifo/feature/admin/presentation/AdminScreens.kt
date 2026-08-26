package cr.ac.una.glifo.feature.admin.presentation

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cr.ac.una.glifo.core.data.dummy.DummyAdminProvider
import cr.ac.una.glifo.core.data.dummy.DummyCourseProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

// ── Screen J1: Usuarios Admin ─────────────────────────────────────────────────
@Composable
fun AdminUsersScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors

    var version by remember { mutableStateOf(0) } // bumped after adding a user, to recompose the list
    val users = remember(version) { DummyAdminProvider.getUsers() }
    var filter by remember { mutableStateOf("Todos") }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val roleFiltered = when (filter) {
        "Docentes" -> users.filter { it.role == "DOCENTE" }
        "Estudiantes" -> users.filter { it.role == "ESTUDIANTE" }
        "Admins" -> users.filter { it.role == "ADMIN" }
        else -> users
    }
    val filtered = if (query.isBlank()) roleFiltered else roleFiltered.filter {
        it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
    }
    val counts = mapOf(
        "Todos" to users.size,
        "Docentes" to users.count { it.role == "DOCENTE" },
        "Estudiantes" to users.count { it.role == "ESTUDIANTE" },
        "Admins" to users.count { it.role == "ADMIN" }
    )

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
                title = "Usuarios",
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            )

            AnimatedVisibility(visible = searchOpen) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    GlifoField(
                        label = "Buscar por nombre o correo",
                        value = query,
                        onValueChange = { query = it },
                        isFocused = true
                    )
                }
            }

            // Chips filters row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todos", "Docentes", "Estudiantes", "Admins").forEach { label ->
                    val active = filter == label
                    Box(
                        modifier = Modifier
                            .background(if (active) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                            .clickable { filter = label }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(text = "$label · ${counts[label]}", fontSize = 12.sp, color = if (active) colors.accentText else colors.textSecondary)
                    }
                }
            }

            if (filtered.isEmpty()) {
                GlifoBanner(text = "Nadie coincide con esa búsqueda.", modifier = Modifier.padding(horizontal = 2.dp))
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

    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, email, role ->
                DummyAdminProvider.addUser(name, email, role)
                version++
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
                Text("Agregar usuario", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = colors.textSecondary, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Spacer(modifier = Modifier.height(12.dp))
            GlifoField(label = "Nombre completo", value = name, onValueChange = { name = it })
            Spacer(modifier = Modifier.height(8.dp))
            GlifoField(label = "Correo", value = email, placeholder = "usuario@una.ac.cr", onValueChange = { email = it })
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ESTUDIANTE" to "Estudiante", "DOCENTE" to "Docente", "ADMIN" to "Admin").forEach { (value, label) ->
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
                text = "Agregar",
                enabled = name.isNotBlank() && email.isNotBlank(),
                onClick = { onAdd(name, email, role) }
            )
        }
    }
}

// ── Screen J2: Detalle Usuario Admin ──────────────────────────────────────────
@Composable
fun AdminUserDetailScreen(
    userName: String = "David González",
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val colors = GlifoTheme.colors
    val user = DummyAdminProvider.findUser(userName)
    val roleKey = when (user.role) {
        "DOCENTE" -> "Docente"
        "ADMIN" -> "Administrador"
        else -> "Estudiante"
    }
    val privs = DummyAdminProvider.getPrivilegesFor(roleKey)
    var checkedRole by remember(userName) { mutableStateOf(roleKey) }

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
                    Text("Guardar", color = colors.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        )

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

            GlifoField(label = "Nombre completo", value = userName)
            Spacer(modifier = Modifier.height(10.dp))
            GlifoField(label = "Correo institucional", value = user.email)

            Text(
                text = "ROLES ASIGNADOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                listOf(
                    Pair("Estudiante", "Rol principal en la plataforma · activo"),
                    Pair("Docente", "Acceso a temarios y métricas de grupo"),
                    Pair("Administrador", "Acceso total al sistema")
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
                text = "PRIVILEGIOS EFECTIVOS",
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
                    text = "Se calculan desde user_roles y role_privileges. No se asignan uno por uno.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlifoButton(
                text = if (user.isActive) "Desactivar la cuenta" else "Reactivar la cuenta",
                variant = GlifoButtonVariant.Danger,
                onClick = {
                    DummyAdminProvider.setActive(userName, !user.isActive)
                    onSaveSuccess()
                }
            )
        }
    }
}

// ── Screen J3: Roles y Privilegios ────────────────────────────────────────────
@Composable
fun AdminRolesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val roles = DummyAdminProvider.getRoles()

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
                title = "Roles",
                onNavigationClick = { scope.launch { drawerState.open() } }
                // No "add role" action here on purpose — Estudiante/Docente/Administrador
                // are the platform's fixed roles, not something admins create ad hoc.
            )

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
                                    text = "${role.userCount} USUARIOS",
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
                                    GlifoBadge(text = "+${role.extraPrivilegeCount}", backgroundColor = colors.neutralSoft, textColor = colors.textSecondary)
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
                            Text("Ningún rol lee apuntes ajenos", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text("No existe el privilegio note:read:any. El docente solo ve datos agregados.", fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
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
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val courses = (DummyCourseProvider.getCourses + DummyCourseProvider.getTeacherCourses).distinctBy { it.id }

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
                title = "Cursos",
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                courses.forEachIndexed { i, course ->
                    val stats = DummyCourseProvider.getStats(course.id)
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
                                if (!stats.hasSyllabus) GlifoBadge(text = "SIN TEMARIO", backgroundColor = colors.alertSoft, textColor = colors.alert)
                            }
                            Text(
                                text = "${course.code} · ${stats.instructorName}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            if (stats.hasSyllabus) {
                                CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            Text(
                                text = "${stats.studentCount} estudiantes" + if (stats.hasSyllabus) " · ${stats.coveragePercent}% de cobertura" else "",
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

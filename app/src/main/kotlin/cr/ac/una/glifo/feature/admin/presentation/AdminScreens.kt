package cr.ac.una.glifo.feature.admin.presentation

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyAdminProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

// ── Screen J1: Usuarios Admin ─────────────────────────────────────────────────
@Composable
fun AdminUsersScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors

    val users = DummyAdminProvider.getUsers()
    var filter by remember { mutableStateOf("Todos") }
    val filtered = when (filter) {
        "Docentes" -> users.filter { it.roleLabel == "DOCENTE" }
        "Estudiantes" -> users.filter { it.roleLabel == "ESTUDIANTE" }
        "Admins" -> users.filter { it.roleLabel == "ADMIN" }
        else -> users
    }
    val counts = mapOf(
        "Todos" to users.size,
        "Docentes" to users.count { it.roleLabel == "DOCENTE" },
        "Estudiantes" to users.count { it.roleLabel == "ESTUDIANTE" },
        "Admins" to users.count { it.roleLabel == "ADMIN" }
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
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = colors.textSecondary, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Add, contentDescription = "Agregar", tint = colors.textSecondary, modifier = Modifier.size(20.dp)) }
                }
            )

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
                                text = u.roleLabel,
                                backgroundColor = if (u.roleLabel == "INACTIVO") colors.alertSoft else colors.accentSoft,
                                textColor = if (u.roleLabel == "INACTIVO") colors.alert else colors.accentText
                            )
                        }
                    }
                }
            }
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
    val roleKey = when (user.roleLabel) {
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
                onClick = onSaveSuccess
            )
        }
    }
}

// ── Screen J3: Roles y Privilegios ────────────────────────────────────────────
@Composable
fun AdminRolesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
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
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Add, contentDescription = "Agregar", tint = colors.textSecondary, modifier = Modifier.size(20.dp)) }
                }
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

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val users = listOf(
        Triple("MG", "Maikol Guzmán", Pair("maikol.guzman@una.cr", Pair("DOCENTE", true))),
        Triple("BB", "Brandon Brenes", Pair("bbrenes@est.una.ac.cr", Pair("ESTUDIANTE", true))),
        Triple("DG", "David González", Pair("dgonzalez@est.una.ac.cr", Pair("ESTUDIANTE", true))),
        Triple("FU", "Felipe Ugalde", Pair("fugalde@est.una.ac.cr", Pair("ESTUDIANTE", true))),
        Triple("KJ", "Keneth Jara", Pair("kjara@est.una.ac.cr", Pair("INACTIVO", false)))
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
                listOf(
                    Pair("Todos · 142", true),
                    Pair("Docentes · 8", false),
                    Pair("Estudiantes · 131", false),
                    Pair("Admins · 3", false)
                ).forEach { (label, active) ->
                    Box(
                        modifier = Modifier
                            .background(if (active) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(text = label, fontSize = 12.sp, color = if (active) colors.accentText else colors.textSecondary)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                users.forEachIndexed { i, (initials, name, data) ->
                    val (email, roleInfo) = data
                    val (roleName, isActive) = roleInfo

                    GlifoItem(
                        modifier = Modifier.alpha(if (isActive) 1f else 0.55f),
                        onClick = { onNavigateToUserDetail(name) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (isActive) colors.accentSoft else colors.surfaceHigh, CircleShape)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = initials, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isActive) colors.accentText else colors.textSecondary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, fontSize = 16.sp, color = colors.textPrimary)
                            Text(text = email, fontSize = 14.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                        GlifoBadge(
                            text = roleName,
                            backgroundColor = if (roleName == "INACTIVO") colors.alertSoft else colors.accentSoft,
                            textColor = if (roleName == "INACTIVO") colors.alert else colors.accentText
                        )
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
    val privs = listOf("note:create", "note:read:own", "coverage:read:own", "study:attempt", "course:join", "glossary:suggest")

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
                    Text(text = "DG", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                }
                Column {
                    Text(text = userName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(text = "dgonzalez@est.una.ac.cr", fontSize = 13.sp, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlifoField(label = "Nombre completo", value = userName)
            Spacer(modifier = Modifier.height(10.dp))
            GlifoField(label = "Correo institucional", value = "dgonzalez@est.una.ac.cr")

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
                    val isChecked = i == 0
                    GlifoItem(showBottomBorder = i < 2) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = roleName, fontSize = 15.sp, color = colors.textPrimary)
                            Text(text = desc, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {},
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
                text = "Desactivar la cuenta",
                variant = GlifoButtonVariant.Danger,
                onClick = {}
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
                // Role 1: Estudiante
                GlifoCard(modifier = Modifier.padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estudiante", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        GlifoBadge(text = "131 USUARIOS", backgroundColor = colors.accentSoft, textColor = colors.accentText)
                    }
                    Text("Captura, revisa y estudia sobre sus propios apuntes.", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("note:create", "note:read:own", "coverage:read:own", "+3").forEach {
                            GlifoBadge(text = it, backgroundColor = colors.neutralSoft, textColor = colors.textSecondary)
                        }
                    }
                }

                // Role 2: Docente
                GlifoCard(modifier = Modifier.padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Docente", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        GlifoBadge(text = "9 USUARIOS", backgroundColor = colors.accentSoft, textColor = colors.accentText)
                    }
                    Text("Publica temario y glosario; ve brechas agregadas del grupo.", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("course:create", "syllabus:publish", "glossary:write", "+6").forEach {
                            GlifoBadge(text = it, backgroundColor = colors.neutralSoft, textColor = colors.textSecondary)
                        }
                    }
                }

                // Role 3: Administrador
                GlifoCard(modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Administrador", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        GlifoBadge(text = "2 USUARIOS", backgroundColor = colors.alertSoft, textColor = colors.alert)
                    }
                    Text("Usuarios, roles y estado del sistema.", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("user:manage", "role:assign", "system:read", "+11").forEach {
                            GlifoBadge(text = it, backgroundColor = colors.neutralSoft, textColor = colors.textSecondary)
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

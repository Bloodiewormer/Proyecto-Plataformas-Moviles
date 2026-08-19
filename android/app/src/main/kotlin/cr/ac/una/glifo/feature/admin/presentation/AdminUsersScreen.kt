package cr.ac.una.glifo.feature.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.network.GlifoApi
import cr.ac.una.glifo.core.network.dto.UpdateRolesRequest
import cr.ac.una.glifo.core.network.dto.UpdateStatusRequest
import cr.ac.una.glifo.core.network.dto.UserAdminDto
import cr.ac.una.glifo.core.ui.component.GlifoCard
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun AdminUsersScreen(
    api: GlifoApi,
    onNavigateBack: () -> Unit
) {
    var users by remember { mutableStateOf<List<UserAdminDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedUserForRoles by remember { mutableStateOf<UserAdminDto?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun loadUsers() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.getAdminUsers()
                users = response.data
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error al cargar usuarios"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    Scaffold(
        topBar = {
            GlifoTopBar(
                title = "Panel de Administración",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = GlifoTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Gestión de Usuarios y Roles (GLI-050)",
                color = GlifoTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Administra roles, privilegios y estado de acceso de cada usuario.",
                color = GlifoTheme.colors.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlifoTheme.colors.accent)
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage!!,
                        color = GlifoTheme.colors.alert,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { loadUsers() },
                        colors = ButtonDefaults.buttonColors(containerColor = GlifoTheme.colors.accent)
                    ) {
                        Text("Reintentar", color = GlifoTheme.colors.onAccent)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users) { user ->
                        AdminUserCard(
                            user = user,
                            onEditRoles = { selectedUserForRoles = user },
                            onToggleStatus = { newStatus ->
                                coroutineScope.launch {
                                    try {
                                        val res = api.updateUserStatus(user.id, UpdateStatusRequest(newStatus))
                                        if (res.data != null) {
                                            users = users.map { if (it.id == user.id) res.data else it }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (selectedUserForRoles != null) {
        val user = selectedUserForRoles!!
        RoleEditModal(
            user = user,
            onDismiss = { selectedUserForRoles = null },
            onSave = { updatedRoles ->
                coroutineScope.launch {
                    try {
                        val res = api.updateUserRoles(user.id, UpdateRolesRequest(updatedRoles))
                        if (res.data != null) {
                            users = users.map { if (it.id == user.id) res.data else it }
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        selectedUserForRoles = null
                    }
                }
            }
        )
    }
}

@Composable
fun AdminUserCard(
    user: UserAdminDto,
    onEditRoles: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    GlifoCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val name = if (!user.firstName.isNullOrBlank()) {
                        "${user.firstName} ${user.lastName ?: ""}".trim()
                    } else {
                        user.email
                    }
                    Text(
                        text = name,
                        color = GlifoTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.email,
                        color = GlifoTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }

                Switch(
                    checked = user.isActive,
                    onCheckedChange = onToggleStatus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GlifoTheme.colors.accent,
                        checkedTrackColor = GlifoTheme.colors.surfaceHigh
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    user.roles.forEach { role ->
                        val (label, containerColor, textColor) = when (role) {
                            "ROLE_ADMIN", "ADMINISTRADOR" -> Triple("Admin", GlifoTheme.colors.alert, GlifoTheme.colors.textPrimary)
                            "ROLE_TEACHER", "DOCENTE" -> Triple("Docente", GlifoTheme.colors.accent, GlifoTheme.colors.onAccent)
                            else -> Triple("Estudiante", GlifoTheme.colors.surfaceHigh, GlifoTheme.colors.textPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .background(containerColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text(
                    text = "Editar Roles ✏️",
                    color = GlifoTheme.colors.accentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onEditRoles() }
                )
            }
        }
    }
}

@Composable
fun RoleEditModal(
    user: UserAdminDto,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val availableRoles = listOf("ROLE_STUDENT", "ROLE_TEACHER", "ROLE_ADMIN")
    val selectedRoles = remember { mutableStateListOf(*user.roles.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Asignar Roles a ${user.firstName ?: user.email}",
                color = GlifoTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                availableRoles.forEach { role ->
                    val isChecked = selectedRoles.contains(role)
                    val label = when (role) {
                        "ROLE_ADMIN" -> "Administrador (USER_MANAGE, ROLE_MANAGE)"
                        "ROLE_TEACHER" -> "Docente (COURSE_WRITE, SYLLABUS_PUBLISH)"
                        else -> "Estudiante (NOTE_READ_OWN, STUDY_ITEM)"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedRoles.remove(role) else selectedRoles.add(role)
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedRoles.add(role) else selectedRoles.remove(role)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, color = GlifoTheme.colors.textPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedRoles.toList()) },
                enabled = selectedRoles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = GlifoTheme.colors.accent)
            ) {
                Text("Guardar", color = GlifoTheme.colors.onAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = GlifoTheme.colors.textSecondary)
            }
        },
        containerColor = GlifoTheme.colors.surface
    )
}

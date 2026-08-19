package cr.ac.una.glifo.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.network.dto.CourseResponse
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.feature.home.domain.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCourse: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStudy: () -> Unit,
    viewModel: HomeViewModel? = null
) {
    val uiState by (viewModel?.uiState?.collectAsState() ?: remember {
        mutableStateOf(
            HomeUiState(
                role = UserRole.STUDENT,
                courses = listOf(
                    CourseResponse(id = 1L, name = "Cálculo II", code = "MAT-202", joinCode = "MAT202", emoji = "📐"),
                    CourseResponse(id = 2L, name = "Programación IV", code = "EIF-209", joinCode = "EIF209", emoji = "💻")
                )
            )
        )
    })

    var showCreateCourseDialog by remember { mutableStateOf(false) }
    var showJoinCourseDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel?.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel?.clearMessages()
        }
    }

    // Role-dependent UI labels for Student vs Teacher
    val greetingTitle = when (uiState.role) {
        UserRole.TEACHER -> "Hola, Docente"
        UserRole.STUDENT -> "Hola, Estudiante"
    }

    val coursesSectionTitle = when (uiState.role) {
        UserRole.TEACHER -> "Cursos que imparto"
        UserRole.STUDENT -> "Mis cursos matriculados"
    }

    val fabLabel = when (uiState.role) {
        UserRole.TEACHER -> "+ Crear curso"
        UserRole.STUDENT -> "+ Unirse a curso"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlifoTopBar(
                title = greetingTitle
            )
        },
        bottomBar = {
            GlifoBottomBar(
                currentRoute = "home",
                onNavigateToHome = { },
                onNavigateToStudy = onNavigateToStudy,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (uiState.role == UserRole.TEACHER) {
                        showCreateCourseDialog = true
                    } else {
                        showJoinCourseDialog = true
                    }
                },
                containerColor = GlifoTheme.colors.accent,
                contentColor = GlifoTheme.colors.onAccent,
                icon = {
                    Icon(
                        imageVector = if (uiState.role == UserRole.TEACHER) Icons.Default.School else Icons.Default.GroupAdd,
                        contentDescription = fabLabel
                    )
                },
                text = { Text(fabLabel, fontWeight = FontWeight.SemiBold) }
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
            // Role indicator banner
            RoleBadgeBanner(role = uiState.role)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = coursesSectionTitle,
                color = GlifoTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlifoTheme.colors.accent)
                }
            } else if (uiState.courses.isEmpty()) {
                EmptyCoursesPlaceholder(role = uiState.role) {
                    if (uiState.role == UserRole.TEACHER) {
                        showCreateCourseDialog = true
                    } else {
                        showJoinCourseDialog = true
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.courses) { course ->
                        CourseCardByRole(
                            course = course,
                            role = uiState.role,
                            onCourseClick = { onNavigateToCourse(course.id) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateCourseDialog) {
        CreateCourseDialog(
            onDismiss = { showCreateCourseDialog = false },
            onCreate = { name, code, term ->
                viewModel?.createCourse(name, code, term) { success ->
                    if (success) showCreateCourseDialog = false
                } ?: run { showCreateCourseDialog = false }
            },
            isLoading = uiState.isActionLoading
        )
    }

    if (showJoinCourseDialog) {
        JoinCourseDialog(
            onDismiss = { showJoinCourseDialog = false },
            onJoin = { joinCode ->
                viewModel?.joinCourse(joinCode) { success ->
                    if (success) showJoinCourseDialog = false
                } ?: run { showJoinCourseDialog = false }
            },
            isLoading = uiState.isActionLoading
        )
    }
}

@Composable
fun RoleBadgeBanner(role: UserRole) {
    val (badgeText, badgeColor) = when (role) {
        UserRole.TEACHER -> "Flujo de Docente: Gestión de Cursos y Sílabo" to GlifoTheme.colors.accent
        UserRole.STUDENT -> "Flujo de Estudiante: Apuntes, Estudio y Cobertura" to GlifoTheme.colors.surfaceHigh
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlifoTheme.colors.surfaceHigh, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(badgeColor, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = badgeText,
                color = GlifoTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CourseCardByRole(
    course: CourseResponse,
    role: UserRole,
    onCourseClick: () -> Unit
) {
    GlifoCard(onClick = onCourseClick) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = course.emoji ?: "📚",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = course.name,
                            color = GlifoTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = course.code,
                            color = GlifoTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                if (role == UserRole.TEACHER) {
                    // Teacher view: display joinCode to share with students
                    Box(
                        modifier = Modifier
                            .background(GlifoTheme.colors.surfaceHigh, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Código: ${course.joinCode}",
                            color = GlifoTheme.colors.accentText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Student view: show active badge
                    Text(
                        text = "Activo",
                        color = GlifoTheme.colors.accentText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCoursesPlaceholder(role: UserRole, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (role == UserRole.TEACHER) "No has creado ningún curso aún" else "No estás matriculado en ningún curso",
            color = GlifoTheme.colors.textSecondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        GlifoButton(
            text = if (role == UserRole.TEACHER) "Crear mi primer curso" else "Unirme a un curso",
            onClick = onAction,
            modifier = Modifier.width(220.dp)
        )
    }
}

@Composable
fun CreateCourseDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, code: String, term: String) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var term by remember { mutableStateOf("I-2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Crear Nuevo Curso", color = GlifoTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlifoTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre del curso (ej. Cálculo II)"
                )
                GlifoTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = "Código (ej. MAT-202)"
                )
                GlifoTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = "Período (ej. I-2026)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, code, term) },
                enabled = name.isNotBlank() && code.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = GlifoTheme.colors.accent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GlifoTheme.colors.onAccent)
                } else {
                    Text("Crear Curso", color = GlifoTheme.colors.onAccent)
                }
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

@Composable
fun JoinCourseDialog(
    onDismiss: () -> Unit,
    onJoin: (joinCode: String) -> Unit,
    isLoading: Boolean
) {
    var joinCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Unirse a un Curso", color = GlifoTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ingresa el código proporcionado por tu docente.",
                    color = GlifoTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                GlifoTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase() },
                    label = "Código de matrícula (ej. MAT202)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(joinCode) },
                enabled = joinCode.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = GlifoTheme.colors.accent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GlifoTheme.colors.onAccent)
                } else {
                    Text("Unirme", color = GlifoTheme.colors.onAccent)
                }
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

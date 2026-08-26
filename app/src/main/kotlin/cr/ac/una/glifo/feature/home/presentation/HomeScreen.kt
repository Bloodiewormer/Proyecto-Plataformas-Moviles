package cr.ac.una.glifo.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
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
                    role = GlifoRole.Student,
                    currentScreen = "home",
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Top AppBar
                GlifoTopAppBar(
                    title = "Cálculo II",
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
                GlifoBanner(
                    text = "2 capturas en cola · se envían al volver la red",
                    icon = Icons.Default.Sync
                )

                // Body content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    // Cobertura del curso Card
                    GlifoCard(
                        onClick = onNavigateToCoverage
                    ) {
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
                                text = "68%",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentText
                            )
                        }

                        CovBar(
                            v = 54f,
                            r = 20f,
                            e = 14f,
                            u = 12f,
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
                                text = "+2 desde el 8 ago",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.verified
                            )
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
                        GlifoCard(
                            onClick = onNavigateToFlashcards,
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
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
                                    text = "12 flashcards",
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

                        // Quiz Box
                        GlifoCard(
                            onClick = onNavigateToQuiz,
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
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
                                    text = "1 quiz",
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

                    // Section: Últimos apuntes
                    Text(
                        text = "ÚLTIMOS APUNTES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )

                    GlifoCard(tight = true) {
                        // Note 1
                        GlifoItem(
                            onClick = { onNavigateToNoteDetail(1L) }
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
                                    text = "Integrales impropias",
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = "12 ago · 3 páginas",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                GlifoConfidenceChip(state = ConfidenceState.Verified)
                                GlifoConfidenceChip(state = ConfidenceState.Repaired)
                                GlifoConfidenceChip(state = ConfidenceState.Escalated)
                            }
                        }

                        // Note 2
                        GlifoItem(
                            showBottomBorder = false,
                            onClick = { onNavigateToNoteDetail(2L) }
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
                                    text = "Criterio de comparación",
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = "7 ago · 2 páginas",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                GlifoConfidenceChip(state = ConfidenceState.Verified)
                                GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(70.dp))
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
    onNavigateToSettings: () -> Unit,
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
                    role = GlifoRole.Teacher,
                    currentScreen = "home_teacher",
                    activeCourseName = "EIF411",
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
                title = "EIF411",
                subtitle = "Curso activo",
                hasDropdown = true,
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                // Group coverage card
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
                            text = "54%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accentText
                        )
                    }

                    CovBar(
                        v = 40f,
                        r = 24f,
                        e = 26f,
                        u = 10f,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )

                    Text(
                        text = "27 estudiantes inscritos · promedio del curso",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
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

                GlifoCard(tight = true) {
                    GlifoItem(compact = true, onClick = onNavigateToGaps) {
                        GlifoConfidenceChip(state = ConfidenceState.Escalated)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "4.3 Sockets e hilos", fontSize = 13.sp, color = colors.textPrimary)
                            Text(text = "19 de 27 sin cubrir", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Text(text = "70%", fontSize = 12.sp, color = colors.textSecondary)
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    GlifoItem(compact = true, onClick = onNavigateToGaps) {
                        GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "6.1 Seguridad y permisos", fontSize = 13.sp, color = colors.textPrimary)
                            Text(text = "14 de 27 sin cubrir", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Text(text = "52%", fontSize = 12.sp, color = colors.textSecondary)
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    GlifoItem(compact = true, showBottomBorder = false, onClick = onNavigateToGaps) {
                        GlifoConfidenceChip(state = ConfidenceState.Repaired)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "3.2 Jetpack Compose", fontSize = 13.sp, color = colors.textPrimary)
                            Text(text = "8 de 27 sin cubrir", fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Text(text = "30%", fontSize = 12.sp, color = colors.textSecondary)
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
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

// ── Screen B4: Inicio Administrador ────────────────────────────────────────────
@Composable
fun AdminHomeScreen(
    onNavigateToUsers: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToRoles: () -> Unit,
    onNavigateToAiCost: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
                    currentScreen = "home_admin",
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        when (route) {
                            "admin_users" -> onNavigateToUsers()
                            "teacher_courses" -> onNavigateToCourses()
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
                    GlifoCard(
                        onClick = onNavigateToUsers,
                        modifier = Modifier.weight(1f).height(84.dp)
                    ) {
                        Text("142", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Usuarios activos", fontSize = 12.sp, color = colors.textSecondary)
                    }
                    GlifoCard(
                        onClick = onNavigateToAiCost,
                        modifier = Modifier.weight(1f).height(84.dp)
                    ) {
                        Text("64%", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Sin llamadas de pago", fontSize = 12.sp, color = colors.textSecondary)
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
                        Text("4 pendientes", fontSize = 12.sp, color = colors.textSecondary)
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

                GlifoCard(tight = true) {
                    val rolesData = listOf(
                        Triple("Estudiante", "131", colors.accent to 0.88f),
                        Triple("Docente", "9", colors.accent to 0.08f),
                        Triple("Administrador", "2", colors.alert to 0.03f)
                    )
                    rolesData.forEachIndexed { i, (label, count, barInfo) ->
                        val (barColor, fraction) = barInfo
                        GlifoItem(showBottomBorder = i < rolesData.size - 1) {
                            Text(label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
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
                                count,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textSecondary,
                                modifier = Modifier.width(28.dp),
                                textAlign = TextAlign.End
                            )
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

                GlifoCard(tight = true) {
                    GlifoItem(compact = true) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "dgonzalez@est.una.ac.cr",
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "hace 2 h",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    GlifoItem(compact = true, showBottomBorder = false) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "fugalde@est.una.ac.cr",
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "hace 5 h",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

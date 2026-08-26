package cr.ac.una.glifo.feature.teacher.presentation

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cr.ac.una.glifo.core.data.dummy.DummyCourseProvider
import cr.ac.una.glifo.core.data.dummy.DummyTeacherProvider
import cr.ac.una.glifo.core.data.session.SessionManager
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

private const val TEACHER_ACTIVE_COURSE_ID = 3L

// ── Screen I1: Cursos Docente ─────────────────────────────────────────────────
@Composable
fun TeacherCoursesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToNewCourse: () -> Unit,
    onNavigateToSyllabus: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val user = SessionManager.currentUser
    val courses = DummyCourseProvider.getTeacherCourses

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
                    currentScreen = "teacher_courses",
                    userName = user?.fullName ?: "Maikol Guzmán",
                    userEmail = user?.email ?: "docente@una.ac.cr",
                    userInitials = user?.initials ?: "MG",
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            GlifoTopAppBar(
                title = "Mis cursos",
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = onNavigateToNewCourse) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Crear", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
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
                courses.forEachIndexed { i, course ->
                    val stats = DummyCourseProvider.getStats(course.id)
                    GlifoStaggeredEntry(index = i) {
                        if (stats.hasSyllabus) {
                            GlifoCard(
                                onClick = onNavigateToSyllabus,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = course.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    GlifoBadge(text = "ACTIVO")
                                }
                                Text("${course.code} · II Ciclo 2026", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 8.dp))
                                Text("${stats.coveragePercent}% de cobertura · ${stats.studentCount} estudiantes", fontSize = 12.sp, color = colors.textSecondary)
                            }
                        } else {
                            GlifoCard(
                                onClick = onNavigateToSyllabus,
                                borderColor = colors.alertLine,
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                Text(text = course.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text("${course.code} · II Ciclo 2026", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .background(colors.alertFaint, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = colors.alert, modifier = Modifier.size(14.dp))
                                        Text("Sin temario · sin cobertura", fontSize = 12.sp, color = colors.alert)
                                    }
                                }
                            }
                        }
                    }
                }

                GlifoButton(
                    text = "Crear nuevo curso",
                    variant = GlifoButtonVariant.Secondary,
                    icon = Icons.Default.Add,
                    onClick = onNavigateToNewCourse
                )
            }
        }
    }
}

// ── Screen I2: Nuevo Curso ────────────────────────────────────────────────────
@Composable
fun TeacherNewCourseScreen(
    onNavigateBack: () -> Unit,
    onCreateSuccess: () -> Unit
) {
    val colors = GlifoTheme.colors
    val clipboard = LocalClipboardManager.current

    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("II Ciclo 2026") }
    var joinCode by remember { mutableStateOf(generateJoinCode()) }
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Nuevo curso",
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            GlifoField(label = "Nombre del curso", value = name, placeholder = "p. ej. Plataformas Móviles", onValueChange = { name = it })
            Spacer(modifier = Modifier.height(10.dp))
            GlifoField(label = "Código institucional", value = code, placeholder = "p. ej. EIF411", onValueChange = { code = it })
            Spacer(modifier = Modifier.height(10.dp))
            GlifoField(label = "Ciclo", value = cycle, onValueChange = { cycle = it })

            Text(
                text = "CÓDIGO DE INSCRIPCIÓN",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
            )

            GlifoCard(borderColor = colors.accentLine) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = joinCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        color = colors.accentText,
                        letterSpacing = 3.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Generar otro código",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    joinCode = generateJoinCode()
                                    copied = false
                                }
                        )
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copiar código",
                            tint = if (copied) colors.verified else colors.accentText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    clipboard.setText(AnnotatedString(joinCode))
                                    copied = true
                                }
                        )
                    }
                }
                Text("Los estudiantes lo escriben a mano, por eso no lleva letras ambiguas.", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlifoCard(
                backgroundColor = colors.accentFaint,
                borderColor = colors.accentLine
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(18.dp))
                    Column {
                        Text("El temario va después", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("Sin temario el curso funciona, pero no calcula cobertura.", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlifoButton(
                text = "Crear curso",
                enabled = name.isNotBlank() && code.isNotBlank(),
                onClick = onCreateSuccess
            )
        }
    }
}

private fun generateJoinCode(): String {
    val chars = "23456789ABCDEFGHJKMNPQRSTUVWXYZ" // no ambiguous 0/O/1/I/L
    return "GLF-" + (1..4).map { chars.random() }.joinToString("")
}

// ── Screen I3: Publicar Temario ───────────────────────────────────────────────
@Composable
fun TeacherSyllabusScreen(
    onNavigateBack: () -> Unit,
    onPublishSuccess: () -> Unit
) {
    val colors = GlifoTheme.colors
    var version by remember { mutableStateOf(0) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    val units = remember(version) { DummyTeacherProvider.getSyllabusUnits() }
    val totalTopics = units.sumOf { it.topicCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Temario · EIF411",
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                TextButton(onClick = onPublishSuccess) {
                    Text("Publicar", color = colors.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            GlifoCard(borderColor = colors.accentLine) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(DummyTeacherProvider.getSyllabusFileName(), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("1.2 MB · procesado hace 2 minutos", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                Text(
                    text = "Se extrajeron ${units.size} unidades y $totalTopics temas. Revisalos antes de publicar: el temario se procesa una sola vez.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            }

            Text(
                text = "ESTRUCTURA DETECTADA",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                units.forEachIndexed { unitIdx, unit ->
                    GlifoItem(compact = true, backgroundColor = colors.surfaceHigh, showBottomBorder = unit.topics.isEmpty() && unitIdx < units.size - 1) {
                        Icon(
                            imageVector = if (unit.topics.isNotEmpty()) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(unit.label, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Text("${unit.topicCount} temas", fontSize = 12.sp, color = colors.textSecondary)
                        Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    unit.topics.forEachIndexed { i, topic ->
                        GlifoItem(
                            compact = true,
                            backgroundColor = if (topic.hasIssue) colors.alertFaint else Color.Transparent,
                            showBottomBorder = !(unitIdx == units.size - 1 && i == unit.topics.size - 1),
                            modifier = Modifier.padding(start = 24.dp)
                        ) {
                            Text(topic.label, fontSize = 15.sp, color = if (topic.hasIssue) colors.alert else colors.textPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Edit, contentDescription = null, tint = if (topic.hasIssue) colors.alert else colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlifoButton(
                text = "Agregar un tema a mano",
                variant = GlifoButtonVariant.Secondary,
                icon = Icons.Default.Add,
                onClick = { showAddTopicDialog = true }
            )
        }
    }

    if (showAddTopicDialog) {
        AddTopicDialog(
            onDismiss = { showAddTopicDialog = false },
            onAdd = { label ->
                DummyTeacherProvider.addSyllabusTopic(label)
                version++
                showAddTopicDialog = false
            }
        )
    }
}

@Composable
private fun AddTopicDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val colors = GlifoTheme.colors
    var label by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        GlifoCard {
            Text("Agregar tema", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Se agrega a la primera unidad (3 · Series).", fontSize = 12.sp, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            GlifoField(label = "Nombre del tema", value = label, placeholder = "p. ej. 3.6 Series alternadas", onValueChange = { label = it })
            Spacer(modifier = Modifier.height(14.dp))
            GlifoButton(text = "Agregar", enabled = label.isNotBlank(), onClick = { onAdd(label) })
        }
    }
}

// ── Screen I4: Glosario Docente ───────────────────────────────────────────────
@Composable
fun TeacherGlossaryScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val user = SessionManager.currentUser
    var version by remember { mutableStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val allEntries = remember(version) { DummyTeacherProvider.getGlossaryEntries() }
    val entries = allEntries.filter { query.isBlank() || it.term.contains(query, ignoreCase = true) }
    val suggestions = DummyTeacherProvider.getGlossarySuggestions()

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
                    currentScreen = "glossary",
                    userName = user?.fullName ?: "Maikol Guzmán",
                    userEmail = user?.email ?: "docente@una.ac.cr",
                    userInitials = user?.initials ?: "MG",
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            GlifoTopAppBar(
                title = "Glosario",
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
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    GlifoField(label = "Buscar término", value = query, onValueChange = { query = it }, isFocused = true)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                GlifoCard(
                    backgroundColor = colors.accentFaint,
                    borderColor = colors.accentLine
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Esto no es un diccionario", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text("Cada entrada sube la confianza del reconocimiento sobre ese símbolo para todo el curso.", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                Text(
                    text = "${entries.size} ENTRADAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                GlifoCard(tight = true) {
                    entries.forEachIndexed { i, entry ->
                        GlifoItem(showBottomBorder = i < entries.size - 1) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = entry.term, fontSize = 16.sp, color = colors.textPrimary)
                                Text(text = entry.notation, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                            val bg = if (entry.isSymbol) colors.repairedSoft else colors.accentSoft
                            val fg = if (entry.isSymbol) colors.repaired else colors.accentText
                            GlifoBadge(text = entry.tag, backgroundColor = bg, textColor = fg)
                        }
                    }
                }

                if (suggestions.isNotEmpty()) {
                    Text(
                        text = "SUGERIDAS POR CORRECCIONES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )

                    GlifoCard(tight = true) {
                        suggestions.forEachIndexed { i, s ->
                            GlifoItem(compact = true, showBottomBorder = i < suggestions.size - 1) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.term, fontSize = 15.sp, color = colors.textPrimary)
                                    Text(s.meta, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Descartar", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                                    Icon(Icons.Default.Check, contentDescription = "Aceptar", tint = colors.verified, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                if (entries.isEmpty()) {
                    GlifoBanner(text = "Ningún término coincide con esa búsqueda.", modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddGlossaryEntryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { term, notation, isSymbol ->
                DummyTeacherProvider.addGlossaryEntry(term, notation, isSymbol)
                version++
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddGlossaryEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (term: String, notation: String, isSymbol: Boolean) -> Unit
) {
    val colors = GlifoTheme.colors
    var term by remember { mutableStateOf("") }
    var notation by remember { mutableStateOf("") }
    var isSymbol by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlifoCard {
            Text("Agregar al glosario", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            GlifoField(label = "Término", value = term, onValueChange = { term = it })
            Spacer(modifier = Modifier.height(8.dp))
            GlifoField(label = "Notación", value = notation, placeholder = "p. ej. \\sum o sinónimos separados por ·", onValueChange = { notation = it })
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "Término", true to "Símbolo").forEach { (value, label) ->
                    val selected = isSymbol == value
                    Box(
                        modifier = Modifier
                            .background(if (selected) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                            .clickable { isSymbol = value }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 12.sp, color = if (selected) colors.accentText else colors.textSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            GlifoButton(
                text = "Agregar",
                enabled = term.isNotBlank() && notation.isNotBlank(),
                onClick = { onAdd(term, notation, isSymbol) }
            )
        }
    }
}

// ── Screen I5: Brechas del Grupo ──────────────────────────────────────────────
@Composable
fun TeacherGapsScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToTopicDetail: (String) -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user = SessionManager.currentUser
    val stats = DummyCourseProvider.getStats(TEACHER_ACTIVE_COURSE_ID)
    val gaps = DummyTeacherProvider.getGaps()

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
                    currentScreen = "gaps",
                    userName = user?.fullName ?: "Maikol Guzmán",
                    userEmail = user?.email ?: "docente@una.ac.cr",
                    userInitials = user?.initials ?: "MG",
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigateDrawer(route)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            GlifoTopAppBar(
                title = "EIF411",
                subtitle = "Brechas de",
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = {
                        val summary = buildString {
                            appendLine("Brechas del grupo · EIF411")
                            appendLine("${stats.studentCount} estudiantes · ${stats.coveragePercent}% de cobertura promedio")
                            appendLine()
                            gaps.forEach { gap -> appendLine("• ${gap.label} — ${gap.fraction} sin cubrir (${gap.percent.toInt()}%)") }
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Brechas del grupo · EIF411")
                            putExtra(Intent.EXTRA_TEXT, summary)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Exportar brechas"))
                    }) {
                        Icon(Icons.Default.Upload, contentDescription = "Exportar", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
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
                GlifoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("${stats.studentCount} estudiantes", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("${stats.coveragePercent}%", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                    }
                    CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 10.dp))
                    Text("Cobertura promedio del grupo · actualizada hoy", fontSize = 12.sp, color = colors.textSecondary)
                }

                Text(
                    text = "TEMAS CON MÁS VACÍOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                GlifoCard(tight = true) {
                    gaps.forEachIndexed { i, gap ->
                        GlifoItem(showBottomBorder = i < gaps.size - 1, onClick = { onNavigateToTopicDetail(gap.label) }) {
                            GlifoConfidenceChip(state = gap.state)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = gap.label, fontSize = 15.sp, color = colors.textPrimary)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .padding(top = 7.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.surfaceHigh)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(gap.percent / 100f)
                                            .background(
                                                when (gap.state) {
                                                    ConfidenceState.Escalated -> colors.escalated
                                                    ConfidenceState.Uncertain -> colors.uncertain
                                                    else -> colors.repaired
                                                }
                                            )
                                    )
                                }
                            }
                            Text(text = gap.fraction, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlifoCard(
                    backgroundColor = colors.uncertainFaint,
                    borderColor = colors.uncertainLine
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        GlifoConfidenceChip(state = ConfidenceState.Uncertain, modifier = Modifier.padding(top = 2.dp))
                        Column {
                            Text("6.1 es un caso distinto", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text("11 de los 14 tienen apuntes del tema, pero ilegibles. No es que no lo estudiaran.", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                    Text("Datos agregados. El docente nunca ve apuntes individuales.", fontSize = 12.sp, color = colors.textSecondary)
                }
            }
        }
    }
}

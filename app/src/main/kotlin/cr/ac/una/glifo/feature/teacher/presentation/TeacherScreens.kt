package cr.ac.una.glifo.feature.teacher.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

// ── Screen I1: Cursos Docente ─────────────────────────────────────────────────
@Composable
fun TeacherCoursesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToNewCourse: () -> Unit,
    onNavigateToSyllabus: () -> Unit,
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
                    role = GlifoRole.Teacher,
                    currentScreen = "teacher_courses",
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
                // Course 1
                GlifoCard(
                    onClick = onNavigateToSyllabus,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Plataformas Móviles", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        GlifoBadge(text = "ACTIVO")
                    }
                    Text("EIF411 · II Ciclo 2026", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    CovBar(v = 40f, r = 24f, e = 26f, u = 10f, modifier = Modifier.padding(vertical = 8.dp))
                    Text("54% de cobertura · 27 estudiantes", fontSize = 12.sp, color = colors.textSecondary)
                }

                // Course 2: No syllabus
                GlifoCard(
                    onClick = onNavigateToSyllabus,
                    borderColor = colors.alertLine,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Text(text = "Álgebra Lineal", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Text("MAT-101 · II Ciclo 2026", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
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
            GlifoField(label = "Nombre del curso", value = "Plataformas Móviles")
            Spacer(modifier = Modifier.height(10.dp))
            GlifoField(label = "Código institucional", value = "EIF411")
            Spacer(modifier = Modifier.height(10.dp))
            GlifoField(label = "Ciclo", value = "II Ciclo 2026")

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
                        text = "GLF-4K2P",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        color = colors.accentText,
                        letterSpacing = 3.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(18.dp))
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
                onClick = onCreateSuccess
            )
        }
    }
}

// ── Screen I3: Publicar Temario ───────────────────────────────────────────────
@Composable
fun TeacherSyllabusScreen(
    onNavigateBack: () -> Unit,
    onPublishSuccess: () -> Unit
) {
    val colors = GlifoTheme.colors

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
                        Text("programa-EIF411.pdf", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("1.2 MB · procesado hace 2 minutos", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
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
                    text = "Se extrajeron 6 unidades y 33 temas. Revisalos antes de publicar: el temario se procesa una sola vez.",
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
                GlifoItem(compact = true, backgroundColor = colors.surfaceHigh) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(18.dp))
                    Text("3 · Series", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("5 temas", fontSize = 12.sp, color = colors.textSecondary)
                    Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                GlifoItem(compact = true, modifier = Modifier.padding(start = 24.dp)) {
                    Text("3.1 Sucesiones", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                GlifoItem(compact = true, modifier = Modifier.padding(start = 24.dp)) {
                    Text("3.2 Series geométricas", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                GlifoItem(compact = true, backgroundColor = colors.alertFaint, modifier = Modifier.padding(start = 24.dp)) {
                    Text("3.3 Criterío de comparacion", fontSize = 15.sp, color = colors.alert, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, contentDescription = null, tint = colors.alert, modifier = Modifier.size(18.dp))
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("4 · Cálculo vectorial", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("7 temas", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlifoButton(
                text = "Agregar un tema a mano",
                variant = GlifoButtonVariant.Secondary,
                icon = Icons.Default.Add,
                onClick = {}
            )
        }
    }
}

// ── Screen I4: Glosario Docente ───────────────────────────────────────────────
@Composable
fun TeacherGlossaryScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors

    val entries = listOf(
        Pair(Triple("Sumatoria", "\\sum_{n=1}^{\\infty}", "SÍMBOLO"), Pair(colors.repairedSoft, colors.repaired)),
        Pair(Triple("Converge", "conv. · cnv · converg.", "TÉRMINO"), Pair(colors.accentSoft, colors.accentText)),
        Pair(Triple("Radio de convergencia", "R", "TÉRMINO"), Pair(colors.accentSoft, colors.accentText)),
        Pair(Triple("Integral impropia", "\\int_a^{\\infty}", "SÍMBOLO"), Pair(colors.repairedSoft, colors.repaired)),
        Pair(Triple("Límite n→∞", "\\lim_{n \\to \\infty}", "SÍMBOLO"), Pair(colors.repairedSoft, colors.repaired))
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
                    role = GlifoRole.Teacher,
                    currentScreen = "glossary",
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
                title = "Glosario",
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = colors.textSecondary, modifier = Modifier.size(20.dp)) }
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
                    text = "42 ENTRADAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                GlifoCard(tight = true) {
                    entries.forEachIndexed { i, (item, tagColors) ->
                        GlifoItem(showBottomBorder = i < entries.size - 1) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.first, fontSize = 16.sp, color = colors.textPrimary)
                                Text(text = item.second, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                            GlifoBadge(text = item.third, backgroundColor = tagColors.first, textColor = tagColors.second)
                        }
                    }
                }

                Text(
                    text = "SUGERIDAS POR CORRECCIONES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                )

                GlifoCard(tight = true) {
                    GlifoItem(compact = true, showBottomBorder = false) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("diverge", fontSize = 15.sp, color = colors.textPrimary)
                            Text("corregido 7 veces esta semana", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Screen I5: Brechas del Grupo ──────────────────────────────────────────────
@Composable
fun TeacherGapsScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToTopicDetail: () -> Unit,
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
                    role = GlifoRole.Teacher,
                    currentScreen = "gaps",
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
                title = "EIF411",
                subtitle = "Brechas de",
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Upload, contentDescription = "Exportar", tint = colors.textSecondary, modifier = Modifier.size(20.dp)) }
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
                        Text("27 estudiantes", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("54%", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                    }
                    CovBar(v = 40f, r = 24f, e = 26f, u = 10f, modifier = Modifier.padding(vertical = 10.dp))
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
                    listOf(
                        Triple(ConfidenceState.Escalated, "4.3 Sockets e hilos", Pair(70f, "19/27")),
                        Triple(ConfidenceState.Uncertain, "6.1 Seguridad y permisos", Pair(52f, "14/27")),
                        Triple(ConfidenceState.Repaired, "3.2 Jetpack Compose", Pair(30f, "8/27"))
                    ).forEachIndexed { i, (state, label, meta) ->
                        GlifoItem(showBottomBorder = i < 2, onClick = onNavigateToTopicDetail) {
                            GlifoConfidenceChip(state = state)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = label, fontSize = 15.sp, color = colors.textPrimary)
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
                                            .fillMaxWidth(meta.first / 100f)
                                            .background(
                                                when (state) {
                                                    ConfidenceState.Escalated -> colors.escalated
                                                    ConfidenceState.Uncertain -> colors.uncertain
                                                    else -> colors.repaired
                                                }
                                            )
                                    )
                                }
                            }
                            Text(text = meta.second, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = colors.textSecondary)
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

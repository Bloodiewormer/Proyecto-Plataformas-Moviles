package cr.ac.una.glifo.feature.coverage.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun CoverageScreen(
    courseId: Long,
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToTopicDetail: (String) -> Unit,
    onNavigateToDelta: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors

    val topics = listOf(
        Triple(ConfidenceState.Verified, "3.1 Sucesiones", Pair(null, "0.92")),
        Triple(ConfidenceState.Verified, "3.2 Series geométricas", Pair(null, "0.88")),
        Triple(ConfidenceState.Repaired, "3.3 Comparación", Pair("Falta el caso del límite", "0.54")),
        Triple(ConfidenceState.Escalated, "3.4 Series de potencias", Pair("Sin evidencia en tus apuntes", "0.11")),
        Triple(ConfidenceState.Uncertain, "3.5 Radio de convergencia", Pair("Apunte ilegible en esa sección", "—"))
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
                    role = GlifoRole.Student,
                    currentScreen = "coverage",
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
                title = "Cálculo II",
                subtitle = "Cobertura de",
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = onNavigateToDelta) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Calendario", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            )

            // Banner delta
            GlifoBanner(
                text = "",
                content = {
                    Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    Text("+2 temas desde el 8 de agosto", fontSize = 12.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.clickable(onClick = onNavigateToDelta)
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("33 temas", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("68%", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                    }

                    CovBar(
                        v = 54f,
                        r = 20f,
                        e = 14f,
                        u = 12f,
                        height = 10.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlifoConfidenceChip(state = ConfidenceState.Verified)
                                Text("18 sólidos", fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlifoConfidenceChip(state = ConfidenceState.Escalated)
                                Text("5 ausentes", fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlifoConfidenceChip(state = ConfidenceState.Repaired)
                                Text("7 parciales", fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                                Text("3 dudosos", fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }

                Text(
                    text = "UNIDAD 3 · SERIES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                GlifoCard(tight = true) {
                    topics.forEachIndexed { i, (chip, name, meta) ->
                        GlifoItem(
                            compact = true,
                            showBottomBorder = i < topics.size - 1,
                            onClick = { onNavigateToTopicDetail(name) }
                        ) {
                            GlifoConfidenceChip(state = chip)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = name, fontSize = 13.sp, color = colors.textPrimary)
                                if (meta.first != null) {
                                    Text(text = meta.first!!, fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                            Text(
                                text = meta.second,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

package cr.ac.una.glifo.feature.course.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyCourseProvider
import cr.ac.una.glifo.core.data.session.SessionManager
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun CoursesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToCourseDetail: (Long) -> Unit,
    onNavigateToJoinCourse: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val user = SessionManager.currentUser

    val courses = DummyCourseProvider.getCourses

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
                    currentScreen = "courses",
                    userName = user?.fullName ?: "Brandon Brenes",
                    userEmail = user?.email ?: "estudiante@est.una.ac.cr",
                    userInitials = user?.initials ?: "BB",
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
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
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
                        GlifoCard(
                            onClick = { onNavigateToCourseDetail(course.id) },
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = course.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                if (i == 0) GlifoBadge(text = "ACTIVO")
                            }
                            Text(
                                text = "${course.code} · ${stats.instructorName}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${stats.coveragePercent}% de cobertura", fontSize = 12.sp, color = colors.textSecondary)
                                Text(
                                    text = if (stats.weeklyDeltaPoints > 0) "+${stats.weeklyDeltaPoints}" else "sin cambios",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (stats.weeklyDeltaPoints > 0) colors.verified else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                GlifoButton(
                    text = "Unirme con un código",
                    variant = GlifoButtonVariant.Secondary,
                    icon = Icons.Default.Add,
                    onClick = onNavigateToJoinCourse
                )
            }
        }
    }
}

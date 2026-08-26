package cr.ac.una.glifo.feature.coverage.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ShowChart
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
import cr.ac.una.glifo.core.data.dummy.DummyCoverageProvider
import cr.ac.una.glifo.core.data.session.SessionManager
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
    val user = SessionManager.currentUser

    val course = DummyCourseProvider.findCourse(courseId)
    val summary = DummyCoverageProvider.getSummary(courseId)
    val topics = DummyCoverageProvider.getTopics(courseId)

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
                title = course?.name ?: "Curso",
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
                    Text(summary.deltaLabel, fontSize = 12.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
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
                GlifoStaggeredEntry(index = 0) {
                    GlifoCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${summary.topicCount} temas", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text("${summary.coveragePercent}%", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                        }

                        CovBar(
                            v = summary.v,
                            r = summary.r,
                            e = summary.e,
                            u = summary.u,
                            height = 10.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlifoConfidenceChip(state = ConfidenceState.Verified)
                                    Text("${summary.solidCount} sólidos", fontSize = 12.sp, color = colors.textSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlifoConfidenceChip(state = ConfidenceState.Escalated)
                                    Text("${summary.absentCount} ausentes", fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlifoConfidenceChip(state = ConfidenceState.Repaired)
                                    Text("${summary.partialCount} parciales", fontSize = 12.sp, color = colors.textSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                                    Text("${summary.doubtfulCount} dudosos", fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = summary.unitLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                GlifoStaggeredEntry(index = 1) {
                    GlifoCard(tight = true) {
                        topics.forEachIndexed { i, topic ->
                            GlifoItem(
                                compact = true,
                                showBottomBorder = i < topics.size - 1,
                                onClick = { onNavigateToTopicDetail(topic.name) }
                            ) {
                                GlifoConfidenceChip(state = topic.state)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = topic.name, fontSize = 13.sp, color = colors.textPrimary)
                                    if (topic.note != null) {
                                        Text(text = topic.note, fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                }
                                Text(
                                    text = topic.score,
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
}

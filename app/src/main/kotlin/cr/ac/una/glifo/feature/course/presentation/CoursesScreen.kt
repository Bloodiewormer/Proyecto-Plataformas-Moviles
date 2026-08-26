package cr.ac.una.glifo.feature.course.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
                // Course 1
                GlifoCard(
                    onClick = { onNavigateToCourseDetail(1L) },
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Cálculo II", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        GlifoBadge(text = "ACTIVO")
                    }
                    Text(
                        text = "MAT-202 · Prof. Rodríguez",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    CovBar(v = 54f, r = 20f, e = 14f, u = 12f, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "68% de cobertura", fontSize = 12.sp, color = colors.textSecondary)
                        Text(text = "+2", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.verified)
                    }
                }

                // Course 2
                GlifoCard(
                    onClick = { onNavigateToCourseDetail(2L) },
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text(text = "Física General II", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Text(
                        text = "FIS-204 · Prof. Mora",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    CovBar(v = 31f, r = 14f, e = 25f, u = 30f, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "45% de cobertura", fontSize = 12.sp, color = colors.textSecondary)
                        Text(text = "sin cambios", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }

                // Course 3
                GlifoCard(
                    onClick = { onNavigateToCourseDetail(3L) },
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Text(text = "Programación IV", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Text(
                        text = "EIF-209 · Prof. Guzmán",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    CovBar(v = 70f, r = 12f, e = 10f, u = 8f, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "82% de cobertura", fontSize = 12.sp, color = colors.textSecondary)
                        Text(text = "+5", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.verified)
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

package cr.ac.una.glifo.feature.sync.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun SyncQueueScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
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
                    currentScreen = "sync",
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
                title = "Sincronización",
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            GlifoBanner(
                text = "Sincronizando 2 apuntes…",
                icon = Icons.Default.Sync
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Item 1: Syncing
                GlifoItem(onClick = { onNavigateToNoteDetail(1L) }) {
                    GlifoThumb {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Integrales impropias · p.1", fontSize = 13.sp, color = colors.textPrimary)
                        Text(text = "12 ago · 9:41 AM", fontSize = 11.sp, color = colors.textSecondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.surfaceHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.72f)
                                    .background(colors.accent)
                            )
                        }
                    }
                }

                // Item 2: Queued
                GlifoItem(onClick = { onNavigateToNoteDetail(2L) }) {
                    GlifoThumb {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Criterio de comparación · p.1", fontSize = 13.sp, color = colors.textPrimary)
                        Text(text = "7 ago · 8:20 AM", fontSize = 11.sp, color = colors.textSecondary)
                    }
                    GlifoBadge(text = "EN COLA")
                }

                // Item 3: Done
                GlifoItem(onClick = { onNavigateToNoteDetail(3L) }) {
                    GlifoThumb {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Series de potencias · p.2", fontSize = 13.sp, color = colors.textPrimary)
                        Text(text = "2 ago · 7:55 AM", fontSize = 11.sp, color = colors.textSecondary)
                    }
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(16.dp))
                }

                // Item 4: Done
                GlifoItem(showBottomBorder = false, onClick = { onNavigateToNoteDetail(4L) }) {
                    GlifoThumb {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Prueba de la integral · p.1", fontSize = 13.sp, color = colors.textPrimary)
                        Text(text = "28 jul · 10:12 AM", fontSize = 11.sp, color = colors.textSecondary)
                    }
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

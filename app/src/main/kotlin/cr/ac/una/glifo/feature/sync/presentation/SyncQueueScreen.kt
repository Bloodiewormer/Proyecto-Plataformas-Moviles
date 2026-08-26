package cr.ac.una.glifo.feature.sync.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummySyncProvider
import cr.ac.una.glifo.core.data.dummy.SyncStatus
import cr.ac.una.glifo.core.data.session.SessionManager
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun SyncQueueScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val user = SessionManager.currentUser

    val items = DummySyncProvider.getQueueItems()
    val syncingCount = items.count { it.status == SyncStatus.Syncing }

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
                    userName = user?.fullName ?: "Brandon Brenes",
                    userEmail = user?.email ?: "estudiante@est.una.ac.cr",
                    userInitials = user?.initials ?: "BB",
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
                title = "Sincronización",
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            if (syncingCount > 0) {
                GlifoBanner(
                    text = "Sincronizando $syncingCount ${if (syncingCount == 1) "apunte" else "apuntes"}…",
                    icon = Icons.Default.Sync
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEachIndexed { i, item ->
                    GlifoStaggeredEntry(index = i) {
                        GlifoItem(
                            showBottomBorder = i < items.size - 1,
                            onClick = { onNavigateToNoteDetail(item.noteId) }
                        ) {
                            GlifoThumb {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.title, fontSize = 13.sp, color = colors.textPrimary)
                                Text(text = item.timestamp, fontSize = 11.sp, color = colors.textSecondary)
                                if (item.status == SyncStatus.Syncing) {
                                    var animate by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) { animate = true }
                                    val progress by animateFloatAsState(
                                        targetValue = if (animate) item.progress else 0f,
                                        animationSpec = tween(900),
                                        label = "sync-progress"
                                    )
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
                                                .fillMaxWidth(progress)
                                                .background(colors.accent)
                                        )
                                    }
                                }
                            }
                            when (item.status) {
                                SyncStatus.Queued -> GlifoBadge(text = "EN COLA")
                                SyncStatus.Done -> Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(16.dp))
                                SyncStatus.Syncing -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

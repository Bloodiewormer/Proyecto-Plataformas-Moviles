package cr.ac.una.glifo.feature.note.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors

    val notes = listOf(
        Triple("Integrales impropias", "12 ago · 3 páginas", listOf(ConfidenceState.Verified, ConfidenceState.Repaired, ConfidenceState.Escalated)),
        Triple("Criterio de comparación", "7 ago · 2 páginas", listOf(ConfidenceState.Verified, ConfidenceState.Uncertain)),
        Triple("Series de potencias", "2 ago · 4 páginas", listOf(ConfidenceState.Verified, ConfidenceState.Verified, ConfidenceState.Repaired)),
        Triple("Prueba de la integral", "28 jul · 2 páginas", listOf(ConfidenceState.Verified))
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
                    currentScreen = "notes",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GlifoTopAppBar(
                    title = "Apuntes",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                        }
                    }
                )

                GlifoBanner(
                    text = "2 capturas en cola",
                    icon = Icons.Default.Sync
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    notes.forEachIndexed { i, (title, meta, chips) ->
                        GlifoItem(
                            onClick = { onNavigateToNoteDetail(i.toLong() + 1) }
                        ) {
                            GlifoThumb {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, fontSize = 14.sp, color = colors.textPrimary)
                                Text(text = meta, fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                chips.forEach { chipState ->
                                    GlifoConfidenceChip(state = chipState)
                                }
                            }
                        }
                    }
                }
            }

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

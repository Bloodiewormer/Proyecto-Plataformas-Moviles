package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.NotesViewModel
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(
    courseId: Long,
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

    // Se recarga al volver del detalle o de una captura nueva: la lista sale de Room.
    LaunchedEffect(courseId) { viewModel.load(courseId) }

    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

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
                    userName = user?.fullName.orEmpty(),
                    userEmail = user?.email.orEmpty(),
                    userInitials = user?.initials.orEmpty(),
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
                        sessionViewModel.logout()
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
                    title = stringResource(R.string.notes_title),
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.common_search), tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                        }
                    }
                )

                AnimatedVisibility(visible = searchOpen) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        GlifoField(label = stringResource(R.string.notes_search_placeholder), value = query, onValueChange = { query = it }, isFocused = true)
                    }
                }

                when (val state = uiState) {
                    is UiState.Loading -> GlifoLoadingBox()
                    is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                    is UiState.Empty -> GlifoBanner(text = stringResource(R.string.notes_empty_no_notes))
                    is UiState.Success -> {
                        val pendingCount = state.data.pendingSyncCount
                        val notes = state.data.notes.filter {
                            query.isBlank() || it.note.title.contains(query, ignoreCase = true)
                        }

                        if (pendingCount > 0) {
                            GlifoBanner(
                                text = stringResource(R.string.notes_pending_captures, pendingCount),
                                icon = Icons.Default.Sync
                            )
                        }

                        if (notes.isEmpty()) {
                            // Sin búsqueda, la lista vacía es que el curso todavía no tiene apuntes.
                            GlifoBanner(
                                text = stringResource(
                                    if (query.isBlank()) R.string.notes_empty_course else R.string.notes_empty_no_match
                                )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState())
                        ) {
                            notes.forEachIndexed { i, meta ->
                                GlifoStaggeredEntry(index = i) {
                                    GlifoItem(
                                        onClick = { onNavigateToNoteDetail(meta.note.id) }
                                    ) {
                                        GlifoThumb {
                                            Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = meta.note.title, fontSize = 14.sp, color = colors.textPrimary)
                                            Text(text = pluralStringResource(R.plurals.notes_date_page_count_plural, meta.pageCount, meta.displayDate, meta.pageCount), fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            meta.chips.forEach { chipState ->
                                                GlifoConfidenceChip(state = chipState)
                                            }
                                        }
                                    }
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

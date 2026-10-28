package cr.ac.una.glifo.presentation.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.model.SyncStatus
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.SyncQueueViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

@Composable
fun SyncQueueScreen(
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SyncQueueViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            GlifoTopAppBar(
                title = stringResource(R.string.sync_queue_title),
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.sync_notification_permission_banner),
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    GlifoButton(
                        text = stringResource(R.string.sync_notification_permission_action),
                        variant = GlifoButtonVariant.Secondary,
                        onClick = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
            }

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoBanner(text = stringResource(R.string.sync_queue_empty))
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val items = state.data.items
                    val syncingCount = state.data.syncingCount
                    val failedCount = items.count { it.status == SyncStatus.Failed }
                    val running by viewModel.running.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlifoButton(
                            text = stringResource(R.string.sync_queue_sync_now),
                            icon = Icons.Default.Sync,
                            enabled = !running && items.any { it.status != SyncStatus.Done },
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::syncNow
                        )
                        if (failedCount > 0) {
                            GlifoButton(
                                text = stringResource(R.string.sync_queue_retry_failed),
                                variant = GlifoButtonVariant.Secondary,
                                enabled = !running,
                                modifier = Modifier.weight(1f),
                                onClick = viewModel::retryFailed
                            )
                        }
                    }
                    state.data.lastRun?.let { run ->
                        Text(
                            stringResource(R.string.sync_queue_result, run.uploaded, run.remaining),
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }

                    if (items.isEmpty()) {
                        GlifoBanner(text = stringResource(R.string.sync_queue_empty))
                    }

                    if (syncingCount > 0) {
                        val noteWord = if (syncingCount == 1) {
                            stringResource(R.string.sync_queue_note_singular)
                        } else {
                            stringResource(R.string.sync_queue_note_plural)
                        }
                        GlifoBanner(
                            text = stringResource(R.string.sync_queue_syncing_note, syncingCount, noteWord),
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
                                        Text(
                                            text = if (item.attempts > 0 && item.status != SyncStatus.Done) {
                                                stringResource(R.string.sync_queue_attempts, item.attempts, item.timestamp)
                                            } else {
                                                item.timestamp
                                            },
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                        if (item.status != SyncStatus.Done) {
                                            item.lastError?.let { error ->
                                                Text(text = error, fontSize = 11.sp, color = if (item.status == SyncStatus.Failed) colors.alert else colors.textSecondary)
                                            }
                                        }
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
                                        SyncStatus.Queued -> GlifoBadge(text = stringResource(R.string.sync_queue_queued_badge))
                                        SyncStatus.Failed -> GlifoBadge(
                                            text = stringResource(R.string.sync_queue_failed_badge),
                                            backgroundColor = colors.alertSoft,
                                            textColor = colors.alert
                                        )
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
    }
}

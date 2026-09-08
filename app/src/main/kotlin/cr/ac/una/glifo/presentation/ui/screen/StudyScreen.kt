package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.SessionViewModel
import cr.ac.una.glifo.presentation.viewmodel.StudyHomeViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage
import kotlinx.coroutines.launch

@Composable
fun StudyScreen(
    courseId: Long,
    onNavigateBackOrMenu: () -> Unit,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateDrawer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: StudyHomeViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = GlifoTheme.colors
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(courseId) { viewModel.load(courseId) }

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
                    currentScreen = "study",
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
                title = (uiState as? UiState.Success)?.data?.course?.name ?: "Curso",
                subtitle = "Estudio de",
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(), onRetry = { viewModel.load(courseId) })
                is UiState.Success -> {
                    val flashcardCount = state.data.flashcardCount
                    val quizQuestion = state.data.quizQuestion

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        // Flashcards Entry Card
                        GlifoStaggeredEntry(index = 0) {
                            GlifoCard(
                                onClick = onNavigateToFlashcards,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(22.dp))
                                    Text("$flashcardCount flashcards", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "Generadas de los temas parciales. 4 son de la corrección de ayer.",
                                    fontSize = 14.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        // Quiz Entry Card
                        GlifoStaggeredEntry(index = 1) {
                            GlifoCard(
                                onClick = onNavigateToQuiz,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DonutLarge, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(22.dp))
                                    Text("Quiz de 5 preguntas", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "Sobre los cinco vacíos con menos evidencia, empezando por ${quizQuestion.topicLabel}",
                                    fontSize = 14.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        Text(
                            text = "El material de estudio sale de la cobertura, no de un temario genérico: solo pregunta lo que tus apuntes no sostienen.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

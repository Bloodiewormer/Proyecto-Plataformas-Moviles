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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.GlifoRole
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
    val context = LocalContext.current
    val activeCourse = LocalActiveCourse.current
    val user by sessionViewModel.currentUser.collectAsState()
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(courseId) { viewModel.load(courseId) }

    val courseTitle = activeCourse.name?.takeIf { it.isNotBlank() }
        ?: (uiState as? UiState.Success)?.data?.course?.name?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.capture_default_course_name)

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
                title = courseTitle,
                subtitle = stringResource(R.string.study_subtitle),
                onNavigationClick = { scope.launch { drawerState.open() } }
            )

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = { viewModel.load(courseId, force = true) })
                is UiState.Success -> {
                    val data = state.data
                    val flashcardCount = data.flashcardCount
                    val quizCount = data.quizQuestionCount

                    val flashcardTitle = pluralStringResource(
                        R.plurals.study_flashcards_plural,
                        flashcardCount,
                        flashcardCount
                    )
                    val flashcardSubtitle = if (flashcardCount > 0) {
                        val topicCount = data.flashcardTopicCount.coerceAtLeast(1)
                        val topicsText = pluralStringResource(
                            R.plurals.study_topics_plural,
                            topicCount,
                            topicCount
                        )
                        stringResource(
                            R.string.study_flashcard_topics_summary,
                            flashcardTitle,
                            topicsText
                        )
                    } else {
                        stringResource(R.string.study_no_material_yet)
                    }

                    val quizTitle = if (quizCount > 0) {
                        stringResource(R.string.study_quiz_count_title, quizCount)
                    } else {
                        stringResource(R.string.study_quiz_title)
                    }
                    val quizSubtitle = if (quizCount > 0) {
                        val questionsText = pluralStringResource(
                            R.plurals.study_quiz_questions_plural,
                            quizCount,
                            quizCount
                        )
                        val topicsSummary = data.quizTopicsSummary
                        if (topicsSummary.isNotBlank()) {
                            stringResource(
                                R.string.study_quiz_topics_summary,
                                questionsText,
                                topicsSummary
                            )
                        } else {
                            questionsText
                        }
                    } else {
                        stringResource(R.string.study_no_material_yet)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        GlifoButton(
                            text = if (data.isGenerating) {
                                stringResource(R.string.study_generating_ai)
                            } else {
                                stringResource(R.string.study_generate_button)
                            },
                            enabled = !data.isGenerating,
                            onClick = { viewModel.generateStudyMaterial(courseId) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (data.isGenerating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.accent
                                )
                                Text(
                                    text = stringResource(R.string.study_generating_ai),
                                    fontSize = 14.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (data.generationError != null) {
                            GlifoBanner(
                                text = data.generationError.toMessage(context),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

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
                                    Text(flashcardTitle, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = flashcardSubtitle,
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
                                    Text(quizTitle, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = quizSubtitle,
                                    fontSize = 14.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.study_footer_note),
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

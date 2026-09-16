package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.JoinCourseViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun JoinCourseScreen(
    onNavigateBack: () -> Unit,
    onJoinSuccess: () -> Unit,
    viewModel: JoinCourseViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    var courseCode by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val joinState by viewModel.joinState.collectAsState()

    LaunchedEffect(joinState) {
        if (joinState is UiState.Success) {
            onJoinSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.join_course_title),
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.join_course_instructions),
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GlifoField(
                label = stringResource(R.string.join_course_field_label),
                value = courseCode,
                onValueChange = { courseCode = it; viewModel.reset() },
                isFocused = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlifoButton(
                text = stringResource(R.string.join_course_search_button),
                enabled = courseCode.isNotBlank(),
                onClick = { viewModel.search(courseCode) }
            )

            AnimatedVisibility(
                visible = searchState != null,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 6 }
            ) {
                when (val state = searchState) {
                    null -> {}
                    is UiState.Loading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = colors.accent) }
                    is UiState.Success -> {
                        val foundCourse = state.data.course
                        val stats = state.data.stats
                        Column {
                            Text(
                                text = stringResource(R.string.join_course_found_section),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                            )

                            GlifoCard(borderColor = colors.accentLine) {
                                Text(
                                    text = stringResource(R.string.join_course_code_name, foundCourse.code, foundCourse.name),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.join_course_term_instructor, stats.instructorName),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                        Text(text = stringResource(R.string.join_course_student_count, stats.studentCount), fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                        Text(text = stringResource(R.string.join_course_topic_count, stats.topicCount), fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                GlifoButton(
                                    text = stringResource(R.string.join_course_join_button),
                                    small = true,
                                    enabled = joinState !is UiState.Loading,
                                    onClick = { viewModel.joinCourse(foundCourse.joinCode) }
                                )

                                if (joinState is UiState.Loading) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(20.dp))
                                    }
                                }

                                if (joinState is UiState.Error) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val errorMsg = (joinState as UiState.Error).error.toMessage(context)
                                    GlifoBanner(
                                        text = errorMsg,
                                        icon = Icons.Default.Warning,
                                        isWarning = true
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error, is UiState.Empty -> {
                        GlifoBanner(
                            text = stringResource(R.string.join_course_not_found),
                            icon = Icons.Default.Warning,
                            isWarning = true,
                            modifier = Modifier.padding(top = 18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                )
                Text(
                    text = stringResource(R.string.join_course_privacy_note),
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

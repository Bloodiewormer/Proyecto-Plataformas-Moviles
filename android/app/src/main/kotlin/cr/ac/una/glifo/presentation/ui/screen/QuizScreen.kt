package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.presentation.viewmodel.QuizViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    val title = stringResource(R.string.study_quiz_title)
    when (val state = uiState) {
        is UiState.Loading -> GlifoStatusScreen(title, onNavigateBack) { GlifoLoadingBox() }
        is UiState.Empty -> GlifoStatusScreen(title, onNavigateBack) {
            GlifoEmptyBox(
                message = stringResource(R.string.study_empty_quiz),
                actionText = stringResource(R.string.common_back),
                onAction = onNavigateBack
            )
        }
        is UiState.Error -> GlifoStatusScreen(title, onNavigateBack) {
            GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
        }
        is UiState.Success -> {
            val questions = state.data
            val safeIndex = currentIndex.coerceIn(0, questions.lastIndex)
            QuizContent(
                question = questions[safeIndex],
                questionNumber = safeIndex + 1,
                totalQuestions = questions.size,
                isLastQuestion = safeIndex >= questions.lastIndex,
                colors = colors,
                onNavigateBack = onNavigateBack,
                onSubmitAnswer = { selectedIdx ->
                    viewModel.submitAnswer(selectedIdx, safeIndex)
                },
                onNextQuestion = { viewModel.nextQuestion() },
                onNavigateToResult = {
                    viewModel.finishQuiz()
                    onNavigateToResult()
                }
            )
        }
    }
}

@Composable
private fun QuizContent(
    question: QuizQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    isLastQuestion: Boolean,
    colors: cr.ac.una.glifo.core.ui.theme.GlifoColorScheme,
    onNavigateBack: () -> Unit,
    onSubmitAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    var selectedOption by remember(questionNumber) { mutableStateOf<Int?>(null) }
    var isAnswered by remember(questionNumber) { mutableStateOf(false) }

    fun borderFor(i: Int): androidx.compose.ui.graphics.Color {
        if (!isAnswered) return if (selectedOption == i) colors.accentText else colors.border
        if (i == question.correctIndex) return colors.verified
        if (i == selectedOption && selectedOption != question.correctIndex) return colors.alert
        return colors.border
    }

    fun bgFor(i: Int): androidx.compose.ui.graphics.Color {
        if (!isAnswered) return if (selectedOption == i) colors.accentFaint else colors.surface
        if (i == question.correctIndex) return colors.verifiedSoft
        if (i == selectedOption && selectedOption != question.correctIndex) return colors.alertFaint
        return colors.surface
    }

    val progressFraction = (questionNumber.toFloat() / totalQuestions.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            isBack = true,
            onNavigationClick = onNavigateBack,
            customTitle = {
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surfaceHigh)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(colors.accent)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.quiz_progress_format, questionNumber, totalQuestions),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                GlifoBadge(
                    text = question.topicTag,
                    backgroundColor = colors.escalatedSoft,
                    textColor = colors.escalated
                )
                Text(question.topicLabel, fontSize = 12.sp, color = colors.textSecondary)
            }

            if (LatexHtmlBuilder.containsLatex(question.prompt)) {
                LatexFormula(
                    latex = question.prompt,
                    mode = LatexRenderMode.Mixed,
                    fontSize = 19.sp,
                    lineHeight = 28.sp,
                    textColor = colors.textPrimary,
                    textAlign = TextAlign.Start,
                    fallbackFontWeight = FontWeight.Normal,
                    allowHorizontalScroll = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 22.dp)
                )
            } else {
                Text(
                    text = question.prompt,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 28.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 22.dp)
                )
            }

            question.options.forEachIndexed { i, label ->
                val optionLetter = listOf("A", "B", "C", "D").getOrElse(i) { "${i + 1}" }
                val animatedBorder by animateColorAsState(borderFor(i), tween(220), label = "quiz-border")
                val animatedBg by animateColorAsState(bgFor(i), tween(220), label = "quiz-bg")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .background(animatedBg, RoundedCornerShape(12.dp))
                        .border(1.5.dp, animatedBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isAnswered) { selectedOption = i }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(2.dp, animatedBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAnswered && i == question.correctIndex) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(14.dp))
                            } else if (isAnswered && i == selectedOption && selectedOption != question.correctIndex) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = colors.alert, modifier = Modifier.size(14.dp))
                            } else if (!isAnswered && selectedOption == i) {
                                Box(modifier = Modifier.size(10.dp).background(colors.accentText, CircleShape))
                            } else {
                                Text(
                                    text = optionLetter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        if (LatexHtmlBuilder.containsLatex(label)) {
                            LatexFormula(
                                latex = label,
                                mode = LatexRenderMode.Mixed,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                textColor = colors.textPrimary,
                                textAlign = TextAlign.Start,
                                fallbackFontWeight = FontWeight.Normal,
                                allowHorizontalScroll = false,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(text = label, fontSize = 16.sp, color = colors.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isAnswered) {
                GlifoButton(
                    text = stringResource(R.string.quiz_answer_button),
                    enabled = selectedOption != null,
                    onClick = {
                        val chosen = selectedOption ?: return@GlifoButton
                        isAnswered = true
                        onSubmitAnswer(chosen)
                    }
                )
            } else {
                GlifoButton(
                    text = if (isLastQuestion) {
                        stringResource(R.string.quiz_view_result_button)
                    } else {
                        stringResource(R.string.quiz_next_question_button)
                    },
                    onClick = {
                        if (isLastQuestion) {
                            onNavigateToResult()
                        } else {
                            onNextQuestion()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                val correct = selectedOption == question.correctIndex
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (correct) colors.verifiedSoft else colors.alertFaint, RoundedCornerShape(10.dp))
                            .border(1.dp, if (correct) colors.verifiedLine else colors.alertLine, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                if (correct) stringResource(R.string.quiz_correct_feedback) else stringResource(R.string.quiz_incorrect_feedback),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (correct) colors.verified else colors.alert
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (LatexHtmlBuilder.containsLatex(question.explanation)) {
                                LatexFormula(
                                    latex = question.explanation,
                                    mode = LatexRenderMode.Mixed,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    textColor = colors.textSecondary,
                                    textAlign = TextAlign.Start,
                                    fallbackFontWeight = FontWeight.Normal,
                                    allowHorizontalScroll = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = question.explanation,
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                Text(
                    text = stringResource(R.string.quiz_local_grading_note),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

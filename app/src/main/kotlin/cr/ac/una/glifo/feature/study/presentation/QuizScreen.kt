package cr.ac.una.glifo.feature.study.presentation

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyStudyProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val colors = GlifoTheme.colors
    val question = DummyStudyProvider.getQuizQuestion(courseId = 1L)

    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isAnswered by remember { mutableStateOf(false) }

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
                                .fillMaxWidth(0.40f)
                                .background(colors.accent)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "2 / 5",
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

            Text(
                text = question.prompt,
                fontSize = 19.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 22.dp)
            )

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
                        Text(text = label, fontSize = 16.sp, color = colors.textPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isAnswered) {
                GlifoButton(
                    text = "Responder",
                    enabled = selectedOption != null,
                    onClick = { isAnswered = true }
                )
            } else {
                GlifoButton(
                    text = "Ver el resultado",
                    onClick = onNavigateToResult
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
                                if (correct) "¡Correcto!" else "No era esa",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (correct) colors.verified else colors.alert
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                Text(
                    text = "La corrección es local. Las 5 preguntas se generaron en una sola llamada.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

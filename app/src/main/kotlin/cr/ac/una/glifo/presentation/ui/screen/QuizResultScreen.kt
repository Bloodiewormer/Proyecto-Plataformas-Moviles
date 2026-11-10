package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.QuizResultViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun QuizResultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCoverage: () -> Unit,
    // Antes este botón abría el glosario del docente y dejaba al estudiante en el menú de docente.
    onNavigateToStudy: () -> Unit,
    viewModel: QuizResultViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    when (val state = uiState) {
        is UiState.Loading -> GlifoLoadingBox()
        is UiState.Empty -> GlifoBanner(text = stringResource(R.string.study_empty_course))
        is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
        is UiState.Success -> QuizResultContent(state.data, colors, onNavigateBack, onNavigateToCoverage, onNavigateToStudy)
    }
}

@Composable
private fun QuizResultContent(
    result: cr.ac.una.glifo.domain.model.QuizResultInfo,
    colors: cr.ac.una.glifo.core.ui.theme.GlifoColorScheme,
    onNavigateBack: () -> Unit,
    onNavigateToCoverage: () -> Unit,
    onNavigateToStudy: () -> Unit
) {
    val targetFraction = if (result.total > 0) {
        result.score.toFloat() / result.total.toFloat()
    } else {
        0f
    }

    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }
    val sweep by animateFloatAsState(
        targetValue = if (animateIn) targetFraction else 0f,
        animationSpec = tween(900),
        label = "score-ring"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.quiz_result_title),
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
            // Score circle presentation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(128.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 12.dp.toPx()
                        val radius = (size.minDimension - strokeW) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Background ring
                        drawCircle(
                            color = colors.surfaceHigh,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeW)
                        )

                        // Progress ring, animated in on entry
                        drawArc(
                            color = colors.accentText,
                            startAngle = -90f,
                            sweepAngle = sweep * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.score}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.quiz_result_of_total, result.total),
                            fontSize = 15.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = result.headline,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = result.subtext,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = stringResource(R.string.quiz_result_review_section),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                result.review.forEachIndexed { i, row ->
                    GlifoItem(
                        compact = true,
                        showBottomBorder = i < result.review.size - 1,
                        backgroundColor = if (row.correct) Color.Transparent else colors.alertFaint
                    ) {
                        Icon(
                            imageVector = if (row.correct) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (row.correct) colors.verified else colors.alert,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = row.label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (result.rescheduledCount != null && result.daysUntilReturn != null) {
                Spacer(modifier = Modifier.height(10.dp))

                GlifoCard(
                    backgroundColor = colors.accentFaint,
                    borderColor = colors.accentLine
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        Column {
                            Text(stringResource(R.string.quiz_result_rescheduled_count, result.rescheduledCount), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text(stringResource(R.string.quiz_result_return_in_days, result.daysUntilReturn), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlifoButton(
                text = stringResource(R.string.quiz_result_view_coverage),
                icon = Icons.Default.DonutLarge,
                onClick = onNavigateToCoverage
            )

            Spacer(modifier = Modifier.height(6.dp))

            GlifoButton(
                // Lleva a Estudio para repasar con flashcards (IA-03, explicación a pedido, no está implementada).
                text = stringResource(R.string.quiz_result_review_flashcards),
                variant = GlifoButtonVariant.Secondary,
                icon = Icons.Default.MenuBook,
                onClick = onNavigateToStudy
            )
        }
    }
}

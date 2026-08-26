package cr.ac.una.glifo.feature.capture.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyCaptureProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import kotlinx.coroutines.delay

@Composable
fun CaptureProcessingScreen(
    onNavigateBack: () -> Unit,
    onViewResults: () -> Unit
) {
    val colors = GlifoTheme.colors
    val steps = DummyCaptureProvider.getProcessingSteps()
    val revealTarget = steps.count { it.state != "pending" }

    // Steps reveal themselves over time instead of appearing pre-finished, so the
    // "processing" screen actually reads as processing.
    var revealedCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (revealedCount < revealTarget) {
            delay(450)
            revealedCount++
        }
    }

    val progress by animateFloatAsState(
        targetValue = revealedCount.toFloat() / steps.size,
        animationSpec = tween(400),
        label = "capture-progress"
    )

    fun stateFor(index: Int): String = when {
        index < revealedCount - 1 -> "done"
        index == revealedCount - 1 -> steps[index].state
        else -> "pending"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Procesando",
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                GlifoBadge(
                    text = "0 LLAMADAS DE PAGO",
                    backgroundColor = colors.verifiedSoft,
                    textColor = colors.verified
                )
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // Processing Summary Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp, 74.dp)
                        .background(colors.surfaceHigh, RoundedCornerShape(6.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Página 1 de 3", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Text("18 regiones detectadas", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .padding(top = 10.dp)
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

            // Steps Timeline — the full path is always visible; each dot lights up in
            // turn as `revealedCount` advances, so upcoming steps read as "pending"
            // rather than popping in out of nowhere.
            steps.forEachIndexed { i, step ->
                val state = stateFor(i)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(22.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (state == "done") colors.verified else Color.Transparent)
                                .border(
                                    2.dp,
                                    if (state == "done") colors.verified else if (state == "now") colors.accent else colors.border,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state == "done") {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.background, modifier = Modifier.size(13.dp))
                            } else {
                                Text(
                                    text = "${i + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (state == "now") colors.accentText else colors.textSecondary
                                )
                            }
                        }
                        if (i < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(36.dp)
                                    .background(if (state == "done") colors.verified else colors.border)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = step.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (state == "now") colors.accentText else if (state == "pending") colors.textSecondary else colors.textPrimary
                        )
                        Text(
                            text = step.description,
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlifoButton(
                text = "Ver resultados",
                enabled = revealedCount >= revealTarget,
                onClick = onViewResults
            )
        }
    }
}

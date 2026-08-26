package cr.ac.una.glifo.feature.coverage.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyCoverageProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun CoverageDeltaScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit
) {
    val colors = GlifoTheme.colors
    val delta = DummyCoverageProvider.getDelta()

    // Animate the "current" bar growing in from the "previous" state on entry.
    var animateToCurrent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateToCurrent = true }
    val vAnim by animateFloatAsState(if (animateToCurrent) delta.toBar.v else delta.fromBar.v, tween(600), label = "delta-v")
    val rAnim by animateFloatAsState(if (animateToCurrent) delta.toBar.r else delta.fromBar.r, tween(600), label = "delta-r")
    val eAnim by animateFloatAsState(if (animateToCurrent) delta.toBar.e else delta.fromBar.e, tween(600), label = "delta-e")
    val uAnim by animateFloatAsState(if (animateToCurrent) delta.toBar.u else delta.fromBar.u, tween(600), label = "delta-u")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Qué cambió",
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
            GlifoCard {
                Text(delta.dateRange, fontSize = 12.sp, color = colors.textSecondary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("${delta.fromPercent}%", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    Text("${delta.toPercent}%", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                    Spacer(modifier = Modifier.weight(1f))
                    GlifoBadge(text = "+${delta.deltaPoints} PUNTOS", backgroundColor = colors.verifiedSoft, textColor = colors.verified)
                }

                // Bar animates from the previous state to the current one on entry.
                CovBar(v = vAnim, r = rAnim, e = eAnim, u = uAnim, height = 8.dp)

                Text(
                    "${delta.newNotesCount} apuntes nuevos · ${delta.newPagesCount} páginas procesadas",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = "TEMAS QUE MEJORARON",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                delta.improved.forEachIndexed { i, item ->
                    GlifoItem(compact = true, showBottomBorder = i < delta.improved.size - 1) {
                        Text(text = item.label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            GlifoConfidenceChip(state = item.from)
                            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            GlifoConfidenceChip(state = item.to)
                        }
                    }
                }
            }

            Text(
                text = "SIGUE SIN EVIDENCIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                delta.stillMissing.forEachIndexed { i, item ->
                    GlifoItem(compact = true, showBottomBorder = i < delta.stillMissing.size - 1) {
                        GlifoConfidenceChip(state = item.state)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.label, fontSize = 15.sp, color = colors.textPrimary)
                            Text(item.meta, fontSize = 12.sp, color = colors.textSecondary)
                        }
                        if (item.needsReview) {
                            GlifoBadge(text = "REVISAR", backgroundColor = colors.uncertainSoft, textColor = colors.uncertain)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlifoButton(
                text = "Ver el apunte ilegible",
                variant = GlifoButtonVariant.Secondary,
                onClick = { onNavigateToNoteDetail(3L) }
            )
        }
    }
}

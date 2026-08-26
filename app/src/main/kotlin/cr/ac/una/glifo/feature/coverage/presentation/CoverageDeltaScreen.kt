package cr.ac.una.glifo.feature.coverage.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun CoverageDeltaScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit
) {
    val colors = GlifoTheme.colors

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
                Text("Del 8 al 15 de agosto", fontSize = 12.sp, color = colors.textSecondary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("62%", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    Text("68%", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                    Spacer(modifier = Modifier.weight(1f))
                    GlifoBadge(text = "+6 PUNTOS", backgroundColor = colors.verifiedSoft, textColor = colors.verified)
                }

                // Previous CovBar (faint)
                CovBar(v = 48f, r = 18f, e = 20f, u = 14f, height = 6.dp, modifier = Modifier.padding(bottom = 4.dp))
                // Current CovBar
                CovBar(v = 54f, r = 20f, e = 14f, u = 12f, height = 6.dp)

                Text("4 apuntes nuevos · 11 páginas procesadas", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
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
                listOf(
                    Triple("3.2 Series geométricas", ConfidenceState.Escalated, ConfidenceState.Verified),
                    Triple("3.3 Comparación", ConfidenceState.Escalated, ConfidenceState.Repaired),
                    Triple("2.4 Integrales impropias", ConfidenceState.Repaired, ConfidenceState.Verified)
                ).forEachIndexed { i, (label, fromState, toState) ->
                    GlifoItem(compact = true, showBottomBorder = i < 2) {
                        Text(text = label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            GlifoConfidenceChip(state = fromState)
                            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            GlifoConfidenceChip(state = toState)
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
                GlifoItem(compact = true) {
                    GlifoConfidenceChip(state = ConfidenceState.Escalated)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("3.4 Series de potencias", fontSize = 15.sp, color = colors.textPrimary)
                        Text("Sin cambios desde el 29 de julio", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    GlifoConfidenceChip(state = ConfidenceState.Uncertain)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("3.5 Radio de convergencia", fontSize = 15.sp, color = colors.textPrimary)
                        Text("Hay apunte, pero ilegible", fontSize = 12.sp, color = colors.textSecondary)
                    }
                    GlifoBadge(text = "REVISAR", backgroundColor = colors.uncertainSoft, textColor = colors.uncertain)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlifoButton(
                text = "Ver el apunte ilegible",
                variant = GlifoButtonVariant.Secondary,
                onClick = { onNavigateToNoteDetail(1L) }
            )
        }
    }
}

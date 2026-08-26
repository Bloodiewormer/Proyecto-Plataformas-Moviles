package cr.ac.una.glifo.feature.note.presentation

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyNoteContentProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun RegionProvenanceScreen(
    noteId: Long,
    onNavigateBack: () -> Unit
) {
    val colors = GlifoTheme.colors
    val provenance = DummyNoteContentProvider.get(noteId).provenance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Región",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Recorte de la región", fontSize = 12.sp, color = colors.textSecondary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlifoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlifoBadge(
                        text = "INCIERTO",
                        backgroundColor = colors.uncertainSoft,
                        textColor = colors.uncertain
                    )
                    Text("${provenance.confidence}", fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = colors.uncertain)
                }
                Text("Umbral configurado: ${provenance.threshold}", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
            }

            Text(
                text = "CÓMO SE CALCULÓ",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            GlifoCard {
                provenance.factors.forEach { factor ->
                    val barColor = if (factor.isStrong) colors.verified else colors.uncertain
                    val animatedFraction by animateFloatAsState(
                        targetValue = factor.value,
                        animationSpec = tween(500),
                        label = "provenance-factor"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = factor.label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.surfaceHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedFraction)
                                    .background(barColor)
                            )
                        }
                        Text(
                            text = String.format("%.2f", factor.value),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.width(36.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Right
                        )
                    }
                }
            }

            Text(
                text = "RECORRIDO EN LA ESCALERA",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                provenance.pipeline.forEachIndexed { i, step ->
                    GlifoItem(compact = true, showBottomBorder = i < provenance.pipeline.size - 1) {
                        val icon = if (step.state == "skipped") Icons.Default.Close else Icons.Default.Check
                        val tint = when (step.state) {
                            "escalated" -> colors.escalated
                            "skipped" -> colors.textSecondary
                            else -> colors.verified
                        }
                        val textColor = if (step.state == "skipped") colors.textSecondary else colors.textPrimary
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                        Text(step.label, fontSize = 15.sp, color = textColor, modifier = Modifier.weight(1f))
                        Text(step.duration, fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlifoCard(
                backgroundColor = colors.escalatedFaint,
                borderColor = colors.escalatedLine,
                onClick = onNavigateBack
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = colors.escalated, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Enviar la página completa (N3)", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("Una llamada adicional al modelo de pago. Último recurso.", fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

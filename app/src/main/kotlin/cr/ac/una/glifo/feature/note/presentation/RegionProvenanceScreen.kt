package cr.ac.una.glifo.feature.note.presentation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun RegionProvenanceScreen(
    onNavigateBack: () -> Unit
) {
    val colors = GlifoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Región 12",
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
                    Text("0.41", fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = colors.uncertain)
                }
                Text("Umbral configurado: 0.70", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
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
                listOf(
                    Triple("Confianza del OCR", "0.38", Pair(38f, colors.uncertain)),
                    Triple("Coincide con el glosario", "0.00", Pair(0f, colors.accent)),
                    Triple("Palabra del diccionario", "0.00", Pair(0f, colors.accent)),
                    Triple("Coherencia con región vecina", "0.72", Pair(72f, colors.verified))
                ).forEach { (label, value, bar) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = label, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
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
                                    .fillMaxWidth(bar.first / 100f)
                                    .background(bar.second)
                            )
                        }
                        Text(
                            text = value,
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
                GlifoItem(compact = true) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                    Text("N0 · preprocesada", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("0.02 s", fontSize = 12.sp, color = colors.textSecondary)
                }
                GlifoItem(compact = true) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                    Text("N1 · ML Kit", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("0.06 s", fontSize = 12.sp, color = colors.textSecondary)
                }
                GlifoItem(compact = true) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("N1.5 · no es fórmula", fontSize = 15.sp, color = colors.textSecondary, modifier = Modifier.weight(1f))
                    Text("omitido", fontSize = 12.sp, color = colors.textSecondary)
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.escalated, modifier = Modifier.size(18.dp))
                    Text("N2 · visión, en lote", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("1.4 s", fontSize = 12.sp, color = colors.textSecondary)
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

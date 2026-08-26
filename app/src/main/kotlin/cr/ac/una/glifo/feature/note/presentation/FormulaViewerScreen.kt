package cr.ac.una.glifo.feature.note.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun FormulaViewerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCorrect: () -> Unit
) {
    val colors = GlifoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Fórmula · región 7",
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                GlifoBadge(
                    text = "N1.5",
                    backgroundColor = colors.repairedSoft,
                    textColor = colors.repaired
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
            Text(
                text = "RECORTE ORIGINAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Fotografía de la región", fontSize = 12.sp, color = colors.textSecondary)
            }

            Text(
                text = "TRANSCRIPCIÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(vertical = 22.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "∫_a^∞ f(x) dx = lim_{b→∞} ∫_a^b f(x) dx",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LaTeX Card
            GlifoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LaTeX generado", fontSize = 12.sp, color = colors.textSecondary)
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = colors.accentText, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\\int_a^{\\infty} f(x)\\,dx = \\lim_{b \\to \\infty} \\int_a^b f(x)\\,dx",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlifoCard(tight = true) {
                GlifoItem(compact = true) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.verified, modifier = Modifier.size(18.dp))
                    Text("Compila con JLaTeXMath", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                }
                GlifoItem(compact = true) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("Confianza del motor", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("0.86", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = colors.textSecondary)
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("Resuelto por", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("SimpleTex · gratuito", fontSize = 14.sp, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlifoButton(
                    text = "Corregir",
                    variant = GlifoButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCorrect
                )
                GlifoButton(
                    text = "Está bien",
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateBack
                )
            }
        }
    }
}

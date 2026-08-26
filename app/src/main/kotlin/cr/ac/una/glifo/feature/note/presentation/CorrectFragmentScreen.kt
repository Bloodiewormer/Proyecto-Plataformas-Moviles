package cr.ac.una.glifo.feature.note.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun CorrectFragmentScreen(
    onNavigateBack: () -> Unit,
    onSaveCorrection: () -> Unit
) {
    val colors = GlifoTheme.colors
    var contributeToGlossary by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Corregir fragmento",
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                TextButton(onClick = onSaveCorrection) {
                    Text("Guardar", color = colors.accentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlifoBadge(
                    text = "INCIERTO",
                    backgroundColor = colors.uncertainSoft,
                    textColor = colors.uncertain
                )
                Text("confianza 0.41 · umbral 0.70", fontSize = 12.sp, color = colors.textSecondary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Original crop placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Recorte original de la región", fontSize = 12.sp, color = colors.textSecondary)
            }

            Text(
                text = "Región 12 de 18",
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                text = "LO QUE LEYÓ EL MOTOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Recognition box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 80.dp)
                    .background(colors.surfaceHigh, RoundedCornerShape(8.dp))
                    .border(2.dp, colors.accentLine, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text("TEXTO RECONOCIDO", fontSize = 11.sp, color = colors.accentText, modifier = Modifier.padding(bottom = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("converge; en caso contrario ", fontSize = 14.sp, color = colors.textPrimary)
                        Text("dvierge", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.background(colors.uncertainSoft, RoundedCornerShape(3.dp)).padding(horizontal = 2.dp))
                    }
                }
            }

            Text(
                text = "SUGERENCIAS DEL GLOSARIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("diverge", "divergente", "deriva").forEachIndexed { i, s ->
                    Box(
                        modifier = Modifier
                            .background(if (i == 0) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                            .clickable {}
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(text = s, fontSize = 14.sp, color = if (i == 0) colors.accentText else colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Contribute to glossary Card
            GlifoCard(
                backgroundColor = colors.accentFaint,
                borderColor = colors.accentLine
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colors.accent, RoundedCornerShape(4.dp))
                            .clickable { contributeToGlossary = !contributeToGlossary },
                        contentAlignment = Alignment.Center
                    ) {
                        if (contributeToGlossary) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
                        }
                    }
                    Column {
                        Text("Aportar al glosario del curso", fontSize = 15.sp, color = colors.textPrimary)
                        Text("Tu corrección sube la confianza de esta palabra para todo el grupo.", fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

package cr.ac.una.glifo.feature.capture.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
fun CaptureReviewScreen(
    onNavigateBack: () -> Unit,
    onRetakePhoto: () -> Unit,
    onProcessAnyway: () -> Unit
) {
    val colors = GlifoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Revisá la foto",
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
            // Blurred preview box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Vista previa de la captura",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Warning Card
            GlifoCard(
                backgroundColor = colors.alertFaint,
                borderColor = colors.alertLine
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = colors.alert, modifier = Modifier.size(18.dp))
                    Text("La foto está movida", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                }
                Text(
                    text = "Con este nivel de desenfoque el reconocimiento inventaría texto. Apoyá el codo en la mesa y repetí la toma.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Quality Diagnostics
            Text(
                text = "DIAGNÓSTICO DE CALIDAD",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
            )

            GlifoCard {
                listOf(
                    Triple("Nitidez", "12 / 35", Pair(24f, colors.alert)),
                    Triple("Iluminación", "78", Pair(78f, colors.verified)),
                    Triple("Sin reflejos", "91", Pair(91f, colors.verified)),
                    Triple("Perspectiva", "corregida −4.2°", Pair(100f, colors.verified))
                ).forEachIndexed { i, (label, score, bar) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (bar.second == colors.alert) Icons.Default.Warning else Icons.Default.Check,
                            contentDescription = null,
                            tint = bar.second,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = label, fontSize = 13.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(5.dp)
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
                            text = score,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlifoButton(
                text = "Repetir la foto",
                icon = Icons.Default.CameraAlt,
                onClick = onRetakePhoto
            )

            Spacer(modifier = Modifier.height(6.dp))

            GlifoButton(
                text = "Procesarla igual, sé que va a fallar",
                variant = GlifoButtonVariant.Text,
                onClick = onProcessAnyway
            )
        }
    }
}

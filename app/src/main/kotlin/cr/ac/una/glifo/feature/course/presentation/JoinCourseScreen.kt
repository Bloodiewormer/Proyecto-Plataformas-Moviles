package cr.ac.una.glifo.feature.course.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun JoinCourseScreen(
    onNavigateBack: () -> Unit,
    onJoinSuccess: () -> Unit
) {
    val colors = GlifoTheme.colors
    var courseCode by remember { mutableStateOf("GLF-4K2P") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Unirme a un curso",
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
            Text(
                text = "Pedile el código a tu profesor. Son ocho caracteres y no distingue mayúsculas.",
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GlifoField(
                label = "Código del curso",
                value = courseCode,
                onValueChange = { courseCode = it },
                isFocused = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlifoButton(
                text = "Buscar curso",
                onClick = {}
            )

            Text(
                text = "CURSO ENCONTRADO",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(
                borderColor = colors.accentLine
            ) {
                Text(
                    text = "EIF411 — Plataformas Móviles",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = "II Ciclo 2026 · Prof. Maikol Guzmán",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                        Text(text = "27 inscritos", fontSize = 12.sp, color = colors.textSecondary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                        Text(text = "33 temas", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                GlifoButton(
                    text = "Unirme a este curso",
                    small = true,
                    onClick = onJoinSuccess
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                )
                Text(
                    text = "Tus apuntes son privados. El profesor ve la cobertura del grupo, nunca tus fotos.",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

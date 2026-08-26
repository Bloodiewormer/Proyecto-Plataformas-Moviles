package cr.ac.una.glifo.feature.course.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import cr.ac.una.glifo.core.data.dummy.DummyCourseProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun JoinCourseScreen(
    onNavigateBack: () -> Unit,
    onJoinSuccess: () -> Unit
) {
    val colors = GlifoTheme.colors
    var courseCode by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf(false) }
    val foundCourse = remember(courseCode, searched) {
        if (searched) DummyCourseProvider.getCourseByJoinCode(courseCode) else null
    }
    val stats = foundCourse?.let { DummyCourseProvider.getStats(it.id) }

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
                onValueChange = { courseCode = it; searched = false },
                isFocused = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlifoButton(
                text = "Buscar curso",
                enabled = courseCode.isNotBlank(),
                onClick = { searched = true }
            )

            AnimatedVisibility(
                visible = searched,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 6 }
            ) {
                Column {
                    if (foundCourse != null && stats != null) {
                        Text(
                            text = "CURSO ENCONTRADO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )

                        GlifoCard(borderColor = colors.accentLine) {
                            Text(
                                text = "${foundCourse.code} — ${foundCourse.name}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "II Ciclo 2026 · ${stats.instructorName}",
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
                                    Text(text = "${stats.studentCount} inscritos", fontSize = 12.sp, color = colors.textSecondary)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                    Text(text = "${stats.topicCount} temas", fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            GlifoButton(
                                text = "Unirme a este curso",
                                small = true,
                                onClick = onJoinSuccess
                            )
                        }
                    } else {
                        GlifoBanner(
                            text = "No encontramos ningún curso con ese código. Revisalo con tu profesor.",
                            icon = Icons.Default.Warning,
                            isWarning = true,
                            modifier = Modifier.padding(top = 18.dp)
                        )
                    }
                }
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

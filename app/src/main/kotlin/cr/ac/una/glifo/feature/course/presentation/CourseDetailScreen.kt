package cr.ac.una.glifo.feature.course.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun CourseDetailScreen(
    courseId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToCoverage: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToDelta: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToNoteDetail: (Long) -> Unit
) {
    val colors = GlifoTheme.colors
    val course = DummyCourseProvider.findCourse(courseId)
    val stats = DummyCourseProvider.getStats(courseId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = course?.name ?: "Curso",
            subtitle = "Centro de",
            isBack = true,
            onNavigationClick = onNavigateBack,
            actions = {
                IconButton(onClick = onNavigateToCapture) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Captura", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
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
            GlifoStaggeredEntry(index = 0) {
                // Main Course Summary Card
                GlifoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(text = course?.name ?: "Curso", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text(text = course?.code ?: "—", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text(text = "${stats.coveragePercent}%", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                    }
                    CovBar(v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain, modifier = Modifier.padding(vertical = 10.dp))
                    Text(text = "${stats.instructorName} · ${stats.studentCount} estudiantes", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 Grid of Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlifoStaggeredEntry(index = 1, modifier = Modifier.weight(1f)) {
                    GlifoCard(
                        onClick = onNavigateToNotes,
                        modifier = Modifier.height(100.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Mis apuntes", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(text = "${stats.noteCount} apuntes", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                GlifoStaggeredEntry(index = 2, modifier = Modifier.weight(1f)) {
                    GlifoCard(
                        onClick = onNavigateToCoverage,
                        modifier = Modifier.height(100.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DonutLarge, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Cobertura", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(text = "${stats.topicCount} temas", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlifoStaggeredEntry(index = 3, modifier = Modifier.weight(1f)) {
                    GlifoCard(
                        onClick = onNavigateToStudy,
                        modifier = Modifier.height(100.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Estudio", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(text = "${stats.pendingCount} pendientes", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                GlifoStaggeredEntry(index = 4, modifier = Modifier.weight(1f)) {
                    GlifoCard(
                        onClick = onNavigateToDelta,
                        modifier = Modifier.height(100.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Qué cambió", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(text = "+${stats.weeklyDeltaPoints} pts esta semana", fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            // Acceso Rápido
            Text(
                text = "ACCESO RÁPIDO",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                GlifoItem(compact = true, onClick = { onNavigateToNoteDetail(1L) }) {
                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("Mapa de confianza", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                GlifoItem(compact = true, onClick = onNavigateToCapture) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("Nueva captura", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                GlifoItem(compact = true, showBottomBorder = false, onClick = onNavigateToSync) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("Cola de sincronización", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

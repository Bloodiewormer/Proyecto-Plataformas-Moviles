package cr.ac.una.glifo.feature.course.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.GlifoCard
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun CourseDetailScreen(
    courseId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToCapture: () -> Unit
) {
    Scaffold(
        topBar = {
            GlifoTopBar(
                title = "Cálculo II",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = GlifoTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "MAT-202 📐",
                color = GlifoTheme.colors.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            GlifoCard(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cobertura del curso", color = GlifoTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("68%", color = GlifoTheme.colors.accentText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "ACCIONES RÁPIDAS",
                color = GlifoTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlifoCard(
                    onClick = onNavigateToNotes,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📝 Apuntes", color = GlifoTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
                }
                GlifoCard(
                    onClick = { /* TODO: Cobertura */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📊 Cobertura", color = GlifoTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlifoCard(
                    onClick = { /* TODO: Estudio */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🧠 Estudio", color = GlifoTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
                }
                GlifoCard(
                    onClick = onNavigateToCapture,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📸 Captura", color = GlifoTheme.colors.accentText, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

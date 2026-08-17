package cr.ac.una.glifo.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun HomeScreen(
    onNavigateToCourse: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStudy: () -> Unit
) {
    Scaffold(
        topBar = {
            GlifoTopBar(
                title = "Hola, Estudiante"
            )
        },
        bottomBar = {
            GlifoBottomBar(
                currentRoute = "home",
                onNavigateToHome = { },
                onNavigateToStudy = onNavigateToStudy,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Join course */ },
                containerColor = GlifoTheme.colors.accent,
                contentColor = GlifoTheme.colors.onAccent
            ) {
                Text("+", fontSize = 24.sp, modifier = Modifier.padding(12.dp))
            }
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
                text = "Mis cursos",
                color = GlifoTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Mock Data
            GlifoCard(
                onClick = { onNavigateToCourse(1L) },
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("📐 Cálculo II", color = GlifoTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("MAT-202", color = GlifoTheme.colors.textSecondary, fontSize = 12.sp)
                    }
                    Text("68%", color = GlifoTheme.colors.accentText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            GlifoCard(
                onClick = { onNavigateToCourse(2L) },
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("💻 Programación IV", color = GlifoTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("EIF-209", color = GlifoTheme.colors.textSecondary, fontSize = 12.sp)
                    }
                    Text("82%", color = GlifoTheme.colors.accentText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

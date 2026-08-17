package cr.ac.una.glifo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    var syncWiFiOnly by remember { mutableStateOf(true) }
    var saveOriginalPhotos by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GlifoTopBar(title = "Ajustes", onNavigateBack = onNavigateBack)
        },
        containerColor = GlifoTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            SettingsSectionTitle("CAPTURA")
            SettingsItemToggle("Guardar fotos originales", saveOriginalPhotos) { saveOriginalPhotos = it }
            
            SettingsSectionTitle("PROCESAMIENTO")
            SettingsItem("Nivel de IA por defecto", "Automático (hasta N3)")

            SettingsSectionTitle("SINCRONIZACIÓN")
            SettingsItemToggle("Solo con Wi-Fi", syncWiFiOnly) { syncWiFiOnly = it }
            SettingsItem("Última sincronización", "Hoy, 9:41 AM")

            SettingsSectionTitle("CUENTA")
            SettingsItem("Correo", "bbrenes@est.una.ac.cr")

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlifoTheme.colors.alert,
                    contentColor = GlifoTheme.colors.background
                )
            ) {
                Text("Cerrar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = GlifoTheme.colors.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(text = title, color = GlifoTheme.colors.textPrimary, fontSize = 16.sp)
        Text(text = subtitle, color = GlifoTheme.colors.textSecondary, fontSize = 14.sp)
    }
}

@Composable
fun SettingsItemToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = GlifoTheme.colors.textPrimary, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GlifoTheme.colors.onAccent,
                checkedTrackColor = GlifoTheme.colors.accent,
                uncheckedThumbColor = GlifoTheme.colors.textSecondary,
                uncheckedTrackColor = GlifoTheme.colors.surfaceHigh
            )
        )
    }
}

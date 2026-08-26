package cr.ac.una.glifo.feature.settings.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummySyncProvider
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.core.ui.theme.ThemeManager
import cr.ac.una.glifo.core.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSync: () -> Unit = {},
    onLogout: () -> Unit
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current

    var savePhoto by remember { mutableStateOf(true) }
    var rejectLow by remember { mutableStateOf(true) }
    var allowN2 by remember { mutableStateOf(true) }
    var confirmN3 by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(false) }
    var notifyDone by remember { mutableStateOf(true) }
    var confidenceThreshold by remember { mutableStateOf(0.70f) }
    val pendingCount = DummySyncProvider.getPendingCount()

    @Composable
    fun GlifoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        val thumbOffset by animateDpAsState(
            targetValue = if (checked) 20.dp else 0.dp,
            animationSpec = tween(180),
            label = "switch-thumb"
        )
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .background(if (checked) colors.accent else colors.border, RoundedCornerShape(999.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(18.dp)
                    .background(if (checked) colors.onAccent else colors.textSecondary, CircleShape)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Ajustes",
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
                text = "APARIENCIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlifoCard(modifier = Modifier.padding(bottom = 18.dp)) {
                Text("Tema", fontSize = 16.sp, color = colors.textPrimary)
                Text(
                    text = "Elegí cómo se ve Glifo, o dejá que siga el ajuste del sistema.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.Light to "Claro",
                        ThemeMode.Dark to "Oscuro",
                        ThemeMode.System to "Sistema"
                    ).forEach { (mode, label) ->
                        val selected = ThemeManager.mode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                                .clickable { ThemeManager.setMode(context, mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) colors.accentText else colors.textSecondary)
                        }
                    }
                }
            }

            Text(
                text = "CAPTURA",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                GlifoItem(compact = true) {
                    Text("Guardar la foto original", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(savePhoto) { savePhoto = it }
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Text("Rechazar fotos de baja calidad", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(rejectLow) { rejectLow = it }
                }
            }

            Text(
                text = "PROCESAMIENTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(modifier = Modifier.padding(bottom = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Umbral de confianza", fontSize = 16.sp, color = colors.textPrimary)
                    Text(String.format("%.2f", confidenceThreshold), fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = colors.accentText)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surfaceHigh)
                ) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(confidenceThreshold).background(colors.accent))
                }
                Text(
                    text = "Por debajo de este valor la región escala al siguiente motor. Más alto significa más precisión y más llamadas.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            GlifoCard(tight = true) {
                GlifoItem(compact = true) {
                    Text("Permitir escalar a visión (N2)", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(allowN2) { allowN2 = it }
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Text("Pedir confirmación antes de N3", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(confirmN3) { confirmN3 = it }
                }
            }

            Text(
                text = "SINCRONIZACIÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                GlifoItem(compact = true) {
                    Text("Sincronizar solo con wifi", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(wifiOnly) { wifiOnly = it }
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Text("Avisar cuando termine la cola", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(notifyDone) { notifyDone = it }
                }
            }

            Text(
                text = "ACERCA DE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                GlifoItem(compact = true, onClick = onNavigateToSync) {
                    Text("Cola de sincronización", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("$pendingCount pendientes", fontSize = 13.sp, color = colors.textSecondary)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Text("Versión", fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("1.0.0 · X-Ray", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlifoButton(
                text = "Cerrar sesión",
                variant = GlifoButtonVariant.Danger,
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onLogout
            )
        }
    }
}

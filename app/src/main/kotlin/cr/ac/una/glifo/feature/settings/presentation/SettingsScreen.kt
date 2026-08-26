package cr.ac.una.glifo.feature.settings.presentation

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSync: () -> Unit = {},
    onLogout: () -> Unit
) {
    val colors = GlifoTheme.colors

    var savePhoto by remember { mutableStateOf(true) }
    var rejectLow by remember { mutableStateOf(true) }
    var allowN2 by remember { mutableStateOf(true) }
    var confirmN3 by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(false) }
    var notifyDone by remember { mutableStateOf(true) }

    @Composable
    fun GlifoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .background(if (checked) colors.accent else colors.border, RoundedCornerShape(999.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
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
                    Text("0.70", fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = colors.accentText)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surfaceHigh)
                ) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.70f).background(colors.accent))
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
                    Text("2 pendientes", fontSize = 13.sp, color = colors.textSecondary)
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

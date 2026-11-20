package cr.ac.una.glifo.presentation.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.core.ui.theme.AppLanguage
import cr.ac.una.glifo.core.ui.theme.LanguageManager
import cr.ac.una.glifo.core.ui.theme.ThemeManager
import cr.ac.una.glifo.core.ui.theme.ThemeMode
import cr.ac.una.glifo.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSync: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current

    // Antes eran estados locales que no se guardaban ni se usaban. Ahora salen de AppPreferences
    // y los usan la captura (rechazar baja calidad), el sync (N2) y WorkManager (solo Wi-Fi).
    val rejectLow by viewModel.rejectLowQuality.collectAsState()
    val allowN2 by viewModel.allowVisionEscalation.collectAsState()
    val wifiOnly by viewModel.syncWifiOnly.collectAsState()
    val notifyDone by viewModel.notifySyncDone.collectAsState()
    val dailyReminder by viewModel.dailyStudyReminder.collectAsState()
    // Umbral real del pipeline (ConfidenceScorer / RegionClassifier); se muestra, no se edita.
    val confidenceThreshold = cr.ac.una.glifo.core.confidence.ConfidenceScorer.DEFAULT_THRESHOLD
    val pendingCountState by viewModel.pendingCount.collectAsState()
    val pendingCount = (pendingCountState as? UiState.Success)?.data ?: 0

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotifySyncDone(granted)
    }

    val reminderPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setDailyStudyReminder(granted)
    }

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
            title = stringResource(R.string.settings_title),
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
                text = stringResource(R.string.settings_section_appearance),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlifoCard(modifier = Modifier.padding(bottom = 18.dp)) {
                Text(stringResource(R.string.settings_theme_title), fontSize = 16.sp, color = colors.textPrimary)
                Text(
                    text = stringResource(R.string.settings_theme_description),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.Light to stringResource(R.string.settings_theme_light),
                        ThemeMode.Dark to stringResource(R.string.settings_theme_dark),
                        ThemeMode.System to stringResource(R.string.settings_theme_system)
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

            GlifoCard(modifier = Modifier.padding(bottom = 18.dp)) {
                Text(stringResource(R.string.settings_language_title), fontSize = 16.sp, color = colors.textPrimary)
                Text(
                    text = stringResource(R.string.settings_language_description),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AppLanguage.System to stringResource(R.string.settings_language_system),
                        AppLanguage.Spanish to stringResource(R.string.settings_language_es),
                        AppLanguage.English to stringResource(R.string.settings_language_en)
                    ).forEach { (lang, label) ->
                        val selected = LanguageManager.language == lang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) colors.accentSoft else colors.neutralSoft, RoundedCornerShape(999.dp))
                                .clickable { LanguageManager.setLanguage(context, lang) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) colors.accentText else colors.textSecondary)
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_section_capture),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // "Guardar la foto original" se quitó: la foto se necesita para mostrar y enviar los recortes.
            GlifoCard(tight = true) {
                GlifoItem(compact = true, showBottomBorder = false) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_reject_low_quality), fontSize = 15.sp, color = colors.textPrimary)
                        Text(stringResource(R.string.settings_reject_low_quality_description), fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp)
                    }
                    GlifoSwitch(rejectLow) { viewModel.setRejectLowQuality(it) }
                }
            }

            Text(
                text = stringResource(R.string.settings_section_processing),
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
                    Text(stringResource(R.string.settings_confidence_threshold), fontSize = 16.sp, color = colors.textPrimary)
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
                    text = stringResource(R.string.settings_confidence_threshold_description),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // "Pedir confirmación antes de N3" se quitó: la escalera termina en N2, no existe N3.
            GlifoCard(tight = true) {
                GlifoItem(compact = true, showBottomBorder = false) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_allow_vision_escalation), fontSize = 15.sp, color = colors.textPrimary)
                        Text(stringResource(R.string.settings_allow_vision_escalation_description), fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp)
                    }
                    GlifoSwitch(allowN2) { viewModel.setAllowVisionEscalation(it) }
                }
            }

            Text(
                text = stringResource(R.string.settings_section_sync),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                GlifoItem(compact = true) {
                    Text(stringResource(R.string.settings_sync_wifi_only), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(wifiOnly) { viewModel.setSyncWifiOnly(it) }
                }
                GlifoItem(compact = true) {
                    Text(stringResource(R.string.settings_daily_study_reminder), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(dailyReminder) { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setDailyStudyReminder(true)
                            }
                        } else {
                            viewModel.setDailyStudyReminder(enabled)
                        }
                    }
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Text(stringResource(R.string.settings_notify_queue_done), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    GlifoSwitch(notifyDone) { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setNotifySyncDone(true)
                            }
                        } else {
                            viewModel.setNotifySyncDone(enabled)
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_section_about),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                GlifoItem(compact = true, onClick = onNavigateToSync) {
                    Text(stringResource(R.string.settings_sync_queue), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.settings_pending_count, pendingCount), fontSize = 13.sp, color = colors.textSecondary)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                GlifoItem(compact = true, showBottomBorder = false) {
                    Text(stringResource(R.string.settings_version_label), fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text(cr.ac.una.glifo.BuildConfig.VERSION_NAME, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = colors.textSecondary)
                }
            }

            Text(
                text = stringResource(R.string.settings_section_diagnostics),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                GlifoItem(
                    compact = true,
                    onClick = {
                        cr.ac.una.glifo.core.telemetry.GlifoCrashReporter.setCustomKey("triggered_by", "settings_screen")
                        cr.ac.una.glifo.core.telemetry.GlifoCrashReporter.log("Probando caída de diagnóstico en Firebase Crashlytics")
                        cr.ac.una.glifo.core.telemetry.GlifoCrashReporter.testCrash()
                    }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_test_crashlytics_title), fontSize = 15.sp, color = colors.alert)
                        Text(stringResource(R.string.settings_test_crashlytics_description), fontSize = 12.sp, color = colors.textSecondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                GlifoItem(
                    compact = true,
                    showBottomBorder = false,
                    onClick = {
                        cr.ac.una.glifo.core.telemetry.GlifoCrashReporter.log("Reporte no fatal manual generado desde Ajustes")
                        cr.ac.una.glifo.core.telemetry.GlifoCrashReporter.recordException(Exception("Diagnóstico manual no fatal - Telemetría OK"), "manual_diagnostics")
                    }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_send_nonfatal_title), fontSize = 15.sp, color = colors.textPrimary)
                        Text(stringResource(R.string.settings_send_nonfatal_description), fontSize = 12.sp, color = colors.textSecondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlifoButton(
                text = stringResource(R.string.profile_button_logout),
                variant = GlifoButtonVariant.Danger,
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onLogout
            )
        }
    }
}

package cr.ac.una.glifo.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Stable
class GlifoColorScheme(
    background: Color,
    surface: Color,
    surfaceHigh: Color,
    border: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    accentText: Color,
    onAccent: Color,
    accentSoft: Color,
    accentFaint: Color,
    accentLine: Color,
    alert: Color,
    alertSoft: Color,
    alertFaint: Color,
    alertLine: Color,
    scrim: Color,
    verified: Color,
    verifiedSoft: Color,
    verifiedLine: Color,
    repaired: Color,
    repairedSoft: Color,
    repairedLine: Color,
    escalated: Color,
    escalatedSoft: Color,
    escalatedFaint: Color,
    escalatedLine: Color,
    uncertain: Color,
    uncertainSoft: Color,
    uncertainFaint: Color,
    uncertainLine: Color,
    neutralSoft: Color,
    btnSecBorder: Color,
    btnSecText: Color
) {
    var background by mutableStateOf(background)
        internal set
    var surface by mutableStateOf(surface)
        internal set
    var surfaceHigh by mutableStateOf(surfaceHigh)
        internal set
    var border by mutableStateOf(border)
        internal set
    var textPrimary by mutableStateOf(textPrimary)
        internal set
    var textSecondary by mutableStateOf(textSecondary)
        internal set
    var accent by mutableStateOf(accent)
        internal set
    var accentText by mutableStateOf(accentText)
        internal set
    var onAccent by mutableStateOf(onAccent)
        internal set
    var accentSoft by mutableStateOf(accentSoft)
        internal set
    var accentFaint by mutableStateOf(accentFaint)
        internal set
    var accentLine by mutableStateOf(accentLine)
        internal set
    var alert by mutableStateOf(alert)
        internal set
    var alertSoft by mutableStateOf(alertSoft)
        internal set
    var alertFaint by mutableStateOf(alertFaint)
        internal set
    var alertLine by mutableStateOf(alertLine)
        internal set
    var scrim by mutableStateOf(scrim)
        internal set
    var verified by mutableStateOf(verified)
        internal set
    var verifiedSoft by mutableStateOf(verifiedSoft)
        internal set
    var verifiedLine by mutableStateOf(verifiedLine)
        internal set
    var repaired by mutableStateOf(repaired)
        internal set
    var repairedSoft by mutableStateOf(repairedSoft)
        internal set
    var repairedLine by mutableStateOf(repairedLine)
        internal set
    var escalated by mutableStateOf(escalated)
        internal set
    var escalatedSoft by mutableStateOf(escalatedSoft)
        internal set
    var escalatedFaint by mutableStateOf(escalatedFaint)
        internal set
    var escalatedLine by mutableStateOf(escalatedLine)
        internal set
    var uncertain by mutableStateOf(uncertain)
        internal set
    var uncertainSoft by mutableStateOf(uncertainSoft)
        internal set
    var uncertainFaint by mutableStateOf(uncertainFaint)
        internal set
    var uncertainLine by mutableStateOf(uncertainLine)
        internal set
    var neutralSoft by mutableStateOf(neutralSoft)
        internal set
    var btnSecBorder by mutableStateOf(btnSecBorder)
        internal set
    var btnSecText by mutableStateOf(btnSecText)
        internal set
}

val LocalGlifoColors = staticCompositionLocalOf<GlifoColorScheme> {
    error("No GlifoColorScheme provided")
}

private val DarkColorScheme = GlifoColorScheme(
    background = NightColors.Background,
    surface = NightColors.Surface,
    surfaceHigh = NightColors.SurfaceHigh,
    border = NightColors.Border,
    textPrimary = NightColors.TextPrimary,
    textSecondary = NightColors.TextSecondary,
    accent = NightColors.Accent,
    accentText = NightColors.AccentText,
    onAccent = NightColors.OnAccent,
    accentSoft = NightColors.AccentSoft,
    accentFaint = NightColors.AccentFaint,
    accentLine = NightColors.AccentLine,
    alert = NightColors.Alert,
    alertSoft = NightColors.AlertSoft,
    alertFaint = NightColors.AlertFaint,
    alertLine = NightColors.AlertLine,
    scrim = NightColors.Scrim,
    verified = NightColors.Verified,
    verifiedSoft = NightColors.VerifiedSoft,
    verifiedLine = NightColors.VerifiedLine,
    repaired = NightColors.Repaired,
    repairedSoft = NightColors.RepairedSoft,
    repairedLine = NightColors.RepairedLine,
    escalated = NightColors.Escalated,
    escalatedSoft = NightColors.EscalatedSoft,
    escalatedFaint = NightColors.EscalatedFaint,
    escalatedLine = NightColors.EscalatedLine,
    uncertain = NightColors.Uncertain,
    uncertainSoft = NightColors.UncertainSoft,
    uncertainFaint = NightColors.UncertainFaint,
    uncertainLine = NightColors.UncertainLine,
    neutralSoft = NightColors.NeutralSoft,
    btnSecBorder = NightColors.BtnSecBorder,
    btnSecText = NightColors.BtnSecText
)

private val LightColorScheme = GlifoColorScheme(
    background = DayColors.Background,
    surface = DayColors.Surface,
    surfaceHigh = DayColors.SurfaceHigh,
    border = DayColors.Border,
    textPrimary = DayColors.TextPrimary,
    textSecondary = DayColors.TextSecondary,
    accent = DayColors.Accent,
    accentText = DayColors.AccentText,
    onAccent = DayColors.OnAccent,
    accentSoft = DayColors.AccentSoft,
    accentFaint = DayColors.AccentFaint,
    accentLine = DayColors.AccentLine,
    alert = DayColors.Alert,
    alertSoft = DayColors.AlertSoft,
    alertFaint = DayColors.AlertFaint,
    alertLine = DayColors.AlertLine,
    scrim = DayColors.Scrim,
    verified = DayColors.Verified,
    verifiedSoft = DayColors.VerifiedSoft,
    verifiedLine = DayColors.VerifiedLine,
    repaired = DayColors.Repaired,
    repairedSoft = DayColors.RepairedSoft,
    repairedLine = DayColors.RepairedLine,
    escalated = DayColors.Escalated,
    escalatedSoft = DayColors.EscalatedSoft,
    escalatedFaint = DayColors.EscalatedFaint,
    escalatedLine = DayColors.EscalatedLine,
    uncertain = DayColors.Uncertain,
    uncertainSoft = DayColors.UncertainSoft,
    uncertainFaint = DayColors.UncertainFaint,
    uncertainLine = DayColors.UncertainLine,
    neutralSoft = DayColors.NeutralSoft,
    btnSecBorder = DayColors.BtnSecBorder,
    btnSecText = DayColors.BtnSecText
)

private val MaterialDarkColorScheme = darkColorScheme(
    primary = NightColors.Accent,
    onPrimary = NightColors.OnAccent,
    background = NightColors.Background,
    onBackground = NightColors.TextPrimary,
    surface = NightColors.Surface,
    onSurface = NightColors.TextPrimary,
    error = NightColors.Alert,
    onError = Color.White
)

private val MaterialLightColorScheme = lightColorScheme(
    primary = DayColors.Accent,
    onPrimary = DayColors.OnAccent,
    background = DayColors.Background,
    onBackground = DayColors.TextPrimary,
    surface = DayColors.Surface,
    onSurface = DayColors.TextPrimary,
    error = DayColors.Alert,
    onError = Color.White
)

object GlifoTheme {
    val colors: GlifoColorScheme
        @Composable
        get() = LocalGlifoColors.current
}

@Composable
fun GlifoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val materialColorScheme = if (darkTheme) MaterialDarkColorScheme else LightColorScheme.let { MaterialLightColorScheme }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalGlifoColors provides colorScheme) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

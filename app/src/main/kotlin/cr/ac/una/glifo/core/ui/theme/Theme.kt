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
    alert: Color,
    scrim: Color,
    verified: Color,
    repaired: Color,
    escalated: Color,
    uncertain: Color
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
    var alert by mutableStateOf(alert)
        internal set
    var scrim by mutableStateOf(scrim)
        internal set
    var verified by mutableStateOf(verified)
        internal set
    var repaired by mutableStateOf(repaired)
        internal set
    var escalated by mutableStateOf(escalated)
        internal set
    var uncertain by mutableStateOf(uncertain)
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
    alert = NightColors.Alert,
    scrim = NightColors.Scrim,
    verified = NightColors.Verified,
    repaired = NightColors.Repaired,
    escalated = NightColors.Escalated,
    uncertain = NightColors.Uncertain
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
    alert = DayColors.Alert,
    scrim = DayColors.Scrim,
    verified = DayColors.Verified,
    repaired = DayColors.Repaired,
    escalated = DayColors.Escalated,
    uncertain = DayColors.Uncertain
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
    val materialColorScheme = if (darkTheme) MaterialDarkColorScheme else MaterialLightColorScheme

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

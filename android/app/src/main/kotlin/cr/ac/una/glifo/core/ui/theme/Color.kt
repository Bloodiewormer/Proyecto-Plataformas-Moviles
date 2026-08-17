package cr.ac.una.glifo.core.ui.theme

import androidx.compose.ui.graphics.Color

// Night Mode (Default)
object NightColors {
    val Background = Color(0xFF161E27)
    val Surface = Color(0xFF2E3B4B)
    val SurfaceHigh = Color(0xFF3B4A5C)
    val Border = Color(0xFF4A5A6E)
    val TextPrimary = Color(0xFFD7D1B9)
    val TextSecondary = Color(0xFF959595)
    val Accent = Color(0xFFFFD372)
    val AccentText = Color(0xFFFFD372)
    val OnAccent = Color(0xFF1A1206)
    val Alert = Color(0xFFE0693A)
    val Scrim = Color(0xB8080C11) // rgba(8,12,17,.72)

    // Confidence Map
    val Verified = Color(0xFF5FA88C)
    val Repaired = Color(0xFF8FB7DC)
    val Escalated = Color(0xFFF59E0B)
    val Uncertain = Color(0xFF959595)
}

// Day Mode
object DayColors {
    val Background = Color(0xFFEDEAE0)
    val Surface = Color(0xFFF7F4EC)
    val SurfaceHigh = Color(0xFFD7D1B9)
    val Border = Color(0xFFC4BCA3)
    val TextPrimary = Color(0xFF2E3B4B)
    val TextSecondary = Color(0xFF63666A)
    val Accent = Color(0xFFFFD372)
    val AccentText = Color(0xFF8A6210)
    val OnAccent = Color(0xFF2E3B4B)
    val Alert = Color(0xFFB94117)
    val Scrim = Color(0x802E3B4B) // rgba(46,59,75,.5)

    // Confidence Map
    val Verified = Color(0xFF2F7D62)
    val Repaired = Color(0xFF3E6E9E)
    val Escalated = Color(0xFFD97706)
    val Uncertain = Color(0xFF63666A)
}

// Backward compatibility alias
val NightBackground = NightColors.Background

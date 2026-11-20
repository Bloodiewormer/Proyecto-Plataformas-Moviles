package cr.ac.una.glifo.core.ui.theme

import androidx.compose.ui.graphics.Color

// Night Mode (Default)
object NightColors {
    val Background = Color(0xFF161E27)
    val Surface = Color(0xFF2E3B4B)
    val SurfaceHigh = Color(0xFF3B4A5C)
    val Border = Color(0xFF4A5A6E)
    
    val Accent = Color(0xFFFFD372)
    val AccentText = Color(0xFFFFD372)
    val OnAccent = Color(0xFF1A1206)
    val AccentSoft = Color(0x29FFD372)   // rgba(255,211,114,.16)
    val AccentFaint = Color(0x14FFD372)  // rgba(255,211,114,.08)
    val AccentLine = Color(0x6BFFD372)   // rgba(255,211,114,.42)

    val TextPrimary = Color(0xFFD7D1B9)
    val TextSecondary = Color(0xFF959595)

    val Alert = Color(0xFFE0693A)
    val AlertSoft = Color(0x2EE0693A)    // rgba(224,105,58,.18)
    val AlertFaint = Color(0x17E0693A)   // rgba(224,105,58,.09)
    val AlertLine = Color(0x73E0693A)    // rgba(224,105,58,.45)

    val Verified = Color(0xFF5FA88C)
    val VerifiedSoft = Color(0x2E5FA88C) // rgba(95,168,140,.18)
    val VerifiedLine = Color(0x735FA88C) // rgba(95,168,140,.45)

    val Repaired = Color(0xFF8FB7DC)
    val RepairedSoft = Color(0x2E8FB7DC) // rgba(143,183,220,.18)
    val RepairedLine = Color(0x738FB7DC) // rgba(143,183,220,.45)

    val Escalated = Color(0xFFF59E0B)
    val EscalatedSoft = Color(0x2EF59E0B) // rgba(245,158,11,.18)
    val EscalatedFaint = Color(0x17F59E0B) // rgba(245,158,11,.09)
    val EscalatedLine = Color(0x73F59E0B) // rgba(245,158,11,.45)

    val Uncertain = Color(0xFF959595)
    val UncertainSoft = Color(0x33959595) // rgba(149,149,149,.20)
    val UncertainFaint = Color(0x1A959595) // rgba(149,149,149,.10)
    val UncertainLine = Color(0x73959595) // rgba(149,149,149,.45)

    val NeutralSoft = Color(0x2E959595)   // rgba(149,149,149,.18)
    val BtnSecBorder = Color(0xFFFFD372)
    val BtnSecText = Color(0xFFFFD372)
    val Scrim = Color(0xB8080C11)         // rgba(8,12,17,.72)
}

// Day Mode
object DayColors {
    val Background = Color(0xFFEDEAE0)
    val Surface = Color(0xFFF7F4EC)
    val SurfaceHigh = Color(0xFFD7D1B9)
    val Border = Color(0xFFC4BCA3)

    val Accent = Color(0xFFFFD372)
    val AccentText = Color(0xFF8A6210)
    val OnAccent = Color(0xFF2E3B4B)
    val AccentSoft = Color(0x33C48F14)   // rgba(196,143,20,.20)
    val AccentFaint = Color(0x1AC48F14)  // rgba(196,143,20,.10)
    val AccentLine = Color(0x80A0740F)   // rgba(160,116,15,.5)

    val TextPrimary = Color(0xFF2E3B4B)
    val TextSecondary = Color(0xFF63666A)

    val Alert = Color(0xFFB94117)
    val AlertSoft = Color(0x24B94117)    // rgba(185,65,23,.14)
    val AlertFaint = Color(0x12B94117)   // rgba(185,65,23,.07)
    val AlertLine = Color(0x6BB94117)    // rgba(185,65,23,.42)

    val Verified = Color(0xFF2F7D62)
    val VerifiedSoft = Color(0x242F7D62) // rgba(47,125,98,.14)
    val VerifiedLine = Color(0x6B2F7D62) // rgba(47,125,98,.42)

    val Repaired = Color(0xFF3E6E9E)
    val RepairedSoft = Color(0x243E6E9E) // rgba(62,110,158,.14)
    val RepairedLine = Color(0x6B3E6E9E) // rgba(62,110,158,.42)

    val Escalated = Color(0xFFD97706)
    val EscalatedSoft = Color(0x24D97706) // rgba(217,119,6,.14)
    val EscalatedFaint = Color(0x12D97706) // rgba(217,119,6,.07)
    val EscalatedLine = Color(0x6BD97706) // rgba(217,119,6,.42)

    val Uncertain = Color(0xFF63666A)
    val UncertainSoft = Color(0x2463666A) // rgba(99,102,106,.14)
    val UncertainFaint = Color(0x1263666A) // rgba(99,102,106,.07)
    val UncertainLine = Color(0x6163666A) // rgba(99,102,106,.38)

    val NeutralSoft = Color(0x2463666A)   // rgba(99,102,106,.14)
    val BtnSecBorder = Color(0xFFA07413)
    val BtnSecText = Color(0xFF8A6210)
    val Scrim = Color(0x802E3B4B)         // rgba(46,59,75,.5)
}

// Backward compatibility alias
val NightBackground = NightColors.Background

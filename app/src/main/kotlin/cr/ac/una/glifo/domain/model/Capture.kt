package cr.ac.una.glifo.domain.model

enum class CaptureQualityIssue {
    Blurry,
    Underexposed,
    Overexposed,
    Other
}

data class CaptureQualityCheck(
    val label: String,
    val scoreLabel: String,
    val percent: Float,
    val isWarning: Boolean,
    val issue: CaptureQualityIssue? = null
)
data class CaptureStep(val state: String, val name: String, val description: String)

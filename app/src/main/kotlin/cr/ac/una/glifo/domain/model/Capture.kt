package cr.ac.una.glifo.domain.model

data class CaptureQualityCheck(val label: String, val scoreLabel: String, val percent: Float, val isWarning: Boolean)
data class CaptureStep(val state: String, val name: String, val description: String)

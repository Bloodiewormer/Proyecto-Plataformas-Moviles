package cr.ac.una.glifo.pipeline.model

data class QualityMetrics(
    val blurVariance: Float,
    val brightness: Float,
    val glareRatio: Float,
    val skewAngleDeg: Float,
    val isUsable: Boolean,
    val rejectionReason: RejectionReason?
)

enum class RejectionReason {
    TOO_BLURRY,
    TOO_DARK,
    GLARE,
    SKEW_EXCESSIVE,
    NO_TEXT_DETECTED
}

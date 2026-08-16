package cr.ac.una.glifo.pipeline.model

data class ConfidenceScore(
    val value: Float,
    val breakdown: Map<String, Float>,
    val isAboveThreshold: Boolean
)

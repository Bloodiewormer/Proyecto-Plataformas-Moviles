package cr.ac.una.glifo.pipeline.model

data class RegionResult(
    val regionId: String,
    val finalText: String? = null,
    val latex: String? = null,
    val resolvedAt: ProcessingLevel,
    val confidence: ConfidenceScore,
    val isUncertain: Boolean
)

package cr.ac.una.glifo.pipeline.model

data class PageResult(
    val pageIndex: Int,
    val perceptualHash: String,
    val levelReached: ProcessingLevel,
    val overallConfidence: Float,
    val quality: QualityMetrics,
    val regions: List<RegionResult>
)

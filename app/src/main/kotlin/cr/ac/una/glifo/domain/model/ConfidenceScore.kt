package cr.ac.una.glifo.domain.model

/**
 * Output of [cr.ac.una.glifo.core.confidence.ConfidenceScorer]: a calculated, explainable
 * confidence for one OCR fragment. [breakdown] mirrors Glifo_Arquitectura_Estandares.md's
 * confidence map requirement that the score never be a black box — every named signal that
 * fed the final [value] is inspectable.
 */
data class ConfidenceScore(
    val value: Float,
    val breakdown: Map<String, Float>,
    val isAboveThreshold: Boolean
)

package cr.ac.una.glifo.domain.model

/**
 * What the N1 local text OCR step (ML Kit Text Recognition) hands off for one region/fragment.
 * ML Kit's public API does not expose a per-element confidence score, so [ConfidenceScorer]
 * cannot just read one off — it derives a score from the structural signals ML Kit *does*
 * expose: how much of the region actually got recognized text ([recognizedArea] vs
 * [regionArea]), and how that text is segmented ([lineCount], [elementCount] — roughly one
 * element per word/token ML Kit could isolate).
 */
data class OcrResult(
    val recognizedText: String,
    val elementCount: Int,
    val lineCount: Int,
    val recognizedArea: Int,
    val regionArea: Int
)

/**
 * Output of [ConfidenceScorer]: a calculated, explainable confidence for one [OcrResult].
 * [breakdown] mirrors Glifo_Arquitectura_Estandares.md's confidence map requirement that the
 * score never be a black box — every named signal that fed the final [value] is inspectable.
 */
data class ConfidenceScore(
    val value: Float,
    val breakdown: Map<String, Float>,
    val isAboveThreshold: Boolean
)

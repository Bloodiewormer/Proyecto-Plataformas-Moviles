package cr.ac.una.glifo.core.confidence

import cr.ac.una.glifo.domain.model.ConfidenceScore
import cr.ac.una.glifo.domain.model.OcrResult
import kotlin.math.max
import kotlin.math.min

/**
 * Tunable weights for [ConfidenceScorer]. Split out from the algorithm so recalibration
 * against real capture data (Glifo_Alcance.md §13: "fixed set of calibration photographs
 * defined week 1; adjust weights against it") is a constant change, not a rewrite.
 *
 * The three weights that feed the base score must sum to 1f; [glossaryBonus] is applied on
 * top, uncapped by that sum, and the final value is still clamped to `[0f, 1f]`.
 */
data class ScoringWeights(
    val coverage: Float = 0.4f,
    val legibility: Float = 0.4f,
    val fragmentation: Float = 0.2f,
    val glossaryBonus: Float = 0.10f
) {
    init {
        val base = coverage + legibility + fragmentation
        require(base in 0.99f..1.01f) { "coverage + legibility + fragmentation must sum to 1f, was $base" }
    }
}

/**
 * Deterministic, no-AI confidence scoring for one OCR fragment (Glifo_Alcance.md §8: pure
 * function, not a model call). Combines three signals ML Kit's Text Recognition output can
 * actually support — since ML Kit reports no per-element confidence itself — plus an
 * optional boost when the course's canonical glossary confirms a recognized term:
 *
 * - **coverage**: how much of the region's area ML Kit actually attached recognized text to.
 *   A region that produced almost no bounding boxes was likely misread, not just quiet.
 * - **legibility**: fraction of non-whitespace characters that are plausible note content
 *   (letters, digits, common punctuation/math symbols) rather than OCR noise. Garbled
 *   handwriting recognition tends to surface as a burst of unrelated symbols.
 * - **fragmentation**: how closely ML Kit's element count matches the number of
 *   whitespace-separated tokens in the recognized text. Coherent text segments roughly one
 *   element per token; broken recognition (common on math notation) over- or
 *   under-segments.
 * - **glossary bonus**: recognizing a term from the teacher's canonical glossary
 * (Glifo_Alcance.md §4) is strong positive evidence, so it adds to the score. Its *absence*
 *   is not penalized — most fragments legitimately contain no course-specific vocabulary.
 *
 * The calibration threshold and default [ScoringWeights] are placeholders pending the
 * calibration-photo pass called for in Glifo_Alcance.md §13 — there is no production
 * capture data yet to fit them against.
 */
object ConfidenceScorer {

    const val DEFAULT_THRESHOLD = 0.75f

    private val LEGIBLE_CHAR = Regex("[\\p{L}\\p{Nd}\\s.,;:()+\\-=/*%°'\"!?¿¡]")

    fun score(
        ocr: OcrResult,
        glossary: List<String> = emptyList(),
        weights: ScoringWeights = ScoringWeights(),
        threshold: Float = DEFAULT_THRESHOLD
    ): ConfidenceScore {
        val coverage = coverageSignal(ocr)
        val legibility = legibilitySignal(ocr.recognizedText)
        val fragmentation = fragmentationSignal(ocr)

        val base = weights.coverage * coverage +
            weights.legibility * legibility +
            weights.fragmentation * fragmentation

        val matchedGlossaryTerm = glossary.any { term ->
            term.isNotBlank() && ocr.recognizedText.contains(term, ignoreCase = true)
        }
        val glossaryBoost = if (matchedGlossaryTerm) weights.glossaryBonus else 0f

        val value = (base + glossaryBoost).coerceIn(0f, 1f)

        return ConfidenceScore(
            value = value,
            breakdown = mapOf(
                "coverage" to coverage,
                "legibility" to legibility,
                "fragmentation" to fragmentation,
                "glossaryBoost" to glossaryBoost
            ),
            isAboveThreshold = value >= threshold
        )
    }

    private fun coverageSignal(ocr: OcrResult): Float {
        if (ocr.regionArea <= 0) return 0f
        return (ocr.recognizedArea.toFloat() / ocr.regionArea.toFloat()).coerceIn(0f, 1f)
    }

    private fun legibilitySignal(text: String): Float {
        val meaningful = text.filterNot { it.isWhitespace() }
        if (meaningful.isEmpty()) return 0f
        val legibleCount = meaningful.count { LEGIBLE_CHAR.matches(it.toString()) }
        return legibleCount.toFloat() / meaningful.length.toFloat()
    }

    private fun fragmentationSignal(ocr: OcrResult): Float {
        val tokenCount = ocr.recognizedText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        if (tokenCount == 0 && ocr.elementCount == 0) return 0f
        val larger = max(max(tokenCount, ocr.elementCount), 1)
        val smaller = min(tokenCount, ocr.elementCount)
        return smaller.toFloat() / larger.toFloat()
    }
}

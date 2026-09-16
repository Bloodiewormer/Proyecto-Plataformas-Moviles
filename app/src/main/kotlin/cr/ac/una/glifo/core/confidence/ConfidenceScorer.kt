package cr.ac.una.glifo.core.confidence

import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.domain.model.ConfidenceScore
import cr.ac.una.glifo.domain.model.RegionInkMetrics
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
    val inkAgreement: Float = 0.4f,
    val legibility: Float = 0.4f,
    val fragmentation: Float = 0.2f,
    val glossaryBonus: Float = 0.10f
) {
    init {
        val base = inkAgreement + legibility + fragmentation
        require(base in 0.99f..1.01f) { "inkAgreement + legibility + fragmentation must sum to 1f, was $base" }
    }
}

/**
 * Deterministic, no-AI confidence scoring for one N1 fragment (Glifo_Alcance.md §8: pure
 * function, not a model call) — the fallback path for when ML Kit's Text Recognition API
 * does not publish its own per-line confidence for an [OcrResult] (`ocr.confidence == null`;
 * see [cr.ac.una.glifo.core.mlkit.TextRecognizerN1]). [RegionClassifier][
 * cr.ac.una.glifo.core.pipeline.RegionClassifier] calls this only in that gap — a confidence
 * ML Kit actually reported is trusted as-is, never second-guessed by this heuristic.
 *
 * Combines three signals available at that point in the pipeline, plus an optional boost
 * from the course's canonical glossary:
 *
 * - **ink agreement**: recognized character count against [RegionInkMetrics.componentCount]
 *   — the connected-component count OpenCV's segmentation already measured for this region,
 *   a proxy for "how many strokes/glyphs are visibly there". Lots of ink but almost no
 *   recognized text is a stronger tell of a bad read than character count alone; it is also
 *   how a math/drawing region that N1 barely read gets separated from a short but correctly
 *   read word.
 * - **legibility**: fraction of non-whitespace characters that are plausible note content
 *   (letters, digits, common punctuation/math symbols) rather than OCR noise. Garbled
 *   handwriting recognition tends to surface as a burst of unrelated symbols.
 * - **fragmentation**: how closely ML Kit's element count ([OcrResult.wordCount]) matches
 *   the number of whitespace-separated tokens in the recognized text. Coherent text segments
 *   roughly one element per token; broken recognition (common on math notation) over- or
 *   under-segments.
 * - **glossary bonus**: recognizing a term from the teacher's canonical glossary
 *   (Glifo_Alcance.md §4) is strong positive evidence, so it adds to the score. Its
 *   *absence* is not penalized — most fragments legitimately contain no course vocabulary.
 *
 * Threshold and weights are placeholders pending the calibration-photo pass called for in
 * Glifo_Alcance.md §13 — there is no production capture data yet to fit them against.
 */
object ConfidenceScorer {

    const val DEFAULT_THRESHOLD = 0.75f

    private val LEGIBLE_CHAR = Regex("[\\p{L}\\p{Nd}\\s.,;:()+\\-=/*%°'\"!?¿¡]")

    fun score(
        ocr: OcrResult,
        metrics: RegionInkMetrics,
        glossary: List<String> = emptyList(),
        weights: ScoringWeights = ScoringWeights(),
        threshold: Float = DEFAULT_THRESHOLD
    ): ConfidenceScore {
        val chars = ocr.text.count { !it.isWhitespace() }

        // No text means no content to be confident about — regardless of how much ink the
        // region had, a fragment with nothing recognized never earns confidence this way.
        if (chars == 0) {
            return ConfidenceScore(
                value = 0f,
                breakdown = mapOf(
                    "inkAgreement" to 0f,
                    "legibility" to 0f,
                    "fragmentation" to 0f,
                    "glossaryBoost" to 0f
                ),
                isAboveThreshold = false
            )
        }

        val inkAgreement = inkAgreementSignal(chars, metrics)
        val legibility = legibilitySignal(ocr.text)
        val fragmentation = fragmentationSignal(ocr.text, ocr.wordCount)

        val base = weights.inkAgreement * inkAgreement +
            weights.legibility * legibility +
            weights.fragmentation * fragmentation

        val matchedGlossaryTerm = glossary.any { term ->
            term.isNotBlank() && ocr.text.contains(term, ignoreCase = true)
        }
        val glossaryBoost = if (matchedGlossaryTerm) weights.glossaryBonus else 0f

        val value = (base + glossaryBoost).coerceIn(0f, 1f)

        return ConfidenceScore(
            value = value,
            breakdown = mapOf(
                "inkAgreement" to inkAgreement,
                "legibility" to legibility,
                "fragmentation" to fragmentation,
                "glossaryBoost" to glossaryBoost
            ),
            isAboveThreshold = value >= threshold
        )
    }

    /**
     * Compares recognized characters (always > 0 here — [score] short-circuits empty text)
     * against the connected-component count the segmenter already measured for this region.
     * Ink with almost no recognized text is the strongest local signal that N1 missed most
     * of what a region actually contains; no measured components at all (full-page fallback,
     * no segmentation) is treated as no signal either way.
     */
    private fun inkAgreementSignal(chars: Int, metrics: RegionInkMetrics): Float {
        val components = metrics.componentCount
        if (components <= 0) return 0.7f

        val charsPerComponent = chars.toFloat() / components.toFloat()
        return when {
            charsPerComponent >= 0.5f -> 1f
            charsPerComponent >= 0.25f -> 0.75f
            charsPerComponent >= 0.1f -> 0.5f
            else -> 0.2f
        }
    }

    private fun legibilitySignal(text: String): Float {
        val meaningful = text.filterNot { it.isWhitespace() }
        if (meaningful.isEmpty()) return 0f
        val legibleCount = meaningful.count { LEGIBLE_CHAR.matches(it.toString()) }
        return legibleCount.toFloat() / meaningful.length.toFloat()
    }

    private fun fragmentationSignal(text: String, elementCount: Int): Float {
        val tokenCount = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        if (tokenCount == 0 && elementCount == 0) return 0f
        val larger = max(max(tokenCount, elementCount), 1)
        val smaller = min(tokenCount, elementCount)
        return smaller.toFloat() / larger.toFloat()
    }
}

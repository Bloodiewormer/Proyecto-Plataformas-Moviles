package cr.ac.una.glifo.core.confidence

import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.domain.model.ConfidenceScore
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

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

object ConfidenceScorer {

    const val DEFAULT_THRESHOLD = 0.75f

    private const val MLKIT_BLEND_WEIGHT = 0.80f
    private const val UNREAD_INK_CONFIDENCE_CAP = 0.55f

    private val LEGIBLE_CHAR = Regex("[\\p{L}\\p{Nd}\\s.,;:()+\\-=/*%°'\"!?¿¡]")
    private val DIACRITICS = Regex("\\p{Mn}+")

    fun score(
        ocr: OcrResult,
        metrics: RegionInkMetrics,
        glossary: List<String> = emptyList(),
        weights: ScoringWeights = ScoringWeights(),
        threshold: Float = DEFAULT_THRESHOLD
    ): ConfidenceScore {
        val chars = ocr.text.count { !it.isWhitespace() }

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

        val heuristicBase = weights.inkAgreement * inkAgreement +
            weights.legibility * legibility +
            weights.fragmentation * fragmentation

        val normalizedText = normalizeForGlossary(ocr.text)
        val matchedGlossaryTerm = glossary.any { term ->
            val normalizedTerm = normalizeForGlossary(term)
            normalizedTerm.isNotBlank() && normalizedText.contains(normalizedTerm)
        }
        val glossaryBoost = if (matchedGlossaryTerm) weights.glossaryBonus else 0f

        val mlKit = ocr.confidence?.coerceIn(0f, 1f)
        // Cap ML Kit confidence when most ink went unread so 1-char overconfident reads cannot bypass escalation.
        val combinedBase = if (mlKit != null) {
            val effectiveMlKit = if (inkAgreement <= 0.2f) min(mlKit, UNREAD_INK_CONFIDENCE_CAP) else mlKit
            MLKIT_BLEND_WEIGHT * effectiveMlKit + (1f - MLKIT_BLEND_WEIGHT) * heuristicBase
        } else {
            heuristicBase
        }

        val value = (combinedBase + glossaryBoost).coerceIn(0f, 1f)

        val breakdown = buildMap {
            put("inkAgreement", inkAgreement)
            put("legibility", legibility)
            put("fragmentation", fragmentation)
            put("glossaryBoost", glossaryBoost)
            if (mlKit != null) put("mlKitConfidence", mlKit)
        }

        return ConfidenceScore(
            value = value,
            breakdown = breakdown,
            isAboveThreshold = value >= threshold
        )
    }

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

    private fun normalizeForGlossary(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .trim()
}

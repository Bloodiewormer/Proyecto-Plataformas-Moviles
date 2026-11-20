package cr.ac.una.glifo.core.pipeline

import cr.ac.una.glifo.core.confidence.ConfidenceScorer
import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind

object RegionClassifier {

    const val TEXT_CONFIDENCE_GATE = ConfidenceScorer.DEFAULT_THRESHOLD

    private const val MATH_SCORE_GATE = 0.5f
    private const val DRAWING_SCORE_GATE = 0.5f
    internal const val LARGE_2D_STROKE_THRESHOLD = 0.065f

    private val MATH_SYMBOLS = setOf(
        '=', '+', '-', '*', '/', '^', '<', '>', '(', ')', '[', ']', '{', '}',
        '±', '×', '÷', '−', '√', '∫', '∑', '∏',
        '≠', '≤', '≥', '≈', '∞', '∂', 'Δ', 'π',
        'α', 'β', 'θ', 'λ', 'μ', 'σ', 'Ω'
    )

    private val MULTIPLICATION_REGEX = Regex("\\d\\s*[xX]\\s*\\d|\\b[xX]\\s*10|[a-zA-Z]\\s*[xX]\\s*10")
    private val MATH_KEYWORD_REGEX = Regex(
        "\\b(lim|sen|cos|tan|sec|csc|cot|arctan|arcsin|arccos|log|ln|exp|dx|dt|dy|du)\\b",
        RegexOption.IGNORE_CASE
    )
    private val VAR_ASSIGN_REGEX = Regex("\\b[a-zA-Z]\\s*=")

    fun classify(
        index: Int,
        bounds: RegionBounds,
        metrics: RegionInkMetrics,
        ocr: OcrResult,
        glossary: List<String> = emptyList()
    ): RecognizedRegion {
        val text = ocr.text
        val chars = text.count { !it.isWhitespace() }

        // Combine ML Kit confidence with local ink agreement and glossary boost rather than skipping ConfidenceScorer.
        val ocrConfidence = ConfidenceScorer.score(ocr, metrics, glossary).value

        val mathScore = mathScore(text, chars, ocr, metrics)
        val drawingScore = drawingScore(chars, metrics, mathScore)

        val hasLarge2dFigure = metrics.maxStrokeMinSpanRatio >= LARGE_2D_STROKE_THRESHOLD
        val kind = when {
            mathScore >= MATH_SCORE_GATE && !hasLarge2dFigure -> RegionKind.Math
            drawingScore >= DRAWING_SCORE_GATE -> RegionKind.Drawing
            mathScore >= MATH_SCORE_GATE -> RegionKind.Math
            else -> RegionKind.Text
        }

        val kindConfidence = when (kind) {
            RegionKind.Drawing -> drawingScore
            RegionKind.Math -> mathScore
            RegionKind.Text -> (1f - maxOf(drawingScore, mathScore)).coerceIn(0f, 1f)
        }.coerceIn(0f, 1f)

        val belowGate = kind == RegionKind.Text && ocrConfidence < TEXT_CONFIDENCE_GATE

        return RecognizedRegion(
            index = index,
            bounds = bounds,
            text = text,
            lineCount = ocr.lineCount,
            ocrConfidence = ocrConfidence,
            kind = kind,
            kindConfidence = kindConfidence,
            route = routeFor(kind, belowGate),
            confidenceState = stateFor(kind, belowGate),
            metrics = metrics
        )
    }

    internal fun looksLikeMathText(text: String, wordCount: Int): Boolean {
        val chars = text.count { !it.isWhitespace() }
        if (chars == 0) return false
        return mathScore(
            text = text,
            chars = chars,
            ocr = OcrResult(text = text, lineCount = 1, wordCount = wordCount, confidence = null),
            metrics = RegionInkMetrics(inkDensity = 0.05f, componentCount = chars, aspectRatio = 3f)
        ) >= 0.45f
    }

    internal fun looksLikeProseText(text: String, wordCount: Int): Boolean {
        val chars = text.count { !it.isWhitespace() }
        if (chars < 16 || wordCount < 3) return false
        val letters = text.count { it.isLetter() }
        val digits = text.count { it.isDigit() }
        val symbols = text.count { it in MATH_SYMBOLS }
        val letterRatio = letters.toFloat() / chars
        val digitRatio = digits.toFloat() / chars
        val symbolRatio = symbols.toFloat() / chars
        val tokens = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val effectiveWords = maxOf(wordCount, tokens)
        val avgWordLength = chars.toFloat() / effectiveWords.coerceAtLeast(1)
        return avgWordLength >= 3.2f &&
            letterRatio >= 0.70f &&
            digitRatio < 0.12f &&
            symbolRatio < 0.08f &&
            !text.contains('=') &&
            !looksLikeMathText(text, wordCount)
    }

    internal fun drawingScore(chars: Int, metrics: RegionInkMetrics, mathScore: Float): Float {
        val hasLarge2dFigure = metrics.maxStrokeMinSpanRatio >= LARGE_2D_STROKE_THRESHOLD
        val hasFormulaGeometry = metrics.horizontalBarCount > 0 || metrics.tallStrokeCount > 0

        if (chars == 0) {
            // Unread regions with fraction bars or integral strokes are formulas, not figures.
            if (!hasLarge2dFigure && hasFormulaGeometry) return 0.20f
            return if (metrics.inkDensity >= 0.005f) 0.95f else 0.6f
        }

        var score = 0f
        val glyphsPerComponent = chars.toFloat() / metrics.componentCount.coerceAtLeast(1)

        if (hasLarge2dFigure && glyphsPerComponent < 0.45f) {
            score += 0.60f
        } else if (!hasFormulaGeometry && mathScore < 0.35f) {
            val isTallEnough = metrics.heightRatio == 0f || metrics.heightRatio >= 0.08f
            if (metrics.componentCount >= 8 && glyphsPerComponent < 0.25f && metrics.aspectRatio < 2.2f && isTallEnough) {
                score += 0.55f
            }
            if (chars <= 4 && metrics.inkDensity >= 0.04f && metrics.aspectRatio < 2.0f) {
                score += 0.25f
            }
        }

        return score.coerceIn(0f, 1f)
    }

    internal fun mathScore(
        text: String,
        chars: Int,
        ocr: OcrResult,
        metrics: RegionInkMetrics = RegionInkMetrics(inkDensity = 0.05f, componentCount = chars, aspectRatio = 3f)
    ): Float {
        if (chars == 0) {
            var geomOnly = 0f
            if (metrics.maxStrokeMinSpanRatio < LARGE_2D_STROKE_THRESHOLD) {
                if (metrics.horizontalBarCount >= 1) geomOnly += 0.40f
                if (metrics.horizontalBarCount >= 2) geomOnly += 0.10f
                if (metrics.tallStrokeCount >= 1) geomOnly += 0.35f
                if (metrics.tallStrokeCount >= 2) geomOnly += 0.10f
            }
            return geomOnly.coerceIn(0f, 1f)
        }

        val digits = text.count { it.isDigit() }
        val letters = text.count { it.isLetter() }
        val symbols = text.count { it in MATH_SYMBOLS }

        val digitRatio = digits.toFloat() / chars
        val letterRatio = letters.toFloat() / chars
        val symbolRatio = symbols.toFloat() / chars
        val tokens = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val effectiveWordCount = maxOf(ocr.wordCount, tokens)
        val averageWordLength = if (effectiveWordCount == 0) chars.toFloat() else chars.toFloat() / effectiveWordCount

        val hasKeywordMatch = effectiveWordCount <= 12 && MATH_KEYWORD_REGEX.containsMatchIn(text)
        val hasExplicitMathOp = text.contains('=') ||
            text.contains("->") ||
            text.contains("=>") ||
            text.contains('\u2192') ||
            text.contains('\u21d2') ||
            text.contains('^') ||
            MULTIPLICATION_REGEX.containsMatchIn(text) ||
            VAR_ASSIGN_REGEX.containsMatchIn(text) ||
            hasKeywordMatch

        val trimmed = text.trim()
        val isProseHeading = trimmed.endsWith(':') && letterRatio >= 0.70f && digitRatio == 0f && !hasExplicitMathOp
        val isSingleAlphabeticWord = effectiveWordCount == 1 && chars >= 5 && letters == chars && !hasExplicitMathOp
        val isFluentProse = isProseHeading || isSingleAlphabeticWord || (
            chars >= 18 &&
                effectiveWordCount >= 4 &&
                averageWordLength >= 3.3f &&
                letterRatio >= 0.72f &&
                digitRatio < 0.10f &&
                symbolRatio < 0.08f &&
                !hasExplicitMathOp
            )

        var score = 0f

        // Skip geometry bonus on fluent multi-word prose so cursive word strokes do not trigger MATH.
        if (!isFluentProse && metrics.maxStrokeMinSpanRatio < LARGE_2D_STROKE_THRESHOLD) {
            if (metrics.horizontalBarCount >= 1) score += 0.40f
            if (metrics.horizontalBarCount >= 2) score += 0.10f
            if (metrics.tallStrokeCount >= 1) score += 0.35f
            if (metrics.tallStrokeCount >= 2) score += 0.10f
            if (chars <= 12 && (metrics.horizontalBarCount >= 1 || metrics.tallStrokeCount >= 1)) {
                score += 0.10f
            }
        }

        if (
            text.contains('=') ||
            text.contains("->") ||
            text.contains("=>") ||
            text.contains('\u2192') ||
            text.contains('\u21d2') ||
            text.contains('^')
        ) {
            score += 0.35f
        }

        if (MULTIPLICATION_REGEX.containsMatchIn(text) || VAR_ASSIGN_REGEX.containsMatchIn(text)) {
            score += 0.25f
        }

        if (effectiveWordCount <= 6 && MATH_KEYWORD_REGEX.containsMatchIn(text)) {
            score += 0.35f
        } else if (hasKeywordMatch) {
            score += 0.25f
        }

        score += (symbolRatio * 2.5f).coerceAtMost(0.35f)
        score += (digitRatio * 2.0f).coerceAtMost(0.40f)

        if (digitRatio >= 0.25f || symbolRatio >= 0.12f) score += 0.20f
        if (letterRatio < 0.45f) score += 0.15f
        if (ocr.lineCount <= 2 && chars <= 28) score += 0.10f
        if (effectiveWordCount >= 2 && averageWordLength <= 2.8f && chars <= 36) score += 0.15f

        if (isFluentProse) {
            score -= 0.35f
        }

        return score.coerceIn(0f, 1f)
    }

    private fun routeFor(kind: RegionKind, belowGate: Boolean): String = when {
        kind == RegionKind.Drawing -> "N2 - reparacion por vision"
        kind == RegionKind.Math -> "N1.5 - OCR matematico"
        belowGate -> "N2 - reparacion por vision (texto bajo la compuerta)"
        else -> "N1 - texto verificado en el dispositivo"
    }

    private fun stateFor(kind: RegionKind, belowGate: Boolean): ConfidenceState = when {
        kind == RegionKind.Drawing -> ConfidenceState.Escalated
        kind == RegionKind.Math -> ConfidenceState.Repaired
        belowGate -> ConfidenceState.Uncertain
        else -> ConfidenceState.Verified
    }
}

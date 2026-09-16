package cr.ac.una.glifo.core.pipeline

import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind

/**
 * Clasificador post-N1 y enrutador de fragmentos (Issue #17, segunda mitad).
 *
 * Corre despues del OCR local porque las tres clases se separan mucho mejor combinando lo
 * que se leyo con como se ve la region:
 *
 *  - Texto: N1 leyo palabras de largo normal, la mayoria letras.
 *  - Matematica: hay signos de operacion o igualdad, muchos digitos y pocas palabras.
 *  - Dibujo: hay bastante tinta pero N1 casi no pudo leer nada (los trazos de un diagrama
 *    no son glifos, asi que el OCR devuelve vacio o dos caracteres sueltos).
 *
 * Cada region sale de aca ya enrutada al nivel que le corresponde del pipeline.
 */
object RegionClassifier {

    /** Compuerta de N1: debajo de esto el texto no se da por bueno y escala a N2. */
    const val TEXT_CONFIDENCE_GATE = 0.75f

    /** Umbral del puntaje matematico a partir del cual la region se manda a N1.5. */
    private const val MATH_SCORE_GATE = 0.5f

    private val MATH_SYMBOLS = setOf(
        '=', '+', '-', '*', '/', '^', '<', '>', '(', ')', '[', ']', '{', '}',
        '±', '×', '÷', '−', '√', '∫', '∑', '∏',
        '≠', '≤', '≥', '≈', '∞', '∂', 'Δ', 'π',
        'α', 'β', 'θ', 'λ', 'μ', 'σ', 'Ω'
    )

    fun classify(
        index: Int,
        bounds: RegionBounds,
        metrics: RegionInkMetrics,
        ocr: OcrResult
    ): RecognizedRegion {
        val text = ocr.text
        val chars = text.count { !it.isWhitespace() }

        val drawingScore = drawingScore(chars, metrics)
        val mathScore = if (chars == 0) 0f else mathScore(text, chars, ocr)

        val kind = when {
            drawingScore >= 0.5f -> RegionKind.Drawing
            mathScore >= MATH_SCORE_GATE -> RegionKind.Math
            else -> RegionKind.Text
        }

        val kindConfidence = when (kind) {
            RegionKind.Drawing -> drawingScore
            RegionKind.Math -> mathScore
            RegionKind.Text -> (1f - maxOf(drawingScore, mathScore)).coerceIn(0f, 1f)
        }.coerceIn(0f, 1f)

        val belowGate = kind == RegionKind.Text && ocr.confidence < TEXT_CONFIDENCE_GATE

        return RecognizedRegion(
            index = index,
            bounds = bounds,
            text = text,
            lineCount = ocr.lineCount,
            ocrConfidence = ocr.confidence,
            kind = kind,
            kindConfidence = kindConfidence,
            route = routeFor(kind, belowGate),
            confidenceState = stateFor(kind, belowGate),
            metrics = metrics
        )
    }

    /**
     * Que tan probable es que la region sea un dibujo o diagrama. La senal principal es la
     * desproporcion entre trazos y caracteres leidos: un parrafo tiene aproximadamente un
     * componente conectado por letra, un diagrama tiene trazos largos que no produjeron texto.
     */
    private fun drawingScore(chars: Int, metrics: RegionInkMetrics): Float {
        if (chars == 0) {
            // Hay tinta suficiente para haber formado una region, pero N1 no leyo nada.
            return if (metrics.inkDensity >= 0.005f) 0.95f else 0.6f
        }

        var score = 0f

        val glyphsPerComponent = chars.toFloat() / metrics.componentCount.coerceAtLeast(1)
        if (metrics.componentCount >= 8 && glyphsPerComponent < 0.30f) score += 0.55f
        else if (metrics.componentCount >= 5 && glyphsPerComponent < 0.15f) score += 0.45f

        // Mucha superficie ocupada con muy poco texto leido: cajas, flechas, sombreados.
        if (chars <= 4 && metrics.inkDensity >= 0.04f) score += 0.35f

        // Los diagramas tienden a ser bloques altos; una linea de texto es muy apaisada.
        if (metrics.aspectRatio < 2.5f && chars <= 8) score += 0.15f

        return score.coerceIn(0f, 1f)
    }

    /**
     * Que tan probable es que la region sea una formula. Se apoya en la composicion de
     * caracteres: operadores, digitos y palabras cortas contra prosa con palabras largas.
     */
    private fun mathScore(text: String, chars: Int, ocr: OcrResult): Float {
        val digits = text.count { it.isDigit() }
        val letters = text.count { it.isLetter() }
        val symbols = text.count { it in MATH_SYMBOLS }

        val digitRatio = digits.toFloat() / chars
        val letterRatio = letters.toFloat() / chars
        val symbolRatio = symbols.toFloat() / chars
        val averageWordLength = if (ocr.wordCount == 0) chars.toFloat() else chars.toFloat() / ocr.wordCount

        var score = 0f

        // Un signo de igualdad es la pista mas fuerte de todas.
        if (text.contains('=')) score += 0.40f

        score += (symbolRatio * 2.5f).coerceAtMost(0.30f)
        score += (digitRatio * 1.5f).coerceAtMost(0.20f)

        // Pocas letras para la cantidad de caracteres: no es prosa.
        if (letterRatio < 0.45f) score += 0.15f

        // Las formulas son cortas y ocupan una o dos lineas.
        if (ocr.lineCount <= 2 && chars <= 24) score += 0.10f

        // Palabras largas y mayoria de letras: es un parrafo, aunque tenga algun parentesis.
        if (averageWordLength >= 4f && letterRatio > 0.6f) score -= 0.25f

        return score.coerceIn(0f, 1f)
    }

    private fun routeFor(kind: RegionKind, belowGate: Boolean): String = when {
        kind == RegionKind.Drawing -> "N2 - reparacion por vision"
        kind == RegionKind.Math -> "N1.5 - OCR matematico"
        belowGate -> "N2 - reparacion por vision (texto bajo la compuerta)"
        else -> "N1 - texto verificado en el dispositivo"
    }

    /**
     * Tier esperado de la region segun el nivel al que se la enruta, siguiendo la escalera
     * de confianza de la arquitectura: N1 verifica, N1.5 repara, N2 escala y lo que no
     * llega a la compuerta queda marcado como incierto para que lo revise la persona.
     */
    private fun stateFor(kind: RegionKind, belowGate: Boolean): ConfidenceState = when {
        kind == RegionKind.Drawing -> ConfidenceState.Escalated
        kind == RegionKind.Math -> ConfidenceState.Repaired
        belowGate -> ConfidenceState.Uncertain
        else -> ConfidenceState.Verified
    }
}

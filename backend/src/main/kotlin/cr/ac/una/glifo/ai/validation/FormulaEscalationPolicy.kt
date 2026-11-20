package cr.ac.una.glifo.ai.validation

import cr.ac.una.glifo.note.entity.NotePage
import org.springframework.stereotype.Component

/**
 * LaTeX que N1.5 (MathOcrEngine, issue #38) devolvió para una región. Es el ÚNICO LaTeX que
 * se valida: el `text` que sube Android sale del OCR local y no es LaTeX.
 */
data class MathTranscription(
    val latex: String,
    /** Confianza que reporta el motor de N1.5, en 0..1. */
    val confidence: Float
) {
    init {
        require(confidence in 0f..1f) { "La confianza de N1.5 debe estar entre 0 y 1: $confidence" }
    }
}

/** Dónde queda una región MATH después de la compuerta de N1.5 (issue #39). */
enum class FormulaStatus(
    /**
     * `resolvedAt` que le corresponde HOY en la escala que lee Android (NoteContentMapper):
     * N1_5 → Repaired y N2/N3 → Escalated. Por eso [NEEDS_N2] no es "N2": la app lo
     * mostraría como ya reparado por visión.
     */
    val resolvedAt: String
) {
    /** N1.5 todavía no transcribió la región: no hay LaTeX que validar. */
    AWAITING_N1_5("UNRESOLVED"),

    /** El LaTeX de N1.5 compila y supera la compuerta de confianza: no escala. */
    KEEP_N1_5("N1_5"),

    /** Candidata a N2 (issue #41). N2 NO se ejecutó: la región sigue sin resolver. */
    NEEDS_N2("UNRESOLVED")
}

enum class EscalationReason {
    /** JLaTeXMath no pudo compilar el LaTeX (o vino vacío). */
    LATEX_DOES_NOT_COMPILE,

    /** El LaTeX compila, pero N1.5 lo reportó por debajo de [FormulaEscalationPolicy.CONFIDENCE_GATE]. */
    LOW_CONFIDENCE
}

/**
 * Decisión sobre una región MATH. Campos:
 * - [region]: página, región, bbox y OCR original, para que N2 sepa qué recorte reparar.
 * - [status]: la decisión en sí.
 * - [latex]: lo que devolvió N1.5, sin normalizar; null si N1.5 no corrió.
 * - [latexCompiles]: mismo nombre que en el bloque FORMULA de IA-01; null = no se validó.
 * - [escalationReasons]: por qué va a N2; vacío si no escala. Puede haber más de una.
 */
data class FormulaDecision(
    val region: MathRegion,
    val status: FormulaStatus,
    val latex: String?,
    val latexCompiles: Boolean?,
    val escalationReasons: Set<EscalationReason>
)

/**
 * Compuerta entre N1.5 y N2 para las regiones MATH (issue #39): con el LaTeX de N1.5 decide
 * si la región se queda en N1.5 o es candidata a reparación por visión. No ejecuta N1.5 ni
 * N2 ni persiste nada; el orquestador de #41 la usa para elegir qué recortes mandar y IA-01
 * (#42) para escribir `latexCompiles` y `resolvedAt` en el bloque FORMULA.
 */
@Component
class FormulaEscalationPolicy(
    private val latexValidator: LatexValidator
) {

    fun decide(region: MathRegion, transcription: MathTranscription?): FormulaDecision {
        if (transcription == null) {
            return FormulaDecision(region, FormulaStatus.AWAITING_N1_5, latex = null, latexCompiles = null, escalationReasons = emptySet())
        }

        val compiles = latexValidator.compiles(transcription.latex)
        val reasons = buildSet {
            if (!compiles) add(EscalationReason.LATEX_DOES_NOT_COMPILE)
            if (transcription.confidence < CONFIDENCE_GATE) add(EscalationReason.LOW_CONFIDENCE)
        }
        return FormulaDecision(
            region = region,
            status = if (reasons.isEmpty()) FormulaStatus.KEEP_N1_5 else FormulaStatus.NEEDS_N2,
            latex = transcription.latex,
            latexCompiles = compiles,
            escalationReasons = reasons
        )
    }

    /**
     * Decide todas las regiones MATH de una página. [transcriptions] va por id de región DE
     * ESTA página; una región sin entrada queda en [FormulaStatus.AWAITING_N1_5].
     */
    fun decidePage(page: NotePage, transcriptions: Map<String, MathTranscription>): List<FormulaDecision> {
        val regions = page.mathRegions()
        val unknown = transcriptions.keys - regions.map { it.regionId }.toSet()
        require(unknown.isEmpty()) { "La página ${page.pageIndex} no tiene regiones MATH con id $unknown" }
        return regions.map { decide(it, transcriptions[it.regionId]) }
    }

    companion object {
        /**
         * Por debajo de esta confianza de N1.5 la fórmula escala aunque compile. Es la misma
         * compuerta que usa la app para N1 (ConfidenceScorer.DEFAULT_THRESHOLD); se recalibra
         * cuando haya datos reales de SimpleTex (#38).
         */
        const val CONFIDENCE_GATE = 0.75f
    }
}

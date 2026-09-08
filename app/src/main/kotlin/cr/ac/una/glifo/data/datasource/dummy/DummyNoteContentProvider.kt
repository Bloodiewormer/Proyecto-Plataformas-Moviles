package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.NoteCorrectionInfo
import cr.ac.una.glifo.domain.model.NoteFormulaInfo
import cr.ac.una.glifo.domain.model.NoteProvenanceFactor
import cr.ac.una.glifo.domain.model.NoteProvenanceInfo
import cr.ac.una.glifo.domain.model.NoteProvenanceStep

/**
 * Same shape as [cr.ac.una.glifo.domain.model.NoteContent] minus the resolved `note` field
 * (kept as a bare [noteId] here — [cr.ac.una.glifo.data.repository.NoteRepositoryImpl]
 * resolves it via [DummyNoteProvider] and assembles the domain model). The nested pieces
 * (formula/correction/provenance) have no DTO counterpart, so they're already domain types.
 */
data class DummyNoteContentEntry(
    val noteId: Long,
    val pageLabel: String,
    val heading: String,
    val bodyBeforeHighlight: String,
    val highlighted: String,
    val bodyAfterHighlight: String,
    val verifiedCount: Int,
    val repairedCount: Int,
    val escalatedCount: Int,
    val uncertainCount: Int,
    val formula: NoteFormulaInfo,
    val secondParagraphBefore: String,
    val secondParagraphHighlighted: String,
    val uncertainFragmentLabel: String,
    val correction: NoteCorrectionInfo,
    val provenance: NoteProvenanceInfo
)

object DummyNoteContentProvider {

    private val defaultCorrection = NoteCorrectionInfo(
        regionIndex = 12,
        regionTotal = 18,
        confidence = 0.41f,
        threshold = 0.70f,
        recognizedPrefix = "converge; en caso contrario ",
        recognizedWord = "dvierge",
        suggestions = listOf("diverge", "divergente", "deriva")
    )

    private val defaultProvenance = NoteProvenanceInfo(
        confidence = 0.41f,
        threshold = 0.70f,
        factors = listOf(
            NoteProvenanceFactor("Confianza del OCR", 0.38f, isStrong = false),
            NoteProvenanceFactor("Coincide con el glosario", 0f, isStrong = false),
            NoteProvenanceFactor("Palabra del diccionario", 0f, isStrong = false),
            NoteProvenanceFactor("Coherencia con región vecina", 0.72f, isStrong = true)
        ),
        pipeline = listOf(
            NoteProvenanceStep("done", "N0 · preprocesada", "0.02 s"),
            NoteProvenanceStep("done", "N1 · ML Kit", "0.06 s"),
            NoteProvenanceStep("skipped", "N1.5 · no es fórmula", "omitido"),
            NoteProvenanceStep("escalated", "N2 · visión, en lote", "1.4 s")
        )
    )

    private val entries = listOf(
        DummyNoteContentEntry(
            noteId = 1L,
            pageLabel = "Clase del 12 de agosto · página 1 de 3",
            heading = "Integrales impropias de primera especie",
            bodyBeforeHighlight = "Una integral es impropia cuando el intervalo de integración no está acotado, o cuando la función presenta una ",
            highlighted = "discontinuidad infinita",
            bodyAfterHighlight = " dentro del intervalo.",
            verifiedCount = 14,
            repairedCount = 2,
            escalatedCount = 1,
            uncertainCount = 1,
            formula = NoteFormulaInfo(
                regionLabel = "región 7",
                display = "∫_a^∞ f(x) dx = lim_{b→∞} ∫_a^b f(x) dx",
                latex = "\\int_a^{\\infty} f(x)\\,dx = \\lim_{b \\to \\infty} \\int_a^b f(x)\\,dx",
                confidence = 0.86f,
                tier = "N1.5",
                engine = "SimpleTex · gratuito"
            ),
            secondParagraphBefore = "Si el límite existe y es finito, la integral ",
            secondParagraphHighlighted = "converge; en caso contrario diverge",
            uncertainFragmentLabel = "1 fragmento sin leer con certeza",
            correction = defaultCorrection,
            provenance = defaultProvenance
        ),
        DummyNoteContentEntry(
            noteId = 2L,
            pageLabel = "Clase del 7 de agosto · página 2 de 2",
            heading = "Criterio de comparación para series de términos positivos",
            bodyBeforeHighlight = "Si 0 ≤ aₙ ≤ bₙ para todo n a partir de cierto índice y ∑bₙ converge, entonces ",
            highlighted = "∑aₙ también converge",
            bodyAfterHighlight = ". Si ∑aₙ diverge, entonces ∑bₙ también diverge.",
            verifiedCount = 9,
            repairedCount = 1,
            escalatedCount = 0,
            uncertainCount = 2,
            formula = NoteFormulaInfo(
                regionLabel = "región 3",
                display = "0 ≤ aₙ ≤ bₙ ⟹ ∑aₙ ≤ ∑bₙ",
                latex = "0 \\le a_n \\le b_n \\implies \\sum a_n \\le \\sum b_n",
                confidence = 0.91f,
                tier = "N1",
                engine = "ML Kit · en el dispositivo"
            ),
            secondParagraphBefore = "El criterio no dice nada cuando ",
            secondParagraphHighlighted = "la desigualdad no se cumple desde el inicio",
            uncertainFragmentLabel = "2 fragmentos sin leer con certeza",
            correction = defaultCorrection.copy(
                regionIndex = 5,
                regionTotal = 9,
                recognizedPrefix = "para todo n a partir de cierto ",
                recognizedWord = "indce",
                suggestions = listOf("índice", "indicio", "índices")
            ),
            provenance = defaultProvenance.copy(confidence = 0.52f)
        ),
        DummyNoteContentEntry(
            noteId = 3L,
            pageLabel = "Clase del 2 de agosto · página 1 de 4",
            heading = "Series de potencias y radio de convergencia",
            bodyBeforeHighlight = "Una serie de potencias centrada en a converge en un intervalo determinado por su ",
            highlighted = "radio de convergencia R",
            bodyAfterHighlight = ", que puede calcularse con el criterio del cociente.",
            verifiedCount = 11,
            repairedCount = 3,
            escalatedCount = 2,
            uncertainCount = 0,
            formula = NoteFormulaInfo(
                regionLabel = "región 9",
                display = "R = 1 / lim sup |aₙ|^(1/n)",
                latex = "R = \\dfrac{1}{\\limsup_{n\\to\\infty} |a_n|^{1/n}}",
                confidence = 0.79f,
                tier = "N1.5",
                engine = "SimpleTex · gratuito"
            ),
            secondParagraphBefore = "En los extremos del intervalo, ",
            secondParagraphHighlighted = "la convergencia debe revisarse aparte",
            uncertainFragmentLabel = "Sin fragmentos pendientes",
            correction = defaultCorrection.copy(
                regionIndex = 9,
                regionTotal = 14,
                confidence = 0.79f,
                recognizedPrefix = "el radio de ",
                recognizedWord = "convergncia",
                suggestions = listOf("convergencia", "convergente", "convergen")
            ),
            provenance = defaultProvenance.copy(confidence = 0.79f)
        ),
        DummyNoteContentEntry(
            noteId = 4L,
            pageLabel = "Clase del 28 de julio · página 1 de 2",
            heading = "Criterio de la integral",
            bodyBeforeHighlight = "Si f es una función positiva, continua y decreciente en [1, ∞), entonces ∑f(n) converge si y solo si ",
            highlighted = "la integral impropia converge",
            bodyAfterHighlight = ".",
            verifiedCount = 13,
            repairedCount = 0,
            escalatedCount = 0,
            uncertainCount = 0,
            formula = NoteFormulaInfo(
                regionLabel = "región 2",
                display = "∑ f(n) converge ⟺ ∫₁^∞ f(x) dx converge",
                latex = "\\sum f(n) \\text{ converge} \\iff \\int_1^{\\infty} f(x)\\,dx \\text{ converge}",
                confidence = 0.95f,
                tier = "N1",
                engine = "ML Kit · en el dispositivo"
            ),
            secondParagraphBefore = "Este criterio es útil cuando ",
            secondParagraphHighlighted = "la serie no tiene una forma cerrada evidente",
            uncertainFragmentLabel = "Sin fragmentos pendientes",
            correction = defaultCorrection.copy(regionIndex = 2, regionTotal = 6, confidence = 0.95f),
            provenance = defaultProvenance.copy(confidence = 0.95f)
        )
    )

    fun get(noteId: Long): DummyNoteContentEntry = entries.find { it.noteId == noteId } ?: entries.first()
}

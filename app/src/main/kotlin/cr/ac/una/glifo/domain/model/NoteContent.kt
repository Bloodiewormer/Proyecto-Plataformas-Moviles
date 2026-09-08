package cr.ac.una.glifo.domain.model

// Backs NoteDetail / CorrectFragment / FormulaViewer / RegionProvenance — the "reading a
// captured note" cluster. All four screens key off the same noteId.

data class NoteFormulaInfo(
    val regionLabel: String,
    val display: String,
    val latex: String,
    val confidence: Float,
    val tier: String,
    val engine: String
)

data class NoteCorrectionInfo(
    val regionIndex: Int,
    val regionTotal: Int,
    val confidence: Float,
    val threshold: Float,
    val recognizedPrefix: String,
    val recognizedWord: String,
    val suggestions: List<String>
)

data class NoteProvenanceFactor(val label: String, val value: Float, val isStrong: Boolean)
data class NoteProvenanceStep(val state: String, val label: String, val duration: String)

data class NoteProvenanceInfo(
    val confidence: Float,
    val threshold: Float,
    val factors: List<NoteProvenanceFactor>,
    val pipeline: List<NoteProvenanceStep>
)

data class NoteContent(
    val note: Note,
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

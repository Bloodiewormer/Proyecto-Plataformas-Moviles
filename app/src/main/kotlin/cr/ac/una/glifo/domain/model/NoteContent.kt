package cr.ac.una.glifo.domain.model

// Backs NoteDetail / CorrectFragment / FormulaViewer / RegionProvenance — the "reading a
// captured note" cluster. All four screens key off the same noteId.

/** De dónde salió el contenido que se muestra. */
enum class NoteContentSource {
    /** Resultado del pipeline local (N0 + N1 + clasificador), guardado en Room. */
    LocalPipeline,
    /** Reconstrucción IA-01 del servidor (notes.content, §5.5). */
    ServerReconstruction,
    /** El apunte existe pero todavía no tiene páginas procesadas en este teléfono. */
    Unavailable
}

/**
 * Un fragmento legible de la nota: una región del pipeline o un bloque de la reconstrucción.
 * [id] es estable entre lecturas ("p0-r3" = página 0, región 3) y es lo que referencian las
 * correcciones.
 */
data class NoteFragment(
    val id: String,
    val pageIndex: Int,
    val text: String,
    val kind: RegionKind,
    val state: ConfidenceState,
    /** Confianza de la lectura (0..1). */
    val confidence: Float,
    /** Destino al que el enrutador mandó la región ("N1.5 · OCR matemático", …). */
    val route: String,
    /** Rectángulo en la imagen preprocesada; null en contenido del servidor. */
    val bounds: RegionBounds? = null,
    val kindConfidence: Float? = null,
    val metrics: RegionInkMetrics? = null,
    /** Texto que puso el estudiante al corregir; reemplaza a [text] al mostrarse. */
    val correctedText: String? = null
) {
    val displayText: String get() = correctedText ?: text
    val isCorrected: Boolean get() = correctedText != null
}

/** Página del apunte con lo necesario para mostrar el recorte original de cada región. */
data class NotePageInfo(
    val pageIndex: Int,
    /** Foto en el almacenamiento de la app; null si la página vino del servidor. */
    val photoPath: String?,
    /** Tamaño de la imagen sobre la que se midieron los [NoteFragment.bounds]. */
    val analyzedWidth: Int,
    val analyzedHeight: Int,
    val levelReached: String,
    val overallConfidence: Float
)

data class NoteContent(
    val note: Note,
    val source: NoteContentSource,
    val pages: List<NotePageInfo>,
    val fragments: List<NoteFragment>,
    /** El apunte o alguna de sus páginas todavía está en la cola de sincronización. */
    val hasPendingSync: Boolean,
    /** Umbral de la compuerta de N1: bajo este valor la región escala. */
    val threshold: Float,
    /** Estado del apunte (DRAFT · PROCESSING · READY · ARCHIVED). */
    val status: String = "READY",
    /** Identificador en el servidor; null mientras no esté sincronizado. */
    val serverId: Long? = null
) {
    /** La nota está en PROCESSING y ya fue sincronizada con el servidor (#42). */
    val canReconstruct: Boolean
        get() = status == "PROCESSING" && serverId != null

    val verifiedCount: Int get() = fragments.count { it.state == ConfidenceState.Verified }
    val repairedCount: Int get() = fragments.count { it.state == ConfidenceState.Repaired }
    val escalatedCount: Int get() = fragments.count { it.state == ConfidenceState.Escalated }
    val uncertainCount: Int get() = fragments.count { it.state == ConfidenceState.Uncertain }

    fun fragment(id: String?): NoteFragment? = fragments.firstOrNull { it.id == id }

    fun page(pageIndex: Int): NotePageInfo? = pages.firstOrNull { it.pageIndex == pageIndex }

    /** La primera región clasificada como fórmula (la que ve FormulaViewer por defecto). */
    val firstFormula: NoteFragment? get() = fragments.firstOrNull { it.kind == RegionKind.Math }

    /**
     * El fragmento de texto más dudoso que el estudiante todavía no corrigió: es el que
     * CorrectFragment propone por defecto. Null si todo quedó sobre el umbral.
     */
    val weakestFragment: NoteFragment?
        get() = fragments
            .filter { !it.isCorrected && it.text.isNotBlank() && it.confidence < threshold }
            .minByOrNull { it.confidence }
}

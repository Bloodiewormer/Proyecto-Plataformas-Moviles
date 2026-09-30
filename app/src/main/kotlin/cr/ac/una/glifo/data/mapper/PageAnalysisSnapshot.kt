package cr.ac.una.glifo.data.mapper

import com.google.gson.Gson
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RecognizedRegion
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind

/**
 * Forma en que un [PageAnalysis] se guarda en Room (issue #28). Existe porque
 * [ConfidenceState] es una sealed class de objetos y Gson no sabe reconstruirla: aquí el
 * estado viaja como texto y se traduce de vuelta al leer.
 */
private data class PageAnalysisSnapshot(
    val regions: List<RegionSnapshot>,
    val pageWidth: Int,
    val pageHeight: Int,
    val ocrMillis: Long,
    val totalMillis: Long
)

private data class RegionSnapshot(
    val index: Int,
    val bounds: RegionBounds,
    val text: String,
    val lineCount: Int,
    val ocrConfidence: Float,
    val kind: RegionKind,
    val kindConfidence: Float,
    val route: String,
    val confidenceState: String,
    val metrics: RegionInkMetrics
)

private val gson = Gson()

fun PageAnalysis.toSnapshotJson(): String = gson.toJson(
    PageAnalysisSnapshot(
        regions = regions.map {
            RegionSnapshot(
                index = it.index,
                bounds = it.bounds,
                text = it.text,
                lineCount = it.lineCount,
                ocrConfidence = it.ocrConfidence,
                kind = it.kind,
                kindConfidence = it.kindConfidence,
                route = it.route,
                confidenceState = it.confidenceState.name(),
                metrics = it.metrics
            )
        },
        pageWidth = pageWidth,
        pageHeight = pageHeight,
        ocrMillis = ocrMillis,
        totalMillis = totalMillis
    )
)

/** null si el JSON guardado ya no se puede leer (por ejemplo, tras cambiar el modelo). */
fun pageAnalysisFromSnapshotJson(json: String): PageAnalysis? = runCatching {
    val snapshot = gson.fromJson(json, PageAnalysisSnapshot::class.java)
    PageAnalysis(
        regions = snapshot.regions.map {
            RecognizedRegion(
                index = it.index,
                bounds = it.bounds,
                text = it.text,
                lineCount = it.lineCount,
                ocrConfidence = it.ocrConfidence,
                kind = it.kind,
                kindConfidence = it.kindConfidence,
                route = it.route,
                confidenceState = confidenceStateOf(it.confidenceState),
                metrics = it.metrics
            )
        },
        pageWidth = snapshot.pageWidth,
        pageHeight = snapshot.pageHeight,
        ocrMillis = snapshot.ocrMillis,
        totalMillis = snapshot.totalMillis
    )
}.getOrNull()

private fun ConfidenceState.name(): String = when (this) {
    ConfidenceState.Verified -> "VERIFIED"
    ConfidenceState.Repaired -> "REPAIRED"
    ConfidenceState.Escalated -> "ESCALATED"
    ConfidenceState.Uncertain -> "UNCERTAIN"
}

private fun confidenceStateOf(name: String): ConfidenceState = when (name) {
    "VERIFIED" -> ConfidenceState.Verified
    "REPAIRED" -> ConfidenceState.Repaired
    "ESCALATED" -> ConfidenceState.Escalated
    else -> ConfidenceState.Uncertain
}

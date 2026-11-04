package cr.ac.una.glifo.data.mapper

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.data.dto.CreateNotePageRequest
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RegionKind

/**
 * Nivel que resolvió la página en el teléfono. Todo lo local termina en N1; lo que el
 * enrutador mandó a N1.5 o N2 queda pendiente de esos niveles (Lab 6), no resuelto.
 */
const val LOCAL_LEVEL_REACHED = "N1"

/** Promedio de confianza de las regiones de texto: es lo que va en note_pages.overall_confidence. */
fun PageAnalysis.overallConfidence(): Float = averageConfidence.coerceIn(0f, 1f)

/** Construye el cuerpo de `POST notes/{id}/pages` con el formato de note_pages.regions (§5.6). */
fun NotePageEntity.toUploadRequest(): CreateNotePageRequest? {
    val analysis = pageAnalysisFromSnapshotJson(analysisJson) ?: return null
    val width = analysis.pageWidth.coerceAtLeast(1).toFloat()
    val height = analysis.pageHeight.coerceAtLeast(1).toFloat()

    val regions = JsonArray()
    analysis.regions.forEach { region ->
        regions.add(JsonObject().apply {
            addProperty("id", "r${region.index}")
            add("bbox", JsonArray().apply {
                add(region.bounds.left / width)
                add(region.bounds.top / height)
                add(region.bounds.right / width)
                add(region.bounds.bottom / height)
            })
            addProperty("kind", region.kind.apiName())
            val defaultResolvedAt = if (region.kind == RegionKind.Text && region.confidenceState != ConfidenceState.Uncertain) "N1" else "UNRESOLVED"
            addProperty("resolvedAt", region.resolvedAt ?: defaultResolvedAt)
            addProperty("confidence", region.ocrConfidence)
            addProperty("isUncertain", region.confidenceState == ConfidenceState.Uncertain)
            addProperty("route", region.route)
            addProperty("text", region.text)
            region.latex?.let { addProperty("latex", it) }
            region.latexCompiles?.let { addProperty("latexCompiles", it) }
        })
    }

    return CreateNotePageRequest(
        pageIndex = pageIndex,
        perceptualHash = perceptualHash,
        levelReached = levelReached,
        overallConfidence = overallConfidence,
        regions = regions
    )
}

private fun RegionKind.apiName(): String = when (this) {
    RegionKind.Text -> "TEXT"
    RegionKind.Math -> "MATH"
    RegionKind.Drawing -> "DRAWING"
}

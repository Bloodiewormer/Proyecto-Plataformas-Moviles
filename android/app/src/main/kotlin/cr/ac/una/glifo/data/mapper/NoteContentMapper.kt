package cr.ac.una.glifo.data.mapper

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiText
import cr.ac.una.glifo.core.pipeline.RegionClassifier
import cr.ac.una.glifo.data.datasource.local.entity.FragmentCorrectionEntity
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.datasource.local.entity.NotePageEntity
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.NoteContentSource
import cr.ac.una.glifo.domain.model.NoteFragment
import cr.ac.una.glifo.domain.model.NotePageInfo
import cr.ac.una.glifo.domain.model.RegionKind
import cr.ac.una.glifo.domain.model.routeRes

fun NoteEntity.toDomain(syllabusTopicId: Long? = null): Note = Note(
    id = id,
    courseId = courseId,
    title = title,
    classDate = classDate,
    syllabusTopicId = syllabusTopicId
)

/**
 * Contenido de un apunte capturado en este teléfono: una fila por región que devolvió el
 * pipeline, en orden de lectura, con las correcciones del estudiante aplicadas encima.
 */
fun buildLocalNoteContent(
    note: NoteEntity,
    pages: List<NotePageEntity>,
    corrections: List<FragmentCorrectionEntity>,
    hasPendingSync: Boolean,
    syllabusTopicId: Long? = null
): NoteContent {
    val corrected = corrections.associate { it.fragmentId to it.correctedText }
    val pageInfos = mutableListOf<NotePageInfo>()
    val fragments = mutableListOf<NoteFragment>()

    pages.sortedBy { it.pageIndex }.forEach { page ->
        val analysis = pageAnalysisFromSnapshotJson(page.analysisJson)
        pageInfos += NotePageInfo(
            pageIndex = page.pageIndex,
            photoPath = page.photoPath,
            analyzedWidth = analysis?.pageWidth ?: 0,
            analyzedHeight = analysis?.pageHeight ?: 0,
            levelReached = page.levelReached,
            overallConfidence = page.overallConfidence
        )
        analysis?.regions?.forEach { region ->
            val id = "p${page.pageIndex}-r${region.index}"
            fragments += NoteFragment(
                id = id,
                pageIndex = page.pageIndex,
                text = region.text,
                kind = region.kind,
                state = if (id in corrected) ConfidenceState.Verified else region.confidenceState,
                confidence = region.ocrConfidence,
                route = region.route,
                bounds = region.bounds,
                kindConfidence = region.kindConfidence,
                metrics = region.metrics,
                correctedText = corrected[id],
                latex = region.latex,
                latexCompiles = region.latexCompiles,
                resolvedAt = region.resolvedAt,
                routeText = UiText.Resource(region.routeRes(RegionClassifier.TEXT_CONFIDENCE_GATE))
            )
        }
    }

    return NoteContent(
        note = note.toDomain(syllabusTopicId),
        source = if (pages.isEmpty()) NoteContentSource.Unavailable else NoteContentSource.LocalPipeline,
        pages = pageInfos,
        fragments = fragments,
        hasPendingSync = hasPendingSync,
        threshold = RegionClassifier.TEXT_CONFIDENCE_GATE,
        status = note.status,
        serverId = note.serverId
    )
}

/**
 * Contenido reconstruido con IA-01 desde notes.content (Glifo_UML_Modeling.md §5.5).
 * Sin reconstrucción, el apunte queda sin contenido en vez de mostrar texto inventado.
 */
fun buildServerNoteContent(
    note: NoteEntity,
    corrections: List<FragmentCorrectionEntity>,
    hasPendingSync: Boolean,
    pages: List<NotePageEntity> = emptyList(),
    syllabusTopicId: Long? = null
): NoteContent {
    val corrected = corrections.associate { it.fragmentId to it.correctedText }
    val fragments = note.contentJson?.let { parseReconstruction(it, corrected) }.orEmpty()
    val suggestedTopicId = parseSuggestedTopicId(note.contentJson)
    val pageInfos = pages.sortedBy { it.pageIndex }.map { page ->
        val analysis = pageAnalysisFromSnapshotJson(page.analysisJson)
        NotePageInfo(
            pageIndex = page.pageIndex,
            photoPath = page.photoPath,
            analyzedWidth = analysis?.pageWidth ?: 0,
            analyzedHeight = analysis?.pageHeight ?: 0,
            levelReached = page.levelReached,
            overallConfidence = page.overallConfidence
        )
    }
    return NoteContent(
        note = note.toDomain(syllabusTopicId),
        source = if (fragments.isEmpty()) NoteContentSource.Unavailable else NoteContentSource.ServerReconstruction,
        pages = pageInfos,
        fragments = fragments,
        hasPendingSync = hasPendingSync,
        threshold = RegionClassifier.TEXT_CONFIDENCE_GATE,
        status = note.status,
        serverId = note.serverId,
        suggestedTopicId = suggestedTopicId
    )
}

internal fun parseSuggestedTopicId(json: String?): Long? {
    if (json.isNullOrBlank()) return null
    val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
    val raw = root.get("suggestedTopicId")?.takeIf { !it.isJsonNull }
        ?: root.get("suggested_topic_id")?.takeIf { !it.isJsonNull }
        ?: return null
    val parsed = runCatching {
        val primitive = raw.asJsonPrimitive
        when {
            primitive.isNumber -> primitive.asLong
            primitive.isString -> primitive.asString.trim().toLongOrNull()
            else -> null
        }
    }.getOrNull()
    return parsed?.takeIf { it > 0L }
}

/** Recorre `blocks` del documento IA-01; un bloque PARAGRAPH aporta un fragmento por span. */
internal fun parseReconstruction(json: String, corrected: Map<String, String> = emptyMap()): List<NoteFragment> {
    val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return emptyList()
    val blocks = root.getAsJsonArray("blocks") ?: return emptyList()
    val fragments = mutableListOf<NoteFragment>()

    blocks.forEach { element ->
        val block = element.asJsonObject
        val blockId = block.string("id") ?: "b${fragments.size}"
        when (block.string("type")) {
            "HEADING" -> fragments += serverFragment(
                id = blockId, text = block.string("text").orEmpty(), kind = RegionKind.Text,
                resolvedAt = "N1", confidence = 1f, uncertain = false, corrected = corrected
            )
            "PARAGRAPH" -> block.getAsJsonArray("spans")?.forEachIndexed { i, spanElement ->
                val span = spanElement.asJsonObject
                fragments += serverFragment(
                    id = "$blockId-s$i",
                    text = span.string("text").orEmpty(),
                    kind = RegionKind.Text,
                    resolvedAt = span.string("resolvedAt") ?: "N1",
                    confidence = span.float("confidence") ?: 0f,
                    uncertain = span.get("isUncertain")?.asBoolean ?: false,
                    corrected = corrected
                )
            }
            "FORMULA" -> {
                val latexText = block.string("latex").orEmpty()
                val resolvedAt = block.string("resolvedAt") ?: "N1_5"
                val uncertain = block.get("isUncertain")?.asBoolean ?: false
                val compiles = block.get("latexCompiles")?.takeIf { !it.isJsonNull }?.asBoolean
                    ?: (!uncertain && resolvedAt != "UNRESOLVED")
                fragments += serverFragment(
                    id = blockId,
                    text = latexText,
                    kind = RegionKind.Math,
                    resolvedAt = resolvedAt,
                    confidence = block.float("confidence") ?: 0f,
                    uncertain = uncertain,
                    corrected = corrected,
                    latex = latexText.takeIf { it.isNotBlank() },
                    latexCompiles = if (latexText.isNotBlank()) compiles else null
                )
            }
            "FIGURE" -> fragments += serverFragment(
                id = blockId, text = block.string("caption").orEmpty(), kind = RegionKind.Drawing,
                resolvedAt = block.string("resolvedAt") ?: "N2",
                confidence = block.float("confidence") ?: 0f, uncertain = false, corrected = corrected
            )
        }
    }
    return fragments
}

private fun serverFragment(
    id: String,
    text: String,
    kind: RegionKind,
    resolvedAt: String,
    confidence: Float,
    uncertain: Boolean,
    corrected: Map<String, String>,
    latex: String? = null,
    latexCompiles: Boolean? = null
): NoteFragment {
    // resolvedAt dice qué nivel resolvió el fragmento; se traduce a la escala de la UI (§1).
    val state = when {
        id in corrected -> ConfidenceState.Verified
        uncertain || resolvedAt == "UNRESOLVED" -> ConfidenceState.Uncertain
        resolvedAt == "N1_5" -> ConfidenceState.Repaired
        resolvedAt == "N2" || resolvedAt == "N3" -> ConfidenceState.Escalated
        else -> ConfidenceState.Verified
    }
    return NoteFragment(
        id = id,
        pageIndex = 0,
        text = text,
        kind = kind,
        state = state,
        confidence = confidence,
        route = "$resolvedAt · reconstrucción del servidor",
        correctedText = corrected[id],
        latex = latex,
        latexCompiles = latexCompiles,
        resolvedAt = resolvedAt,
        routeText = UiText.Resource(R.string.note_fragment_route_server, resolvedAt)
    )
}

private fun JsonObject.string(name: String): String? = get(name)?.takeIf { !it.isJsonNull }?.asString
private fun JsonObject.float(name: String): Float? = get(name)?.takeIf { !it.isJsonNull }?.asFloat

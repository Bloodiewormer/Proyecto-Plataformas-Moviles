package cr.ac.una.glifo.ai.validation

import cr.ac.una.glifo.note.entity.NotePage
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

private val jsonMapper = JsonMapper.builder().findAndAddModules().build()
private val log = LoggerFactory.getLogger("cr.ac.una.glifo.ai.validation.MathRegion")

/** `kind` con el que la app sube las regiones matemáticas (PageUploadMapper.apiName). */
private const val MATH_KIND = "MATH"

/**
 * Región `kind=MATH` de una página tal como la subió Android a note_pages.regions (§5.6).
 * No confundir con un bloque `type=FORMULA` de la reconstrucción IA-01 (notes.content).
 *
 * Se identifica por página + [regionId]: la app numera las regiones por página ("r0",
 * "r1"…), así que el mismo id se repite en páginas distintas.
 */
data class MathRegion(
    val noteId: Long,
    val pageId: Long,
    val pageIndex: Int,
    val regionId: String,
    /** bbox normalizado 0..1 `[left, top, right, bottom]`, sin tocar; vacío si no vino. */
    val bbox: List<Double>,
    /** Texto del OCR local del teléfono (ML Kit). NO es LaTeX y no se reemplaza. */
    val ocrText: String?
)

/** Regiones MATH de la página en el orden en que las guardó la app. */
fun NotePage.mathRegions(): List<MathRegion> {
    val json = regions ?: return emptyList()
    val root = jsonMapper.readTree(json)
    if (!root.isArray) return emptyList()

    return root.mapNotNull { node ->
        if (node.stringField("kind") != MATH_KIND) return@mapNotNull null
        val regionId = node.stringField("id")
        if (regionId.isNullOrBlank()) {
            log.warn("Región MATH sin id en la página {} del apunte {}; se omite", pageIndex, note.id)
            return@mapNotNull null
        }
        MathRegion(
            noteId = note.id,
            pageId = id,
            pageIndex = pageIndex,
            regionId = regionId,
            bbox = node.get("bbox")?.takeIf { it.isArray }?.filter { it.isNumber }?.map { it.asDouble() }.orEmpty(),
            ocrText = node.stringField("text")
        )
    }
}

private fun JsonNode.stringField(name: String): String? =
    get(name)?.takeIf { it.isString }?.asString()

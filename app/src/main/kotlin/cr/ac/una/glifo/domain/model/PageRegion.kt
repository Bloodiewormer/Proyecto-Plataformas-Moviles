package cr.ac.una.glifo.domain.model

import androidx.annotation.StringRes
import cr.ac.una.glifo.R

/**
 * Resultado del enrutador de regiones (Issue #17). Cada fragmento de la página cae en
 * exactamente uno de estos tipos y eso decide a qué nivel del pipeline se manda:
 * texto se queda en N1, matemática escala a N1.5 y dibujo escala a N2.
 */
enum class RegionKind(val label: String, @StringRes val labelRes: Int) {
    Text("Texto", R.string.region_kind_text),
    Math("Matemática", R.string.region_kind_math),
    Drawing("Dibujo", R.string.region_kind_drawing)
}

/** Rectángulo de una región, en píxeles de la imagen ya preprocesada por N0. */
data class RegionBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
) {
    val right: Int get() = left + width
    val bottom: Int get() = top + height
    val area: Int get() = width * height
}

/**
 * Métricas puramente visuales que produce la segmentación (OpenCV) antes de que exista
 * cualquier texto. El clasificador las combina con la salida de N1 para decidir el tipo.
 */
data class RegionInkMetrics(
    val inkDensity: Float,
    val componentCount: Int,
    val aspectRatio: Float,
    val horizontalBarCount: Int = 0,
    val tallStrokeCount: Int = 0,
    val maxStrokeMinSpanRatio: Float = 0f,
    val heightRatio: Float = 0f,
    val areaRatio: Float = 0f
)

/** Una región de la página después de N1 (OCR local) y del clasificador. */
data class RecognizedRegion(
    val index: Int,
    val bounds: RegionBounds,
    val text: String,
    val lineCount: Int,
    /** Confianza del OCR local (0..1). */
    val ocrConfidence: Float,
    val kind: RegionKind,
    /** Qué tan segura está la clasificación del tipo de región (0..1). */
    val kindConfidence: Float,
    /** Destino al que el enrutador manda esta región ("N1.5 · OCR matemático", …). */
    val route: String,
    val confidenceState: ConfidenceState,
    val metrics: RegionInkMetrics,
    /** Ruta opcional a un recorte guardado; si es null se recorta desde la foto de la página usando [bounds]. */
    val cropPath: String? = null,
    /** LaTeX generado por N1.5 (#38) o N2 (#41) para regiones matemáticas. */
    val latex: String? = null,
    /** Si el LaTeX compila según el validador JLaTeXMath del servidor (#39 / #40). */
    val latexCompiles: Boolean? = null,
    /** Nivel que resolvió la región ("N1", "N1_5", "N2", "UNRESOLVED"). */
    val resolvedAt: String? = null
)

@StringRes
fun RecognizedRegion.routeRes(textConfidenceGate: Float = 0.75f): Int = when {
    resolvedAt == "N2" || kind == RegionKind.Drawing -> R.string.region_route_n2
    resolvedAt == "N1_5" || kind == RegionKind.Math -> R.string.region_route_n1_5
    ocrConfidence < textConfidenceGate -> R.string.region_route_n2_below_gate
    else -> R.string.region_route_n1_verified
}

/** De qué página ya procesada salió un análisis reutilizado, y qué tan parecidas eran. */
data class PageReuse(
    val perceptualHash: String,
    /** Bits distintos entre los dos hashes (de 64). */
    val hammingDistance: Int,
    val processedAt: Long
)

/** Salida completa del pipeline local para una captura. */
data class PageAnalysis(
    val regions: List<RecognizedRegion>,
    val pageWidth: Int,
    val pageHeight: Int,
    /** Milisegundos que tardó N1 (solo el OCR, para poder mostrarlo en la UI). */
    val ocrMillis: Long,
    /** Milisegundos del pipeline completo (N0 + segmentación + N1 + clasificador). */
    val totalMillis: Long,
    /** No es null cuando el resultado se reutilizó de una página ya procesada (issue #28). */
    val reusedFrom: PageReuse? = null
) {
    val regionCount: Int get() = regions.size
    val textCount: Int get() = regions.count { it.kind == RegionKind.Text }
    val mathCount: Int get() = regions.count { it.kind == RegionKind.Math }
    val drawingCount: Int get() = regions.count { it.kind == RegionKind.Drawing }
    val readCount: Int get() = regions.count { it.text.isNotBlank() }
    val averageConfidence: Float
        get() = regions.filter { it.kind == RegionKind.Text }
            .map { it.ocrConfidence }
            .let { if (it.isEmpty()) 0f else it.average().toFloat() }
    val plainText: String get() = regions.filter { it.text.isNotBlank() }.joinToString("\n\n") { it.text }
}

package cr.ac.una.glifo.domain.model

/**
 * Resultado del enrutador de regiones (Issue #17). Cada fragmento de la página cae en
 * exactamente uno de estos tipos y eso decide a qué nivel del pipeline se manda:
 * texto se queda en N1, matemática escala a N1.5 y dibujo escala a N2.
 */
enum class RegionKind(val label: String) {
    Text("Texto"),
    Math("Matemática"),
    Drawing("Dibujo")
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
    /** Proporción de píxeles con tinta dentro del rectángulo (0..1). */
    val inkDensity: Float,
    /** Cantidad de componentes conectados (proxy de "cuántos glifos/trazos hay"). */
    val componentCount: Int,
    /** ancho / alto del rectángulo. */
    val aspectRatio: Float
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
    val metrics: RegionInkMetrics
)

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

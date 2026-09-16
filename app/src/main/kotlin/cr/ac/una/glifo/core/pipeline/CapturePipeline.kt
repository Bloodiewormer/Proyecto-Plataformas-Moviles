package cr.ac.una.glifo.core.pipeline

import android.graphics.Bitmap
import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.core.mlkit.TextRecognizerN1
import cr.ac.una.glifo.core.opencv.ImagePreprocessor
import cr.ac.una.glifo.core.opencv.PageSegmenter
import cr.ac.una.glifo.core.opencv.SegmentedRegion
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orquestador del tramo local del pipeline: N0 (OpenCV) -> segmentacion -> N1 (ML Kit) ->
 * clasificador enrutador. Es lo que une los Issues #16 y #17 en un solo paso que la capa de
 * datos puede invocar con un Bitmap.
 *
 * Todo lo que hace este objeto corre en el dispositivo y no cuesta nada: recien las regiones
 * que quedan enrutadas a N1.5 o N2 justifican una llamada externa.
 */
object CapturePipeline {

    /**
     * Las fotos de camara llegan a 12 MP y ni OpenCV ni ML Kit ganan nada con ese detalle;
     * bajar el lado mayor a este tamano recorta el tiempo de proceso sin perder legibilidad.
     */
    private const val MAX_WORKING_DIMENSION = 1600

    suspend fun analyze(source: Bitmap, glossary: List<String> = emptyList()): PageAnalysis = withContext(Dispatchers.Default) {
        val startedAt = System.currentTimeMillis()

        val working = PageSegmenter.scaleToMaxDimension(source, MAX_WORKING_DIMENSION)

        // N0: deskew + CLAHE. Si OpenCV no cargo, seguimos con la imagen tal cual en vez de
        // tumbar toda la captura: N1 igual puede leer una foto sin preprocesar.
        val preprocessed = runCatching { ImagePreprocessor.processPipelineN0(working) }
            .getOrDefault(working)

        if (working !== source && working !== preprocessed) working.recycle()

        val segmented = runCatching { PageSegmenter.segment(preprocessed) }
            .getOrDefault(emptyList())
            .ifEmpty { listOf(fullPage(preprocessed)) }

        // N1: OCR local region por region. Secuencial a proposito, porque el recognizer de
        // ML Kit serializa las peticiones internamente y lanzarlas en paralelo solo agrega
        // presion de memoria con los recortes.
        val ocrStartedAt = System.currentTimeMillis()
        val ocrResults = segmented.map { region ->
            try {
                TextRecognizerN1.recognizeRegion(preprocessed, region.bounds)
            } catch (cancellation: CancellationException) {
                // Salir de la pantalla cancela el scope: hay que dejar propagar en vez de
                // seguir procesando regiones que ya nadie va a mirar.
                throw cancellation
            } catch (_: Exception) {
                // Una region ilegible no invalida la pagina entera: queda sin texto y el
                // clasificador la mandara a N2.
                OcrResult.Empty
            }
        }
        val ocrMillis = System.currentTimeMillis() - ocrStartedAt

        // Clasificador: cada fragmento sale ya enrutado a su nivel.
        val regions = segmented.mapIndexed { index, region ->
            RegionClassifier.classify(
                index = index,
                bounds = region.bounds,
                metrics = region.metrics,
                ocr = ocrResults[index],
                glossary = glossary
            )
        }

        val analysis = PageAnalysis(
            regions = regions,
            pageWidth = preprocessed.width,
            pageHeight = preprocessed.height,
            ocrMillis = ocrMillis,
            totalMillis = System.currentTimeMillis() - startedAt
        )

        if (preprocessed !== source) preprocessed.recycle()

        analysis
    }

    /** Cuando la segmentacion no encuentra nada, N1 igual corre sobre la pagina completa. */
    private fun fullPage(bitmap: Bitmap): SegmentedRegion = SegmentedRegion(
        bounds = RegionBounds(0, 0, bitmap.width, bitmap.height),
        metrics = RegionInkMetrics(
            inkDensity = 0f,
            componentCount = 0,
            aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        )
    )

    /**
     * Traduce el resultado del pipeline a la linea de tiempo que dibuja la pantalla de
     * procesamiento. Los niveles que no se llegaron a ejecutar quedan en "pending" con el
     * trabajo que les toca, para que se vea por que se activan (o por que no).
     */
    fun stepsFor(analysis: PageAnalysis): List<CaptureStep> {
        val escalated = analysis.regions.count {
            it.kind == RegionKind.Drawing ||
                (it.kind == RegionKind.Text && it.ocrConfidence < RegionClassifier.TEXT_CONFIDENCE_GATE)
        }
        val seconds = String.format(Locale.US, "%.1f", analysis.ocrMillis / 1000.0)

        return listOf(
            CaptureStep(
                state = "done",
                name = "N0 - Preprocesamiento",
                description = "Perspectiva corregida - contraste normalizado - ${analysis.regionCount} regiones segmentadas"
            ),
            CaptureStep(
                state = "done",
                name = "N1 - OCR de texto",
                description = "${analysis.readCount} de ${analysis.regionCount} regiones leidas - ML Kit, en el dispositivo - $seconds s"
            ),
            CaptureStep(
                state = "done",
                name = "Clasificador de region",
                description = "${analysis.textCount} texto - ${analysis.mathCount} formulas - ${analysis.drawingCount} dibujos"
            ),
            CaptureStep(
                state = if (analysis.mathCount > 0) "now" else "pending",
                name = "N1.5 - OCR matematico",
                description = if (analysis.mathCount > 0) {
                    "${analysis.mathCount} regiones enrutadas a conversion LaTeX - capa gratuita"
                } else {
                    "No se detectaron formulas en esta pagina."
                }
            ),
            CaptureStep(
                state = if (escalated > 0) "now" else "pending",
                name = "N2 - Reparacion por vision",
                description = if (escalated > 0) {
                    "$escalated regiones no superaron la compuerta de N1."
                } else {
                    "Se activa solo si algo no supera las compuertas."
                }
            )
        )
    }
}

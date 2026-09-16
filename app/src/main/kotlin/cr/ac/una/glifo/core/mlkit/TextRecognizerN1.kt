package cr.ac.una.glifo.core.mlkit

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import cr.ac.una.glifo.domain.model.RegionBounds
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Lo que N1 devuelve por region: el texto leido, cuantas lineas, y la confianza que ML Kit
 * reporto para ellas — `null` cuando el modelo local no publico una (ver [ConfidenceScorer][
 * cr.ac.una.glifo.core.confidence.ConfidenceScorer] para el calculo de respaldo, que combina
 * este resultado con las metricas de tinta de la segmentacion y el glosario del curso).
 */
data class OcrResult(
    val text: String,
    val lineCount: Int,
    val wordCount: Int,
    val confidence: Float?
) {
    companion object {
        val Empty = OcrResult(text = "", lineCount = 0, wordCount = 0, confidence = null)
    }
}

/** Bloque de texto detectado nativamente por ML Kit en la página completa. */
data class RecognizedBlock(
    val bounds: RegionBounds,
    val ocr: OcrResult
)

/**
 * Pipeline N1: Reconocimiento optico de caracteres local con ML Kit (Issue #16).
 *
 * Todo corre en el dispositivo con el modelo Latin empaquetado en el APK: cero llamadas de
 * red, cero costo por pagina y funciona sin conexion. El recognizer es caro de construir,
 * asi que se crea una sola vez y se reutiliza para todas las regiones de la captura.
 */
object TextRecognizerN1 {

    /** Un recorte mas chico que esto no le da a ML Kit suficiente contexto para leer. */
    private const val MIN_CROP_PX = 32

    /** Margen mínimo que se le suma al recorte para no cortar ascendentes, descendentes ni tildes. */
    private const val MIN_CROP_PADDING_PX = 24

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /** Corre N1 sobre la imagen completa (se usa cuando la segmentacion no encontro nada). */
    suspend fun recognize(bitmap: Bitmap): OcrResult = process(bitmap)

    /**
     * Corre N1 sobre la imagen completa en una sola pasada de inferencia, extrayendo los
     * bloques de texto delimitados nativamente por el modelo de ML Kit con sus coordenadas
     * reales y texto enriquecido con el contexto lingüístico de la página completa.
     */
    suspend fun recognizeBlocks(bitmap: Bitmap): List<RecognizedBlock> = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (!continuation.isActive) return@addOnSuccessListener
                val blocks = visionText.textBlocks.mapNotNull { block ->
                    val bbox = block.boundingBox ?: return@mapNotNull null
                    val lines = block.lines
                    if (lines.isEmpty()) return@mapNotNull null

                    val text = lines.joinToString("\n") { it.text }.trim()
                    if (text.isBlank()) return@mapNotNull null

                    val wordCount = lines.sumOf { it.elements.size }
                    val reported = lines.mapNotNull { line ->
                        val value: Float? = line.confidence
                        if (value == null || value.isNaN() || value <= 0f) null else value
                    }

                    val left = bbox.left.coerceIn(0, bitmap.width - 1)
                    val top = bbox.top.coerceIn(0, bitmap.height - 1)
                    val width = bbox.width().coerceAtLeast(1).coerceAtMost(bitmap.width - left)
                    val height = bbox.height().coerceAtLeast(1).coerceAtMost(bitmap.height - top)

                    RecognizedBlock(
                        bounds = RegionBounds(left, top, width, height),
                        ocr = OcrResult(
                            text = text,
                            lineCount = lines.size,
                            wordCount = wordCount,
                            confidence = if (reported.isEmpty()) null else reported.average().toFloat().coerceIn(0f, 1f)
                        )
                    )
                }
                continuation.resume(blocks)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    /** Corre N1 sobre una region concreta de la pagina, recortandola antes de pasarla al modelo. */
    suspend fun recognizeRegion(page: Bitmap, bounds: RegionBounds): OcrResult {
        val crop = cropWithPadding(page, bounds) ?: return OcrResult.Empty
        val result = process(crop)
        if (crop != page) crop.recycle()
        return result
    }

    /**
     * Recorta con margen generoso y sin salirse de la pagina.
     */
    private fun cropWithPadding(page: Bitmap, bounds: RegionBounds): Bitmap? {
        val padX = maxOf(MIN_CROP_PADDING_PX, (bounds.width * 0.08f).toInt())
        val padY = maxOf(MIN_CROP_PADDING_PX, (bounds.height * 0.12f).toInt())

        val left = (bounds.left - padX).coerceIn(0, page.width - 1)
        val top = (bounds.top - padY).coerceIn(0, page.height - 1)
        val right = (bounds.right + padX).coerceIn(left + 1, page.width)
        val bottom = (bounds.bottom + padY).coerceIn(top + 1, page.height)

        val width = right - left
        val height = bottom - top
        if (width < MIN_CROP_PX || height < MIN_CROP_PX) return null

        return runCatching { Bitmap.createBitmap(page, left, top, width, height) }.getOrNull()
    }

    /** Puente entre la Task de ML Kit y las corrutinas, cancelable junto con el scope. */
    private suspend fun process(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { text ->
                if (continuation.isActive) continuation.resume(summarize(text))
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    /**
     * Aplana el arbol de ML Kit (bloques a lineas a elementos) en el texto de la region y,
     * si el modelo local la publico, su confianza. Cuando no la publica se deja en `null` —
     * calcular una de respaldo no es trabajo de este engine, es lo que hace
     * [cr.ac.una.glifo.core.confidence.ConfidenceScorer] con datos que este engine no tiene
     * (las metricas de tinta de la region, el glosario del curso).
     */
    private fun summarize(text: com.google.mlkit.vision.text.Text): OcrResult {
        val lines = text.textBlocks.flatMap { it.lines }
        if (lines.isEmpty()) return OcrResult.Empty

        val recognized = text.textBlocks.joinToString("\n") { block ->
            block.lines.joinToString("\n") { it.text }
        }.trim()

        val wordCount = lines.sumOf { it.elements.size }

        val reported = lines.mapNotNull { line ->
            val value: Float? = line.confidence
            if (value == null || value.isNaN() || value <= 0f) null else value
        }

        return OcrResult(
            text = recognized,
            lineCount = lines.size,
            wordCount = wordCount,
            confidence = if (reported.isEmpty()) null else reported.average().toFloat().coerceIn(0f, 1f)
        )
    }
}

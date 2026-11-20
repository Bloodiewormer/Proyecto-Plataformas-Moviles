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

/** Linea individual dentro de un bloque detectado por ML Kit. */
data class RecognizedLine(
    val text: String,
    val bounds: RegionBounds,
    val wordCount: Int,
    val confidence: Float?
)

/** Bloque de texto detectado nativamente por ML Kit en la página completa. */
data class RecognizedBlock(
    val bounds: RegionBounds,
    val ocr: OcrResult,
    val lines: List<RecognizedLine> = emptyList()
)

object TextRecognizerN1 {

    private const val MIN_CROP_PX = 32
    private const val MIN_CROP_PADDING_PX = 24

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognize(bitmap: Bitmap): OcrResult = process(bitmap)

    suspend fun recognizeBlocks(bitmap: Bitmap): List<RecognizedBlock> = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (!continuation.isActive) return@addOnSuccessListener
                val blocks = visionText.textBlocks.mapNotNull { block ->
                    val bbox = block.boundingBox ?: return@mapNotNull null
                    val rawLines = block.lines
                    if (rawLines.isEmpty()) return@mapNotNull null

                    val recognizedLines = rawLines.mapNotNull { line ->
                        val lineText = line.text.trim()
                        if (lineText.isBlank()) return@mapNotNull null
                        val lineBox = line.boundingBox ?: bbox
                        val lLeft = lineBox.left.coerceIn(0, bitmap.width - 1)
                        val lTop = lineBox.top.coerceIn(0, bitmap.height - 1)
                        val lWidth = lineBox.width().coerceAtLeast(1).coerceAtMost(bitmap.width - lLeft)
                        val lHeight = lineBox.height().coerceAtLeast(1).coerceAtMost(bitmap.height - lTop)
                        val rawConf: Float = line.confidence
                        val lineConf: Float? = if (rawConf.isNaN() || rawConf <= 0f) null else rawConf
                        RecognizedLine(
                            text = lineText,
                            bounds = RegionBounds(lLeft, lTop, lWidth, lHeight),
                            wordCount = line.elements.size,
                            confidence = lineConf
                        )
                    }
                    if (recognizedLines.isEmpty()) return@mapNotNull null

                    val text = recognizedLines.joinToString("\n") { it.text }.trim()
                    val wordCount = recognizedLines.sumOf { it.wordCount }
                    val reported = recognizedLines.mapNotNull { it.confidence }

                    val left = bbox.left.coerceIn(0, bitmap.width - 1)
                    val top = bbox.top.coerceIn(0, bitmap.height - 1)
                    val width = bbox.width().coerceAtLeast(1).coerceAtMost(bitmap.width - left)
                    val height = bbox.height().coerceAtLeast(1).coerceAtMost(bitmap.height - top)

                    RecognizedBlock(
                        bounds = RegionBounds(left, top, width, height),
                        ocr = OcrResult(
                            text = text,
                            lineCount = recognizedLines.size,
                            wordCount = wordCount,
                            confidence = if (reported.isEmpty()) null else reported.average().toFloat().coerceIn(0f, 1f)
                        ),
                        lines = recognizedLines
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
        // Crop tightly around the region and pad with white so adjacent lines cannot bleed into OCR.
        val srcPad = 2
        val left = (bounds.left - srcPad).coerceIn(0, page.width - 1)
        val top = (bounds.top - srcPad).coerceIn(0, page.height - 1)
        val right = (bounds.right + srcPad).coerceIn(left + 1, page.width)
        val bottom = (bounds.bottom + srcPad).coerceIn(top + 1, page.height)

        val srcWidth = right - left
        val srcHeight = bottom - top
        if (srcWidth <= 0 || srcHeight <= 0) return null

        val border = 16
        val outWidth = maxOf(srcWidth + border * 2, MIN_CROP_PX)
        val outHeight = maxOf(srcHeight + border * 2, MIN_CROP_PX)
        return runCatching {
            val padded = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(padded)
            canvas.drawColor(android.graphics.Color.WHITE)
            val destLeft = (outWidth - srcWidth) / 2f
            val destTop = (outHeight - srcHeight) / 2f
            canvas.drawBitmap(
                page,
                android.graphics.Rect(left, top, right, bottom),
                android.graphics.RectF(destLeft, destTop, destLeft + srcWidth, destTop + srcHeight),
                null
            )
            padded
        }.getOrNull()
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

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

/** Lo que N1 devuelve por region: el texto leido, cuantas lineas y que tan seguro esta. */
data class OcrResult(
    val text: String,
    val lineCount: Int,
    val wordCount: Int,
    val confidence: Float
) {
    companion object {
        val Empty = OcrResult(text = "", lineCount = 0, wordCount = 0, confidence = 0f)
    }
}

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

    /** Margen que se le suma al recorte: las tildes y los rabos de las letras suelen
     *  quedar justo fuera del rectangulo que devolvio la segmentacion. */
    private const val CROP_PADDING_PX = 8

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /** Corre N1 sobre la imagen completa (se usa cuando la segmentacion no encontro nada). */
    suspend fun recognize(bitmap: Bitmap): OcrResult = process(bitmap)

    /** Corre N1 sobre una region concreta de la pagina, recortandola antes de pasarla al modelo. */
    suspend fun recognizeRegion(page: Bitmap, bounds: RegionBounds): OcrResult {
        val crop = cropWithPadding(page, bounds) ?: return OcrResult.Empty
        val result = process(crop)
        if (crop != page) crop.recycle()
        return result
    }

    /**
     * Recorta con margen y sin salirse de la pagina. Devuelve null si la region es tan
     * chica que ML Kit la rechazaria de todas formas.
     */
    private fun cropWithPadding(page: Bitmap, bounds: RegionBounds): Bitmap? {
        val left = (bounds.left - CROP_PADDING_PX).coerceIn(0, page.width - 1)
        val top = (bounds.top - CROP_PADDING_PX).coerceIn(0, page.height - 1)
        val right = (bounds.right + CROP_PADDING_PX).coerceIn(left + 1, page.width)
        val bottom = (bounds.bottom + CROP_PADDING_PX).coerceIn(top + 1, page.height)

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
     * Aplana el arbol de ML Kit (bloques a lineas a elementos) en el texto de la region y
     * una confianza unica.
     *
     * ML Kit no siempre publica la confianza por linea del modelo local, asi que cuando no
     * viene se deriva una: texto con mas palabras y lineas consistentes es texto que el
     * modelo leyo con soltura, mientras que una region que devolvio dos caracteres sueltos
     * casi siempre es una region que el OCR no supo leer.
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

        val confidence = if (reported.isNotEmpty()) {
            reported.average().toFloat()
        } else {
            derivedConfidence(recognized, lines.size, wordCount)
        }

        return OcrResult(
            text = recognized,
            lineCount = lines.size,
            wordCount = wordCount,
            confidence = confidence.coerceIn(0f, 1f)
        )
    }

    private fun derivedConfidence(text: String, lineCount: Int, wordCount: Int): Float {
        val chars = text.count { !it.isWhitespace() }
        if (chars == 0) return 0f

        // Palabras de longitud razonable son la senal mas fuerte de una lectura sana.
        val averageWordLength = if (wordCount == 0) 0f else chars.toFloat() / wordCount
        val lengthScore = when {
            averageWordLength >= 3f -> 1f
            averageWordLength >= 2f -> 0.75f
            else -> 0.45f
        }

        // Una region con una sola linea y dos caracteres es sospechosa; varias lineas leidas
        // completas, no.
        val volumeScore = when {
            chars >= 40 || lineCount >= 3 -> 1f
            chars >= 15 -> 0.85f
            chars >= 5 -> 0.7f
            else -> 0.5f
        }

        // Los simbolos raros sueltos (|, ~, caracteres de control) delatan basura del OCR.
        val garbageRatio = text.count { !it.isLetterOrDigit() && !it.isWhitespace() }.toFloat() / chars
        val cleanScore = (1f - garbageRatio).coerceIn(0.4f, 1f)

        return (lengthScore * 0.4f + volumeScore * 0.35f + cleanScore * 0.25f).coerceIn(0f, 1f)
    }
}

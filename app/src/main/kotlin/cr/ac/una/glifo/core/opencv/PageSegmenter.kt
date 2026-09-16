package cr.ac.una.glifo.core.opencv

import android.graphics.Bitmap
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.roundToInt

/** Una region candidata: el rectangulo mas las metricas de tinta que usa el clasificador. */
data class SegmentedRegion(val bounds: RegionBounds, val metrics: RegionInkMetrics)

/**
 * Algoritmo de division de pagina (Issue #17, primera mitad).
 *
 * La idea es la clasica del analisis de layout: binarizar la tinta, engordarla con
 * morfologia hasta que las letras de una misma linea o parrafo se peguen entre si, y
 * quedarse con los contornos externos de esos blobs. Cada blob es un fragmento candidato
 * que despues pasa por N1 (OCR) y por el clasificador.
 */
object PageSegmenter {

    /** Regiones mas chicas que esto (respecto al area total) son ruido o manchas. */
    private const val MIN_AREA_RATIO = 0.0006

    /** Una region que cubre casi toda la hoja es el borde del papel, no un fragmento. */
    private const val MAX_AREA_RATIO = 0.92

    /** ML Kit ignora recortes minusculos, asi que no vale la pena emitirlos. */
    private const val MIN_SIDE_PX = 24

    /** Tope de fragmentos por pagina: mas que esto significa que la binarizacion se descontrolo. */
    private const val MAX_REGIONS = 48

    fun segment(bitmap: Bitmap): List<SegmentedRegion> {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()
        if (src.channels() >= 3) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        } else {
            src.copyTo(gray)
        }
        src.release()

        // Binarizacion adaptativa: la tinta queda en blanco (255) sobre fondo negro, y
        // aguanta que la hoja este iluminada de forma despareja. Umbral ajustado a 8.0
        // para retener trazos suaves de lápiz o bolígrafos claros.
        val binary = Mat()
        val blockSize = oddAtLeast(max(gray.rows(), gray.cols()) / 40, 15)
        Imgproc.adaptiveThreshold(
            gray,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            blockSize,
            8.0
        )
        gray.release()

        // Quita el ruido de sal y pimienta antes de dilatar, para no inflar manchas sueltas.
        val denoiseKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_OPEN, denoiseKernel)
        denoiseKernel.release()

        val width = binary.cols()
        val height = binary.rows()

        // Dos pasadas de cierre: la horizontal pega las letras en palabras y lineas cohesivas;
        // la vertical ayuda a agrupar líneas contiguas de un mismo párrafo o fórmula.
        val merged = Mat()
        val horizontal = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(max(14.0, width / 30.0), 1.0)
        )
        Imgproc.morphologyEx(binary, merged, Imgproc.MORPH_CLOSE, horizontal)
        horizontal.release()

        val vertical = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(1.0, max(5.0, height / 60.0))
        )
        Imgproc.morphologyEx(merged, merged, Imgproc.MORPH_CLOSE, vertical)
        vertical.release()

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(merged, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()
        merged.release()

        val totalArea = (width * height).toDouble()
        val rects = contours.mapNotNull { contour ->
            val rect = Imgproc.boundingRect(contour)
            contour.release()
            val ratio = (rect.width * rect.height) / totalArea
            val keep = ratio in MIN_AREA_RATIO..MAX_AREA_RATIO &&
                rect.width >= MIN_SIDE_PX &&
                rect.height >= MIN_SIDE_PX
            if (keep) rect else null
        }

        // Los solapes vienen de trazos que la morfologia no llego a unir (tildes, exponentes,
        // el palo de una fraccion). Fusionarlos evita partir una formula en tres fragmentos.
        val mergedRects = mergeOverlapping(rects)
            .sortedByDescending { it.width * it.height }
            .take(MAX_REGIONS)
            .sortedWith(compareBy({ it.y }, { it.x }))

        val regions = mergedRects.map { rect ->
            SegmentedRegion(
                bounds = RegionBounds(rect.x, rect.y, rect.width, rect.height),
                metrics = measure(binary, rect)
            )
        }

        binary.release()
        return regions
    }

    /**
     * Mide la region sobre la mascara binaria sin dilatar: densidad de tinta real y cantidad
     * de componentes conectados, que es lo que distingue un parrafo (muchos componentes
     * chicos) de un diagrama (pocos trazos largos).
     */
    private fun measure(binary: Mat, rect: Rect): RegionInkMetrics {
        val roi = Mat(binary, rect)
        val inkPixels = Core.countNonZero(roi)
        val area = (rect.width * rect.height).coerceAtLeast(1)

        val innerContours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(roi, innerContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()

        // Los componentes de 1 o 2 pixeles son ruido residual, no glifos.
        var components = 0
        innerContours.forEach { contour ->
            val r = Imgproc.boundingRect(contour)
            if (r.width > 2 && r.height > 2) components++
            contour.release()
        }
        roi.release()

        return RegionInkMetrics(
            inkDensity = (inkPixels.toFloat() / area).coerceIn(0f, 1f),
            componentCount = components,
            aspectRatio = rect.width.toFloat() / rect.height.toFloat()
        )
    }

    /** Fusiona repetidamente los rectangulos que se solapan hasta que ninguno se toca. */
    private fun mergeOverlapping(input: List<Rect>): List<Rect> {
        val pending = input.toMutableList()
        var changed = true
        while (changed) {
            changed = false
            outer@ for (i in pending.indices) {
                for (j in i + 1 until pending.size) {
                    if (intersects(pending[i], pending[j])) {
                        val union = union(pending[i], pending[j])
                        pending.removeAt(j)
                        pending[i] = union
                        changed = true
                        break@outer
                    }
                }
            }
        }
        return pending
    }

    private fun intersects(a: Rect, b: Rect): Boolean {
        // Permitir un margen horizontal de proximidad para fusionar palabras cercanas en la misma línea
        val hPad = 12
        return a.x < b.x + b.width + hPad && b.x < a.x + a.width + hPad &&
            a.y < b.y + b.height && b.y < a.y + a.height
    }

    /**
     * Mide métricas de tinta para una región específica sobre el bitmap de la página.
     */
    fun measureBounds(bitmap: Bitmap, bounds: RegionBounds): RegionInkMetrics {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val gray = Mat()
        if (src.channels() >= 3) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        } else {
            src.copyTo(gray)
        }
        src.release()

        val binary = Mat()
        val blockSize = oddAtLeast(max(gray.rows(), gray.cols()) / 40, 15)
        Imgproc.adaptiveThreshold(
            gray,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            blockSize,
            8.0
        )
        gray.release()

        val rect = Rect(
            bounds.left.coerceIn(0, binary.cols() - 1),
            bounds.top.coerceIn(0, binary.rows() - 1),
            bounds.width.coerceAtLeast(1).coerceAtMost(binary.cols() - bounds.left),
            bounds.height.coerceAtLeast(1).coerceAtMost(binary.rows() - bounds.top)
        )
        val metrics = measure(binary, rect)
        binary.release()
        return metrics
    }

    private fun union(a: Rect, b: Rect): Rect {
        val left = minOf(a.x, b.x)
        val top = minOf(a.y, b.y)
        val right = maxOf(a.x + a.width, b.x + b.width)
        val bottom = maxOf(a.y + a.height, b.y + b.height)
        return Rect(left, top, right - left, bottom - top)
    }

    /** adaptiveThreshold exige un blockSize impar y mayor o igual a 3. */
    private fun oddAtLeast(value: Int, minimum: Int): Int {
        val v = max(value, minimum)
        return if (v % 2 == 0) v + 1 else v
    }

    /**
     * Helper de escalado usado por el pipeline:
     * - Reduce fotos de alta resolución (12 MP) a un tamaño de trabajo óptimo (1600 px).
     * - Si la imagen es una miniatura de muy baja resolución (< 1000 px, ej. 374x534), la escala hacia
     *   arriba para que los caracteres individuales tengan al menos 16-20 píxeles de altura,
     *   permitiendo que los extractores convolucionales de ML Kit detecten trazos en vez de manchas.
     */
    fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int = 1600, minDimension: Int = 1000): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest in minDimension..maxDimension) return bitmap

        val factor = if (largest > maxDimension) {
            maxDimension.toDouble() / largest
        } else {
            minDimension.toDouble() / largest
        }

        val targetWidth = (bitmap.width * factor).roundToInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * factor).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}

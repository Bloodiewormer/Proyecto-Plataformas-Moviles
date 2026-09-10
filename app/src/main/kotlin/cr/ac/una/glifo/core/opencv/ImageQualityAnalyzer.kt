package cr.ac.una.glifo.core.opencv

import android.graphics.Bitmap
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc

/**
 * Analizador de calidad de imagen basado en OpenCV.
 * Resuelve el Issue #10: Diagnóstico de Calidad de Captura con Motivo de Rechazo.
 */
object ImageQualityAnalyzer {

    // Umbrales empíricos
    private const val BLUR_THRESHOLD = 100.0 // Varianza Laplaciana (menor = más borroso)
    private const val DARK_THRESHOLD = 60.0  // Media de brillo (menor = muy oscuro)
    private const val BRIGHT_THRESHOLD = 210.0 // Media de brillo (mayor = muy brillante)

    /**
     * Analiza un Bitmap y devuelve una lista de chequeos de calidad.
     * Si alguna prueba falla (isWarning = true), el UI debería pedir reintento explícito.
     */
    fun analyzeQuality(bitmap: Bitmap): List<CaptureQualityCheck> {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()
        if (src.channels() >= 3) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            src.copyTo(gray)
        }

        // 1. Prueba de Borrosidad (Varianza Laplaciana)
        val blurCheck = checkBlurriness(gray)

        // 2. Prueba de Iluminación (Media de píxeles)
        val illuminationCheck = checkIllumination(gray)

        // Liberar memoria
        src.release()
        gray.release()

        return listOf(blurCheck, illuminationCheck)
    }

    /**
     * Usa la varianza del Laplaciano para detectar qué tan enfocada está la imagen.
     * Un borde bien definido tiene alta varianza. Una imagen borrosa tiene baja varianza.
     */
    private fun checkBlurriness(grayMat: Mat): CaptureQualityCheck {
        val laplacian = Mat()
        Imgproc.Laplacian(grayMat, laplacian, org.opencv.core.CvType.CV_64F)

        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stdDev)

        // Varianza = Desviación estándar al cuadrado
        val variance = Math.pow(stdDev.toArray()[0], 2.0)
        
        laplacian.release()
        mean.release()
        stdDev.release()

        val isBlurry = variance < BLUR_THRESHOLD
        
        // Convertimos la varianza a un porcentaje "entendible" por la UI (topamos en 500 para el 100%)
        val scorePercent = (variance / 500.0).coerceIn(0.0, 1.0).toFloat()

        return CaptureQualityCheck(
            label = "Enfoque y Nitidez",
            scoreLabel = if (isBlurry) "Imagen borrosa" else "Enfoque óptimo",
            percent = scorePercent,
            isWarning = isBlurry
        )
    }

    /**
     * Calcula el brillo promedio para detectar fotos subexpuestas (oscuras) o sobreexpuestas (brillantes).
     */
    private fun checkIllumination(grayMat: Mat): CaptureQualityCheck {
        val mean = Core.mean(grayMat).`val`[0]
        
        val isTooDark = mean < DARK_THRESHOLD
        val isTooBright = mean > BRIGHT_THRESHOLD
        val isWarning = isTooDark || isTooBright

        val scoreLabel = when {
            isTooDark -> "Muy oscuro. Falta iluminación."
            isTooBright -> "Sobreexpuesto. Exceso de luz."
            else -> "Iluminación correcta"
        }

        // Convertimos a porcentaje ideal (128 es el centro perfecto)
        val distanceToIdeal = Math.abs(128.0 - mean)
        val scorePercent = (1.0 - (distanceToIdeal / 128.0)).coerceIn(0.0, 1.0).toFloat()

        return CaptureQualityCheck(
            label = "Iluminación",
            scoreLabel = scoreLabel,
            percent = scorePercent,
            isWarning = isWarning
        )
    }
}

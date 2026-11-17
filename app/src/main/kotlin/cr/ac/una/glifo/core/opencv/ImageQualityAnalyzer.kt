package cr.ac.una.glifo.core.opencv

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import cr.ac.una.glifo.R
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

    internal data class QualityCheckSpec(
        @StringRes val labelRes: Int,
        @StringRes val scoreLabelRes: Int,
        val percent: Float,
        val isWarning: Boolean
    )

    // Umbrales empíricos
    private const val BLUR_THRESHOLD = 100.0 // Varianza Laplaciana (menor = más borroso)
    private const val DARK_THRESHOLD = 60.0  // Media de brillo (menor = muy oscuro)
    private const val BRIGHT_THRESHOLD = 210.0 // Media de brillo (mayor = muy brillante)

    /**
     * Analiza un Bitmap y devuelve una lista de chequeos de calidad.
     * Si alguna prueba falla (isWarning = true), el UI debería pedir reintento explícito.
     */
    fun analyzeQuality(bitmap: Bitmap, context: Context): List<CaptureQualityCheck> {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()
        if (src.channels() >= 3) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            src.copyTo(gray)
        }

        val blurCheck = checkBlurriness(gray, context)
        val illuminationCheck = checkIllumination(gray, context)

        src.release()
        gray.release()

        return listOf(blurCheck, illuminationCheck)
    }

    internal fun blurCheckSpec(variance: Double): QualityCheckSpec {
        val isBlurry = variance < BLUR_THRESHOLD
        val scorePercent = (variance / 500.0).coerceIn(0.0, 1.0).toFloat()
        return QualityCheckSpec(
            labelRes = R.string.capture_quality_focus_label,
            scoreLabelRes = if (isBlurry) R.string.capture_quality_focus_blurry else R.string.capture_quality_focus_optimal,
            percent = scorePercent,
            isWarning = isBlurry
        )
    }

    internal fun illuminationCheckSpec(meanBrightness: Double): QualityCheckSpec {
        val isTooDark = meanBrightness < DARK_THRESHOLD
        val isTooBright = meanBrightness > BRIGHT_THRESHOLD
        val isWarning = isTooDark || isTooBright
        val scoreLabelRes = when {
            isTooDark -> R.string.capture_quality_light_dark
            isTooBright -> R.string.capture_quality_light_bright
            else -> R.string.capture_quality_light_optimal
        }
        val distanceToIdeal = Math.abs(128.0 - meanBrightness)
        val scorePercent = (1.0 - (distanceToIdeal / 128.0)).coerceIn(0.0, 1.0).toFloat()
        return QualityCheckSpec(
            labelRes = R.string.capture_quality_light_label,
            scoreLabelRes = scoreLabelRes,
            percent = scorePercent,
            isWarning = isWarning
        )
    }

    internal fun QualityCheckSpec.toDomain(context: Context): CaptureQualityCheck = CaptureQualityCheck(
        label = context.getString(labelRes),
        scoreLabel = context.getString(scoreLabelRes),
        percent = percent,
        isWarning = isWarning
    )

    private fun checkBlurriness(grayMat: Mat, context: Context): CaptureQualityCheck {
        val laplacian = Mat()
        Imgproc.Laplacian(grayMat, laplacian, org.opencv.core.CvType.CV_64F)

        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stdDev)

        val variance = Math.pow(stdDev.toArray()[0], 2.0)

        laplacian.release()
        mean.release()
        stdDev.release()

        return blurCheckSpec(variance).toDomain(context)
    }

    private fun checkIllumination(grayMat: Mat, context: Context): CaptureQualityCheck {
        val mean = Core.mean(grayMat).`val`[0]
        return illuminationCheckSpec(mean).toDomain(context)
    }
}

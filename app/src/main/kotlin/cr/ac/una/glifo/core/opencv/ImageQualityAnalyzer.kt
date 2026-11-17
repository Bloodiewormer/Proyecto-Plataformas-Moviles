package cr.ac.una.glifo.core.opencv

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import cr.ac.una.glifo.R
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureQualityIssue
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

object ImageQualityAnalyzer {

    internal data class QualityCheckSpec(
        @StringRes val labelRes: Int,
        @StringRes val scoreLabelRes: Int,
        val percent: Float,
        val isWarning: Boolean,
        val issue: CaptureQualityIssue? = null
    )

    internal data class IlluminationStats(
        val meanBrightness: Double,
        val p1: Double,
        val p95: Double,
        val clippedWhiteRatio: Double,
        val clippedBlackRatio: Double
    ) {
        val contrastRange: Double get() = (p95 - p1).coerceAtLeast(0.0)
    }

    private const val BLUR_THRESHOLD = 100.0
    private const val NORMALIZED_BLUR_MAX_DIM = 1000
    private const val HIGHLIGHT_CLIP_LEVEL = 252
    private const val SHADOW_CLIP_LEVEL = 30
    private const val SEVERE_CLIP_RATIO = 0.40
    private const val EXTREME_CLIP_RATIO = 0.85
    private const val MIN_CONTRAST_WITH_CLIP = 80.0
    private const val MIN_CONTRAST_WASHED_OUT = 45.0
    private const val WASHED_OUT_HIGH_MEAN = 200.0
    private const val WASHED_OUT_LOW_MEAN = 75.0
    private const val DARK_MEAN_CEILING = 55.0
    private const val DARK_P95_CEILING = 110.0
    private const val BRIGHT_P1_FLOOR = 180.0

    fun analyzeQuality(bitmap: Bitmap, context: Context): List<CaptureQualityCheck> {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()
        if (src.channels() >= 3) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
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
            isWarning = isBlurry,
            issue = if (isBlurry) CaptureQualityIssue.Blurry else null
        )
    }

    internal fun illuminationCheckSpec(stats: IlluminationStats): QualityCheckSpec {
        val contrast = stats.contrastRange
        // White paper with dark ink legitimately has mean > 210; reject only when highlights/shadows clip or ink contrast collapses.
        val isOverexposed =
            (stats.clippedWhiteRatio >= SEVERE_CLIP_RATIO && contrast < MIN_CONTRAST_WITH_CLIP) ||
                (stats.clippedWhiteRatio >= EXTREME_CLIP_RATIO && stats.p1 >= BRIGHT_P1_FLOOR) ||
                (stats.meanBrightness >= WASHED_OUT_HIGH_MEAN && contrast < MIN_CONTRAST_WASHED_OUT)

        val isUnderexposed =
            (stats.clippedBlackRatio >= SEVERE_CLIP_RATIO && contrast < MIN_CONTRAST_WITH_CLIP) ||
                (stats.meanBrightness <= WASHED_OUT_LOW_MEAN && contrast < MIN_CONTRAST_WASHED_OUT) ||
                (stats.meanBrightness < DARK_MEAN_CEILING && stats.p95 < DARK_P95_CEILING)

        val isWarning = isOverexposed || isUnderexposed
        val scoreLabelRes = when {
            isUnderexposed -> R.string.capture_quality_light_dark
            isOverexposed -> R.string.capture_quality_light_bright
            else -> R.string.capture_quality_light_optimal
        }

        val contrastScore = (contrast / 140.0).coerceIn(0.0, 1.0)
        val dominantClip = maxOf(stats.clippedWhiteRatio, stats.clippedBlackRatio)
        val clipPenalty = ((dominantClip - 0.15) / 0.70).coerceIn(0.0, 1.0)
        val scorePercent = if (isWarning) {
            (contrastScore * (1.0 - clipPenalty) * 0.5).coerceIn(0.0, 0.45).toFloat()
        } else {
            (contrastScore * (1.0 - 0.35 * clipPenalty)).coerceIn(0.5, 1.0).toFloat()
        }

        return QualityCheckSpec(
            labelRes = R.string.capture_quality_light_label,
            scoreLabelRes = scoreLabelRes,
            percent = scorePercent,
            isWarning = isWarning,
            issue = when {
                isUnderexposed -> CaptureQualityIssue.Underexposed
                isOverexposed -> CaptureQualityIssue.Overexposed
                else -> null
            }
        )
    }

    internal fun computeIlluminationStats(histogram: IntArray, totalPixels: Int): IlluminationStats {
        if (totalPixels <= 0) {
            return IlluminationStats(0.0, 0.0, 0.0, 0.0, 1.0)
        }
        var sum = 0L
        var clippedWhite = 0L
        var clippedBlack = 0L
        val p1Target = (totalPixels * 0.01).roundToInt().coerceAtLeast(1)
        val p95Target = (totalPixels * 0.95).roundToInt().coerceAtLeast(1)
        var cumulative = 0L
        var p1 = -1.0
        var p95 = -1.0

        for (level in 0..255) {
            val count = histogram[level]
            if (count <= 0) continue
            sum += level.toLong() * count
            if (level >= HIGHLIGHT_CLIP_LEVEL) clippedWhite += count
            if (level <= SHADOW_CLIP_LEVEL) clippedBlack += count
            cumulative += count
            if (p1 < 0.0 && cumulative >= p1Target) p1 = level.toDouble()
            if (p95 < 0.0 && cumulative >= p95Target) p95 = level.toDouble()
        }

        return IlluminationStats(
            meanBrightness = sum.toDouble() / totalPixels,
            p1 = if (p1 < 0.0) 0.0 else p1,
            p95 = if (p95 < 0.0) 255.0 else p95,
            clippedWhiteRatio = clippedWhite.toDouble() / totalPixels,
            clippedBlackRatio = clippedBlack.toDouble() / totalPixels
        )
    }

    internal fun normalizedTargetSize(width: Int, height: Int, targetMaxDim: Int = NORMALIZED_BLUR_MAX_DIM): Pair<Int, Int>? {
        val largest = max(width, height)
        if (largest <= 0 || largest == targetMaxDim) return null
        val scale = targetMaxDim.toDouble() / largest
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }

    internal fun QualityCheckSpec.toDomain(context: Context): CaptureQualityCheck = CaptureQualityCheck(
        label = context.getString(labelRes),
        scoreLabel = context.getString(scoreLabelRes),
        percent = percent,
        isWarning = isWarning,
        issue = issue
    )

    private fun checkBlurriness(grayMat: Mat, context: Context): CaptureQualityCheck {
        // Normalize resolution before Laplacian so threshold is consistent between thumbnails and 12MP captures.
        val targetSize = normalizedTargetSize(grayMat.cols(), grayMat.rows())
        val normalized = if (targetSize != null) {
            val resized = Mat()
            val interp = if (max(grayMat.cols(), grayMat.rows()) > NORMALIZED_BLUR_MAX_DIM) {
                Imgproc.INTER_AREA
            } else {
                Imgproc.INTER_LINEAR
            }
            Imgproc.resize(grayMat, resized, Size(targetSize.first.toDouble(), targetSize.second.toDouble()), 0.0, 0.0, interp)
            resized
        } else {
            grayMat
        }

        val laplacian = Mat()
        Imgproc.Laplacian(normalized, laplacian, CvType.CV_64F)

        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stdDev)

        val variance = stdDev.toArray()[0].pow(2.0)

        if (normalized !== grayMat) normalized.release()
        laplacian.release()
        mean.release()
        stdDev.release()

        return blurCheckSpec(variance).toDomain(context)
    }

    private fun checkIllumination(grayMat: Mat, context: Context): CaptureQualityCheck {
        val totalPixels = (grayMat.rows() * grayMat.cols()).coerceAtLeast(0)
        if (totalPixels == 0) {
            return illuminationCheckSpec(IlluminationStats(0.0, 0.0, 0.0, 0.0, 1.0)).toDomain(context)
        }
        val pixels = ByteArray(totalPixels)
        grayMat.get(0, 0, pixels)
        val histogram = IntArray(256)
        for (b in pixels) {
            histogram[b.toInt() and 0xFF]++
        }
        val stats = computeIlluminationStats(histogram, totalPixels)
        return illuminationCheckSpec(stats).toDomain(context)
    }
}

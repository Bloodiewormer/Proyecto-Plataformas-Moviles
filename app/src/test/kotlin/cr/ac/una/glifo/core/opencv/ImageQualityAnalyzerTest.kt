package cr.ac.una.glifo.core.opencv

import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.opencv.ImageQualityAnalyzer.IlluminationStats
import cr.ac.una.glifo.domain.model.CaptureQualityIssue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageQualityAnalyzerTest {

    @Test
    fun `blurCheckSpec returns warning and blurry string res below threshold`() {
        val spec = ImageQualityAnalyzer.blurCheckSpec(variance = 45.0)
        assertEquals(R.string.capture_quality_focus_label, spec.labelRes)
        assertEquals(R.string.capture_quality_focus_blurry, spec.scoreLabelRes)
        assertTrue(spec.isWarning)
    }

    @Test
    fun `blurCheckSpec returns optimal string res at or above threshold`() {
        val spec = ImageQualityAnalyzer.blurCheckSpec(variance = 250.0)
        assertEquals(R.string.capture_quality_focus_label, spec.labelRes)
        assertEquals(R.string.capture_quality_focus_optimal, spec.scoreLabelRes)
        assertFalse(spec.isWarning)
        assertEquals(0.5f, spec.percent, 0.001f)
    }

    @Test
    fun `normalizedTargetSize scales large and small images proportionally and skips already normalized`() {
        assertNull(ImageQualityAnalyzer.normalizedTargetSize(750, 1000))
        assertEquals(750 to 1000, ImageQualityAnalyzer.normalizedTargetSize(3000, 4000))
        assertEquals(679 to 1000, ImageQualityAnalyzer.normalizedTargetSize(456, 672))
    }

    @Test
    fun `illuminationCheckSpec accepts bright white paper with strong ink contrast`() {
        val whitePaper = IlluminationStats(
            meanBrightness = 221.6,
            p1 = 59.0,
            p95 = 251.0,
            clippedWhiteRatio = 0.04,
            clippedBlackRatio = 0.003
        )
        val spec = ImageQualityAnalyzer.illuminationCheckSpec(whitePaper)
        assertEquals(R.string.capture_quality_light_label, spec.labelRes)
        assertEquals(R.string.capture_quality_light_optimal, spec.scoreLabelRes)
        assertFalse(spec.isWarning)
        assertNull(spec.issue)
        assertTrue(spec.percent > 0.85f)
    }

    @Test
    fun `illuminationCheckSpec rejects clipped white or washed out overexposed captures`() {
        val clippedOverexposed = IlluminationStats(
            meanBrightness = 254.0,
            p1 = 255.0,
            p95 = 255.0,
            clippedWhiteRatio = 0.99,
            clippedBlackRatio = 0.0
        )
        val clippedSpec = ImageQualityAnalyzer.illuminationCheckSpec(clippedOverexposed)
        assertEquals(R.string.capture_quality_light_bright, clippedSpec.scoreLabelRes)
        assertTrue(clippedSpec.isWarning)
        assertEquals(CaptureQualityIssue.Overexposed, clippedSpec.issue)

        val washedOut = IlluminationStats(
            meanBrightness = 225.0,
            p1 = 195.0,
            p95 = 235.0,
            clippedWhiteRatio = 0.05,
            clippedBlackRatio = 0.0
        )
        val washedSpec = ImageQualityAnalyzer.illuminationCheckSpec(washedOut)
        assertTrue(washedSpec.isWarning)
        assertEquals(CaptureQualityIssue.Overexposed, washedSpec.issue)
    }

    @Test
    fun `illuminationCheckSpec rejects crushed shadows and dim low-contrast captures`() {
        val underexposed = IlluminationStats(
            meanBrightness = 12.0,
            p1 = 12.0,
            p95 = 12.0,
            clippedWhiteRatio = 0.0,
            clippedBlackRatio = 0.99
        )
        val darkSpec = ImageQualityAnalyzer.illuminationCheckSpec(underexposed)
        assertEquals(R.string.capture_quality_light_dark, darkSpec.scoreLabelRes)
        assertTrue(darkSpec.isWarning)
        assertEquals(CaptureQualityIssue.Underexposed, darkSpec.issue)

        val dimMuddy = IlluminationStats(
            meanBrightness = 48.0,
            p1 = 20.0,
            p95 = 95.0,
            clippedWhiteRatio = 0.15,
            clippedBlackRatio = 0.0
        )
        val muddySpec = ImageQualityAnalyzer.illuminationCheckSpec(dimMuddy)
        assertTrue(muddySpec.isWarning)
        assertEquals(CaptureQualityIssue.Underexposed, muddySpec.issue)
    }

    @Test
    fun `computeIlluminationStats derives percentiles, mean, and clipping fractions accurately`() {
        val histogram = IntArray(256)
        histogram[50] = 50
        histogram[230] = 890
        histogram[254] = 60

        val stats = ImageQualityAnalyzer.computeIlluminationStats(histogram, 1000)
        assertEquals(50.0, stats.p1, 0.01)
        assertEquals(254.0, stats.p95, 0.01)
        assertEquals(0.06, stats.clippedWhiteRatio, 0.001)
        assertEquals(0.0, stats.clippedBlackRatio, 0.001)
        assertEquals(222.4, stats.meanBrightness, 0.1)
    }
}

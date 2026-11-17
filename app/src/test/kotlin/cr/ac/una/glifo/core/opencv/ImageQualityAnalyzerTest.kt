package cr.ac.una.glifo.core.opencv

import cr.ac.una.glifo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `illuminationCheckSpec handles dark, bright, and balanced brightness`() {
        val dark = ImageQualityAnalyzer.illuminationCheckSpec(meanBrightness = 30.0)
        assertEquals(R.string.capture_quality_light_label, dark.labelRes)
        assertEquals(R.string.capture_quality_light_dark, dark.scoreLabelRes)
        assertTrue(dark.isWarning)

        val bright = ImageQualityAnalyzer.illuminationCheckSpec(meanBrightness = 230.0)
        assertEquals(R.string.capture_quality_light_bright, bright.scoreLabelRes)
        assertTrue(bright.isWarning)

        val optimal = ImageQualityAnalyzer.illuminationCheckSpec(meanBrightness = 128.0)
        assertEquals(R.string.capture_quality_light_optimal, optimal.scoreLabelRes)
        assertFalse(optimal.isWarning)
        assertEquals(1.0f, optimal.percent, 0.001f)
    }
}

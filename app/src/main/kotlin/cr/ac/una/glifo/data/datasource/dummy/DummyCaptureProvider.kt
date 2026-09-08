package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep

/**
 * Stand-in for the real `pipeline/` output (N0 quality gate + escalation ladder) so the
 * capture screens have something to render before that package is built. Replace with
 * calls into `pipeline.PipelineEngine` once it exists — see Glifo_Arquitectura_Estandares.md §2.
 */
object DummyCaptureProvider {
    fun getQualityChecks(): List<CaptureQualityCheck> = listOf(
        CaptureQualityCheck("Nitidez", "12 / 35", 24f, isWarning = true),
        CaptureQualityCheck("Iluminación", "78", 78f, isWarning = false),
        CaptureQualityCheck("Sin reflejos", "91", 91f, isWarning = false),
        CaptureQualityCheck("Perspectiva", "corregida −4.2°", 100f, isWarning = false)
    )

    fun hasQualityWarning(): Boolean = getQualityChecks().any { it.isWarning }

    fun getProcessingSteps(): List<CaptureStep> = listOf(
        CaptureStep("done", "N0 · Preprocesamiento", "Perspectiva −4.2° · contraste normalizado · 18 regiones segmentadas"),
        CaptureStep("done", "N1 · OCR de texto", "18 de 18 regiones leídas · ML Kit, en el dispositivo · 1.1 s"),
        CaptureStep("done", "Clasificador de región", "14 texto · 3 fórmulas · 1 dibujo"),
        CaptureStep("now", "N1.5 · OCR matemático", "2 de 3 fórmulas convertidas a LaTeX · SimpleTex · capa gratuita"),
        CaptureStep("pending", "N2 · Reparación por visión", "Se activa solo si algo no supera las compuertas.")
    )
}

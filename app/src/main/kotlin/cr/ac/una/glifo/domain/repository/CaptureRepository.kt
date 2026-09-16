package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.PageAnalysis

interface CaptureRepository {
    suspend fun getQualityChecks(photoPath: String? = null): Result<List<CaptureQualityCheck>>
    suspend fun hasQualityWarning(photoPath: String? = null): Result<Boolean>

    /**
     * Corre el tramo local del pipeline sobre la foto: N0, segmentacion, N1 (OCR con ML Kit)
     * y el clasificador que enruta cada region. Sin foto no hay nada que analizar.
     */
    suspend fun analyzePage(photoPath: String?): Result<PageAnalysis>

    /** Linea de tiempo del procesamiento, derivada del analisis real cuando hay foto. */
    suspend fun getProcessingSteps(photoPath: String? = null): Result<List<CaptureStep>>
}

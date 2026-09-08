package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep

interface CaptureRepository {
    suspend fun getQualityChecks(): Result<List<CaptureQualityCheck>>
    suspend fun hasQualityWarning(): Result<Boolean>
    suspend fun getProcessingSteps(): Result<List<CaptureStep>>
}

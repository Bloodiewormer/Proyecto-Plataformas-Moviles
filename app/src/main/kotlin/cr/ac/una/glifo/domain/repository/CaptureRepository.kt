package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep

interface CaptureRepository {
    suspend fun getQualityChecks(photoPath: String? = null): Result<List<CaptureQualityCheck>>
    suspend fun hasQualityWarning(photoPath: String? = null): Result<Boolean>
    suspend fun getProcessingSteps(): Result<List<CaptureStep>>
}

package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.dummy.DummyCaptureProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.repository.CaptureRepository
import javax.inject.Inject

class CaptureRepositoryImpl @Inject constructor() : CaptureRepository {
    override suspend fun getQualityChecks(): Result<List<CaptureQualityCheck>> =
        Result.Success(DummyCaptureProvider.getQualityChecks())

    override suspend fun hasQualityWarning(): Result<Boolean> =
        Result.Success(DummyCaptureProvider.hasQualityWarning())

    override suspend fun getProcessingSteps(): Result<List<CaptureStep>> =
        Result.Success(DummyCaptureProvider.getProcessingSteps())
}

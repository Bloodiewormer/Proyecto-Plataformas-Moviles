package cr.ac.una.glifo.data.repository

import android.graphics.BitmapFactory
import cr.ac.una.glifo.core.opencv.ImageQualityAnalyzer
import cr.ac.una.glifo.data.datasource.dummy.DummyCaptureProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.repository.CaptureRepository
import javax.inject.Inject

class CaptureRepositoryImpl @Inject constructor() : CaptureRepository {
    override suspend fun getQualityChecks(photoPath: String?): Result<List<CaptureQualityCheck>> {
        if (photoPath != null) {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            if (bitmap != null) {
                return Result.Success(ImageQualityAnalyzer.analyzeQuality(bitmap))
            }
        }
        return Result.Success(DummyCaptureProvider.getQualityChecks())
    }

    override suspend fun hasQualityWarning(photoPath: String?): Result<Boolean> {
        if (photoPath != null) {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            if (bitmap != null) {
                return Result.Success(ImageQualityAnalyzer.analyzeQuality(bitmap).any { it.isWarning })
            }
        }
        return Result.Success(DummyCaptureProvider.hasQualityWarning())
    }

    override suspend fun getProcessingSteps(): Result<List<CaptureStep>> =
        Result.Success(DummyCaptureProvider.getProcessingSteps())
}

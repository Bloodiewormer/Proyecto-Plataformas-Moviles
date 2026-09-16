package cr.ac.una.glifo.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.opencv.ImageQualityAnalyzer
import cr.ac.una.glifo.core.pipeline.CapturePipeline
import cr.ac.una.glifo.data.datasource.dummy.DummyCaptureProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.repository.CaptureRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureRepositoryImpl @Inject constructor() : CaptureRepository {

    // La pantalla de procesamiento pide el analisis y los pasos por separado, y volver a
    // correr N0+N1 sobre la misma foto cuesta segundos. Se guarda el ultimo resultado y el
    // mutex evita que dos llamadas concurrentes arranquen el pipeline dos veces.
    private val pipelineLock = Mutex()
    private var lastAnalyzedPath: String? = null
    private var lastAnalysis: PageAnalysis? = null

    override suspend fun getQualityChecks(photoPath: String?): Result<List<CaptureQualityCheck>> {
        val bitmap = decode(photoPath)
        if (bitmap != null) {
            return Result.Success(ImageQualityAnalyzer.analyzeQuality(bitmap))
        }
        return Result.Success(DummyCaptureProvider.getQualityChecks())
    }

    override suspend fun hasQualityWarning(photoPath: String?): Result<Boolean> {
        val bitmap = decode(photoPath)
        if (bitmap != null) {
            return Result.Success(ImageQualityAnalyzer.analyzeQuality(bitmap).any { it.isWarning })
        }
        return Result.Success(DummyCaptureProvider.hasQualityWarning())
    }

    override suspend fun analyzePage(photoPath: String?): Result<PageAnalysis> {
        if (photoPath == null) {
            return Result.Failure(AppError.Validation("photo", "No hay ninguna foto para analizar."))
        }

        return pipelineLock.withLock {
            lastAnalysis?.takeIf { lastAnalyzedPath == photoPath }?.let { return@withLock Result.Success(it) }

            val bitmap = decode(photoPath)
                ?: return@withLock Result.Failure(
                    AppError.Validation("photo", "No se pudo leer la foto capturada.")
                )

            try {
                val analysis = CapturePipeline.analyze(bitmap)
                lastAnalyzedPath = photoPath
                lastAnalysis = analysis
                Result.Success(analysis)
            } catch (cancellation: CancellationException) {
                // Cancelar la pantalla no es un fallo del pipeline: se deja propagar.
                throw cancellation
            } catch (error: Exception) {
                Result.Failure(
                    AppError.Server(
                        code = "pipeline_local",
                        message = error.message ?: "El procesamiento local falló."
                    )
                )
            }
        }
    }

    override suspend fun getProcessingSteps(photoPath: String?): Result<List<CaptureStep>> {
        if (photoPath == null) {
            return Result.Success(DummyCaptureProvider.getProcessingSteps())
        }
        return when (val analysis = analyzePage(photoPath)) {
            is Result.Success -> Result.Success(CapturePipeline.stepsFor(analysis.value))
            is Result.Failure -> Result.Failure(analysis.error)
        }
    }

    private suspend fun decode(photoPath: String?): Bitmap? {
        if (photoPath == null) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeFile(
                    photoPath,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                )
            }.getOrNull()
        }
    }
}

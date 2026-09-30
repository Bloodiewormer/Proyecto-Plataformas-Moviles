package cr.ac.una.glifo.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.opencv.ImageQualityAnalyzer
import cr.ac.una.glifo.core.pipeline.CapturePipeline
import cr.ac.una.glifo.core.pipeline.PerceptualHasher
import cr.ac.una.glifo.data.datasource.dummy.DummyCaptureProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.LocalCourseContext
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.repository.CaptureRepository
import cr.ac.una.glifo.domain.repository.CourseContextRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureRepositoryImpl @Inject constructor(
    private val courseContextRepository: CourseContextRepository,
    private val pageDeduplicator: PageDeduplicator
) : CaptureRepository {

    // La pantalla de procesamiento pide el analisis y los pasos por separado, y volver a
    // correr N0+N1 sobre la misma foto cuesta segundos. Se guarda el ultimo resultado y el
    // mutex evita que dos llamadas concurrentes arranquen el pipeline dos veces.
    private val pipelineLock = Mutex()
    private var lastKey: Pair<String, Long?>? = null
    private var lastAnalysis: PageAnalysis? = null
    private var lastContext: LocalCourseContext? = null

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

    override suspend fun analyzePage(photoPath: String?, courseId: Long?): Result<PageAnalysis> {
        if (photoPath == null) {
            return Result.Failure(AppError.Validation("photo", "No hay ninguna foto para analizar."))
        }

        return pipelineLock.withLock {
            val key = photoPath to courseId
            lastAnalysis?.takeIf { lastKey == key }?.let { return@withLock Result.Success(it) }

            val bitmap = decode(photoPath)
                ?: return@withLock Result.Failure(
                    AppError.Validation("photo", "No se pudo leer la foto capturada.")
                )

            try {
                // Solo Room, sin red: el contexto se descarga al abrir la camara, asi que aqui
                // el pipeline funciona igual con o sin conexion (#27).
                val context = courseId?.let { courseContextRepository.getCached(it) }

                // #28: una pagina ya procesada para este curso no vuelve a pasar por N0 + N1.
                val hash = if (courseId != null) {
                    runCatching { PerceptualHasher.hash(bitmap) }.getOrNull()
                } else {
                    null
                }
                val reused = if (courseId != null && hash != null) {
                    pageDeduplicator.findSamePage(courseId, hash)
                } else {
                    null
                }

                val analysis = reused ?: CapturePipeline.analyze(bitmap, context?.glossaryTerms.orEmpty())
                    .also { fresh ->
                        if (courseId != null && hash != null) {
                            pageDeduplicator.remember(courseId, hash, photoPath, fresh)
                        }
                    }

                lastKey = key
                lastAnalysis = analysis
                lastContext = context
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

    override suspend fun getProcessingSteps(photoPath: String?, courseId: Long?): Result<List<CaptureStep>> {
        if (photoPath == null) {
            return Result.Success(DummyCaptureProvider.getProcessingSteps())
        }
        return when (val analysis = analyzePage(photoPath, courseId)) {
            is Result.Success -> Result.Success(stepsWithOfflineContext(analysis.value, courseId))
            is Result.Failure -> Result.Failure(analysis.error)
        }
    }

    /**
     * Agrega a la linea de tiempo del pipeline lo que aportan #27 y #28: un paso inicial de
     * deduplicacion y, en el clasificador, de donde salio el glosario que uso el scorer.
     */
    private fun stepsWithOfflineContext(analysis: PageAnalysis, courseId: Long?): List<CaptureStep> {
        val reuse = analysis.reusedFrom
        val pipelineSteps = CapturePipeline.stepsFor(analysis).map { step ->
            when {
                reuse != null && (step.name.startsWith("N0") || step.name.startsWith("N1 ")) ->
                    step.copy(description = "Omitido: resultado reutilizado de la página ya procesada.")
                step.name.startsWith("Clasificador") ->
                    step.copy(description = "${step.description} - ${glossaryDescription(courseId)}")
                else -> step
            }
        }

        val dedupStep = CaptureStep(
            state = "done",
            name = "Deduplicación (pHash)",
            description = when {
                courseId == null -> "Sin curso asociado: no se compara contra páginas anteriores."
                reuse != null -> "Página ya procesada (${reuse.hammingDistance}/64 bits distintos) - " +
                    "se reutiliza el resultado - 0 llamadas"
                else -> "Página nueva - hash guardado para detectar repeticiones."
            }
        )

        return listOf(dedupStep) + pipelineSteps
    }

    private fun glossaryDescription(courseId: Long?): String {
        val context = lastContext
        return when {
            courseId == null -> "sin glosario del curso"
            context == null -> "sin glosario en el dispositivo (se descarga al abrir la cámara con conexión)"
            else -> "glosario v${context.glossaryVersion} (${context.entries.size} términos, copia local)"
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

package cr.ac.una.glifo.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.opencv.ImageQualityAnalyzer
import cr.ac.una.glifo.core.pipeline.CapturePipeline
import cr.ac.una.glifo.core.pipeline.CapturePipeline.CaptureStepId
import cr.ac.una.glifo.core.pipeline.PerceptualHasher
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.LocalCourseContext
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.repository.CaptureRepository
import cr.ac.una.glifo.domain.repository.CourseContextRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val courseContextRepository: CourseContextRepository,
    private val pageDeduplicator: PageDeduplicator,
    private val noteRepository: NoteRepository,
    sessionCacheRegistry: SessionCacheRegistry
) : CaptureRepository {

    // La pantalla de procesamiento pide el analisis y los pasos por separado, y volver a
    // correr N0+N1 sobre la misma foto cuesta segundos. Se guarda el ultimo resultado y el
    // mutex evita que dos llamadas concurrentes arranquen el pipeline dos veces.
    private val pipelineLock = Mutex()
    @Volatile private var lastKey: Pair<String, Long?>? = null
    @Volatile private var lastAnalysis: PageAnalysis? = null
    @Volatile private var lastContext: LocalCourseContext? = null
    @Volatile private var lastHash: String? = null

    init {
        sessionCacheRegistry.register(::clearCache)
    }

    fun clearCache() {
        lastKey = null
        lastAnalysis = null
        lastContext = null
        lastHash = null
    }

    override suspend fun saveAsNote(photoPath: String, courseId: Long): Result<Long> {
        val analysis = when (val analyzed = analyzePage(photoPath, courseId)) {
            is Result.Success -> analyzed.value
            is Result.Failure -> return analyzed
        }
        val hash = lastHash
            ?: return Result.Failure(
                AppError.Validation("photo", appContext.getString(R.string.capture_error_hash_failed))
            )

        // La galería y la cámara dejan la foto en cacheDir, que el sistema puede vaciar; el
        // apunte necesita la foto para mostrar el recorte original de cada región.
        val storedPath = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(appContext.filesDir, "pages").apply { mkdirs() }
                val target = File(dir, "${hash}_${System.currentTimeMillis()}.jpg")
                File(photoPath).copyTo(target, overwrite = true).absolutePath
            }.getOrDefault(photoPath)
        }
        return noteRepository.saveCapture(courseId, storedPath, hash, analysis)
    }

    override suspend fun getQualityChecks(photoPath: String?): Result<List<CaptureQualityCheck>> {
        val bitmap = decode(photoPath)
        if (bitmap != null) {
            return Result.Success(ImageQualityAnalyzer.analyzeQuality(bitmap, appContext))
        }
        return noPhoto()
    }

    override suspend fun hasQualityWarning(photoPath: String?): Result<Boolean> {
        val bitmap = decode(photoPath)
        if (bitmap != null) {
            return Result.Success(ImageQualityAnalyzer.analyzeQuality(bitmap, appContext).any { it.isWarning })
        }
        return noPhoto()
    }

    override suspend fun analyzePage(photoPath: String?, courseId: Long?): Result<PageAnalysis> {
        if (photoPath == null) {
            return noPhoto()
        }

        return pipelineLock.withLock {
            val key = photoPath to courseId
            lastAnalysis?.takeIf { lastKey == key }?.let { return@withLock Result.Success(it) }

            val bitmap = decode(photoPath)
                ?: return@withLock Result.Failure(
                    AppError.Validation("photo", appContext.getString(R.string.capture_error_unreadable_photo))
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
                lastHash = hash ?: runCatching { PerceptualHasher.hash(bitmap) }.getOrNull()
                Result.Success(analysis)
            } catch (cancellation: CancellationException) {
                // Cancelar la pantalla no es un fallo del pipeline: se deja propagar.
                throw cancellation
            } catch (error: Exception) {
                Result.Failure(
                    AppError.Server(
                        code = "pipeline_local",
                        message = error.message ?: appContext.getString(R.string.capture_error_local_pipeline_failed)
                    )
                )
            }
        }
    }

    override suspend fun getProcessingSteps(photoPath: String?, courseId: Long?): Result<List<CaptureStep>> {
        if (photoPath == null) {
            return noPhoto()
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
        val pipelineSteps = CapturePipeline.stepSpecsFor(analysis).map { spec ->
            val baseName = appContext.getString(spec.nameRes)
            val baseDescription = if (spec.descriptionArgs.isEmpty()) {
                appContext.getString(spec.descriptionRes)
            } else {
                appContext.getString(spec.descriptionRes, *spec.descriptionArgs.toTypedArray())
            }
            val finalDescription = when {
                reuse != null && (spec.id == CaptureStepId.N0 || spec.id == CaptureStepId.N1) ->
                    appContext.getString(R.string.capture_step_reused_skipped)
                spec.id == CaptureStepId.Classifier ->
                    "$baseDescription - ${glossaryDescription(courseId)}"
                else -> baseDescription
            }
            CaptureStep(state = spec.state, name = baseName, description = finalDescription)
        }

        val dedupStep = CaptureStep(
            state = "done",
            name = appContext.getString(R.string.capture_step_dedup_name),
            description = when {
                courseId == null -> appContext.getString(R.string.capture_step_dedup_no_course)
                reuse != null -> appContext.getString(R.string.capture_step_dedup_reused, reuse.hammingDistance)
                else -> appContext.getString(R.string.capture_step_dedup_new)
            }
        )

        return listOf(dedupStep) + pipelineSteps
    }

    private fun glossaryDescription(courseId: Long?): String {
        val context = lastContext
        return when {
            courseId == null -> appContext.getString(R.string.capture_step_glossary_no_course)
            context == null -> appContext.getString(R.string.capture_step_glossary_not_cached)
            else -> appContext.getString(
                R.string.capture_step_glossary_cached,
                context.glossaryVersion,
                context.entries.size
            )
        }
    }

    private fun <T> noPhoto(): Result<T> =
        Result.Failure(AppError.Validation("photo", appContext.getString(R.string.capture_error_no_photo)))

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

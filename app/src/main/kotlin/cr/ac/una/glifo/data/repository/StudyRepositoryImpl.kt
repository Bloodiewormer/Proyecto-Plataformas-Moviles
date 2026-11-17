package cr.ac.una.glifo.data.repository

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.local.dao.StudyItemDao
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.data.dto.StudyItemResponse
import cr.ac.una.glifo.data.mapper.StudyItemMapper
import cr.ac.una.glifo.data.mapper.toEntity
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.model.SyllabusTopic
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Implementación de [StudyRepository] conectada al backend real y respaldada por Room (IA-02, #44).
 *
 * `getFlashcards` y `getQuizQuestions` consultan el backend, guardan la respuesta en Room y,
 * si no hay red (`AppError.NoConnection`), devuelven el material almacenado localmente.
 * `saveQuizResult` y `getQuizResult` guardan y leen en memoria el último resultado real
 * calculado localmente por `courseId`, sin usar `DummyStudyProvider`.
 */
class StudyRepositoryImpl : StudyRepository {

    private val api: GlifoApi
    private val studyItemDao: StudyItemDao
    private val courseRepository: CourseRepository
    private val appContext: Context?
    private val quizResultsByCourse = ConcurrentHashMap<Long, QuizResultInfo>()

    @Inject
    constructor(
        api: GlifoApi,
        studyItemDao: StudyItemDao,
        courseRepository: CourseRepository,
        @ApplicationContext appContext: Context
    ) {
        this.api = api
        this.studyItemDao = studyItemDao
        this.courseRepository = courseRepository
        this.appContext = appContext
    }

    constructor(
        api: GlifoApi,
        studyItemDao: StudyItemDao,
        courseRepository: CourseRepository
    ) {
        this.api = api
        this.studyItemDao = studyItemDao
        this.courseRepository = courseRepository
        this.appContext = null
    }

    constructor(
        api: GlifoApi,
        studyItemDao: StudyItemDao
    ) : this(
        api = api,
        studyItemDao = studyItemDao,
        courseRepository = CourseRepositoryImpl(api)
    )

    override suspend fun generateStudyItems(
        courseId: Long,
        topicIds: List<Long>,
        itemsPerTopic: Int
    ): Result<Unit> {
        val sanitizedTopics = topicIds.distinct().take(MAX_TOPICS_PER_BATCH)
        if (sanitizedTopics.isEmpty()) {
            return Result.Failure(
                AppError.Validation(
                    "topicIds",
                    appContext?.getString(R.string.study_error_select_topic)
                        ?: "Selecciona al menos un tema del curso"
                )
            )
        }
        val remote = ApiErrorHandler.safeApiCall(
            apiCall = {
                api.generateStudyItems(
                    courseId = courseId,
                    request = GenerateStudyItemsRequest(
                        topicIds = sanitizedTopics,
                        itemsPerTopic = itemsPerTopic
                    )
                )
            },
            transform = { it.data }
        )
        return when (remote) {
            is Result.Success -> {
                cacheRemoteItems(remote.value)
                Result.Success(Unit)
            }
            is Result.Failure -> remote
        }
    }

    override suspend fun getFlashcards(courseId: Long): Result<List<Flashcard>> {
        val remote = fetchAndCacheCourseItems(courseId)
        val topics = fetchTopicsOrEmpty(courseId)
        return when (remote) {
            is Result.Success -> Result.Success(StudyItemMapper.toFlashcards(remote.value, topics, appContext))
            is Result.Failure -> {
                if (remote.error is AppError.NoConnection) {
                    val cached = studyItemDao.itemsForCourse(courseId)
                    if (cached.isNotEmpty()) {
                        Result.Success(StudyItemMapper.entitiesToFlashcards(cached, topics, appContext))
                    } else {
                        remote
                    }
                } else {
                    remote
                }
            }
        }
    }

    override suspend fun getQuizQuestions(courseId: Long): Result<List<QuizQuestion>> {
        val remote = fetchAndCacheCourseItems(courseId)
        val topics = fetchTopicsOrEmpty(courseId)
        return when (remote) {
            is Result.Success -> Result.Success(StudyItemMapper.toQuizQuestions(remote.value, topics, context = appContext))
            is Result.Failure -> {
                if (remote.error is AppError.NoConnection) {
                    val cached = studyItemDao.itemsForCourse(courseId)
                    if (cached.isNotEmpty()) {
                        Result.Success(StudyItemMapper.entitiesToQuizQuestions(cached, topics, context = appContext))
                    } else {
                        remote
                    }
                } else {
                    remote
                }
            }
        }
    }

    override suspend fun getQuizQuestion(courseId: Long): Result<QuizQuestion> =
        when (val result = getQuizQuestions(courseId)) {
            is Result.Success -> {
                val first = result.value.firstOrNull()
                if (first != null) {
                    Result.Success(first)
                } else {
                    Result.Failure(
                        AppError.Validation(
                            "quiz",
                            appContext?.getString(R.string.study_error_no_quiz)
                                ?: "No hay preguntas de quiz disponibles"
                        )
                    )
                }
            }
            is Result.Failure -> result
        }

    override suspend fun saveQuizResult(courseId: Long, result: QuizResultInfo) {
        quizResultsByCourse[courseId] = result
    }

    override suspend fun getQuizResult(courseId: Long): Result<QuizResultInfo?> =
        Result.Success(quizResultsByCourse[courseId])

    private suspend fun fetchAndCacheCourseItems(courseId: Long): Result<List<StudyItemResponse>> {
        val remote = ApiErrorHandler.safeApiCall(
            apiCall = { api.getStudyItems(courseId = courseId) },
            transform = { it.data }
        )
        if (remote is Result.Success) {
            cacheRemoteItems(remote.value)
        }
        return remote
    }

    private suspend fun cacheRemoteItems(items: List<StudyItemResponse>) {
        if (items.isNotEmpty()) {
            studyItemDao.upsertAll(items.map { it.toEntity() })
        }
    }

    private suspend fun fetchTopicsOrEmpty(courseId: Long): List<SyllabusTopic> =
        runCatching {
            (courseRepository.getCourseTopics(courseId) as? Result.Success)?.value.orEmpty()
        }.getOrDefault(emptyList())

    private companion object {
        const val MAX_TOPICS_PER_BATCH = 10
    }
}

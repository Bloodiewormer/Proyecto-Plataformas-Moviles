package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.CoverageResponseDto
import cr.ac.una.glifo.data.mapper.CoverageMapper
import cr.ac.una.glifo.data.mapper.toDeltaInfo
import cr.ac.una.glifo.data.mapper.toSummary
import cr.ac.una.glifo.data.mapper.toTopics
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.CoverageDeltaInfo
import cr.ac.una.glifo.domain.model.CoverageSummary
import cr.ac.una.glifo.domain.model.CoverageTopic
import cr.ac.una.glifo.domain.model.TopicDetail
import cr.ac.una.glifo.domain.model.TopicNoteRef
import cr.ac.una.glifo.domain.repository.CoverageRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Implementación de [CoverageRepository] conectada a los endpoints reales de cobertura (#43):
 * - `GET courses/{courseId}/coverage`
 * - `POST courses/{courseId}/coverage/evaluate`
 * - `GET courses/{courseId}/coverage/delta?since=`
 */
class CoverageRepositoryImpl : CoverageRepository {

    private val api: GlifoApi
    private val activeCourseStore: ActiveCourseStore?
    private val noteDao: NoteDao?

    private val cachedCoverageByCourse = ConcurrentHashMap<Long, CoverageResponseDto>()
    private val lastEvaluationIsoByCourse = ConcurrentHashMap<Long, String>()

    @Inject
    constructor(
        api: GlifoApi,
        activeCourseStore: ActiveCourseStore,
        noteDao: NoteDao
    ) {
        this.api = api
        this.activeCourseStore = activeCourseStore
        this.noteDao = noteDao
    }

    constructor(
        api: GlifoApi,
        activeCourseStore: ActiveCourseStore? = null
    ) {
        this.api = api
        this.activeCourseStore = activeCourseStore
        this.noteDao = null
    }

    override suspend fun getSummary(courseId: Long): Result<CoverageSummary> {
        val remote = fetchCoverage(courseId)
        return when (remote) {
            is Result.Success -> Result.Success(remote.value.toSummary())
            is Result.Failure -> remote
        }
    }

    override suspend fun getTopics(courseId: Long): Result<List<CoverageTopic>> {
        val remote = fetchCoverage(courseId)
        return when (remote) {
            is Result.Success -> Result.Success(remote.value.toTopics())
            is Result.Failure -> remote
        }
    }

    override suspend fun evaluateCoverage(courseId: Long): Result<CoverageSummary> {
        val beforeEvalIso = Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.SECONDS).toString()
        val remote = ApiErrorHandler.safeApiCall(
            apiCall = { api.evaluateCoverage(courseId) },
            transform = { it.data }
        )
        return when (remote) {
            is Result.Success -> {
                cachedCoverageByCourse[courseId] = remote.value
                lastEvaluationIsoByCourse[courseId] = beforeEvalIso
                Result.Success(remote.value.toSummary())
            }
            is Result.Failure -> remote
        }
    }

    override suspend fun getTopicDetail(name: String): Result<TopicDetail> {
        val courseId = activeCourseStore?.activeCourseId?.value
            ?: cachedCoverageByCourse.keys().toList().firstOrNull()
            ?: return Result.Failure(
                AppError.Validation("courseId", "Selecciona un curso activo primero")
            )

        val coverageDto = cachedCoverageByCourse[courseId] ?: when (val fetched = fetchCoverage(courseId)) {
            is Result.Success -> fetched.value
            is Result.Failure -> return fetched
        }

        val topics = coverageDto.toTopics()
        val normalizedQuery = name.trim()
        val matched = topics.firstOrNull {
            it.name.equals(normalizedQuery, ignoreCase = true) ||
                it.title.equals(normalizedQuery, ignoreCase = true) ||
                it.code.equals(normalizedQuery, ignoreCase = true)
        } ?: CoverageTopic(
            state = ConfidenceState.Escalated,
            name = normalizedQuery,
            note = null,
            score = "0.00"
        )

        val noteRefs = findNoteRefsForTopic(courseId, matched)
        return Result.Success(CoverageMapper.toTopicDetail(matched, noteRefs))
    }

    override suspend fun getDelta(courseId: Long?, since: String?): Result<CoverageDeltaInfo> {
        val resolvedCourseId = courseId
            ?: activeCourseStore?.activeCourseId?.value
            ?: cachedCoverageByCourse.keys().toList().firstOrNull()
            ?: return Result.Failure(
                AppError.Validation("courseId", "Selecciona un curso activo primero")
            )

        val resolvedSince = since
            ?: lastEvaluationIsoByCourse[resolvedCourseId]
            ?: Instant.now().minus(DEFAULT_DELTA_DAYS, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString()

        return ApiErrorHandler.safeApiCall(
            apiCall = { api.getCoverageDelta(resolvedCourseId, resolvedSince) },
            transform = { it.data.toDeltaInfo() }
        )
    }

    fun getLastEvaluationIso(courseId: Long): String? = lastEvaluationIsoByCourse[courseId]

    fun setLastEvaluationIso(courseId: Long, iso: String) {
        lastEvaluationIsoByCourse[courseId] = iso
    }

    private suspend fun fetchCoverage(courseId: Long): Result<CoverageResponseDto> {
        val remote = ApiErrorHandler.safeApiCall(
            apiCall = { api.getCoverage(courseId) },
            transform = { it.data }
        )
        if (remote is Result.Success) {
            cachedCoverageByCourse[courseId] = remote.value
        }
        return remote
    }

    private suspend fun findNoteRefsForTopic(courseId: Long, topic: CoverageTopic): List<TopicNoteRef> {
        val dao = noteDao ?: return emptyList()
        return runCatching {
            val notes = dao.notesForCourse(courseId)
            val titleNeedle = topic.title.trim().takeIf { it.length >= 3 }
            val codeNeedle = topic.code.trim().takeIf { it.isNotEmpty() }
            notes.filter { entity ->
                val haystack = "${entity.title} ${entity.contentJson.orEmpty()}"
                (titleNeedle != null && haystack.contains(titleNeedle, ignoreCase = true)) ||
                    (codeNeedle != null && haystack.contains(codeNeedle, ignoreCase = true))
            }.map { entity ->
                TopicNoteRef(
                    noteId = entity.id,
                    title = entity.title,
                    meta = entity.classDate
                )
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val DEFAULT_DELTA_DAYS = 7L
    }
}

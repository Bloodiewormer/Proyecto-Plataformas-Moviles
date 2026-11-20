package cr.ac.una.glifo.data.repository

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.local.NoteTopicCache
import cr.ac.una.glifo.data.datasource.local.dao.NoteDao
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
    private val noteTopicCache: NoteTopicCache?
    private val appContext: Context?

    private val cachedCoverageByCourse = ConcurrentHashMap<Long, CoverageResponseDto>()
    private val lastEvaluationIsoByCourse = ConcurrentHashMap<Long, String>()

    @Inject
    constructor(
        api: GlifoApi,
        activeCourseStore: ActiveCourseStore,
        noteDao: NoteDao,
        noteTopicCache: NoteTopicCache,
        @ApplicationContext appContext: Context,
        sessionCacheRegistry: SessionCacheRegistry
    ) {
        this.api = api
        this.activeCourseStore = activeCourseStore
        this.noteDao = noteDao
        this.noteTopicCache = noteTopicCache
        this.appContext = appContext
        sessionCacheRegistry.register(::clearCache)
    }

    constructor(
        api: GlifoApi,
        activeCourseStore: ActiveCourseStore? = null,
        appContext: Context? = null,
        noteDao: NoteDao? = null,
        noteTopicCache: NoteTopicCache? = null,
        sessionCacheRegistry: SessionCacheRegistry? = null
    ) {
        this.api = api
        this.activeCourseStore = activeCourseStore
        this.noteDao = noteDao
        this.noteTopicCache = noteTopicCache
        this.appContext = appContext
        sessionCacheRegistry?.register(::clearCache)
    }

    fun clearCache() {
        cachedCoverageByCourse.clear()
        lastEvaluationIsoByCourse.clear()
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
                AppError.Validation("courseId", noActiveCourseMessage())
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
                AppError.Validation("courseId", noActiveCourseMessage())
            )

        val resolvedSince = since
            ?: lastEvaluationIsoByCourse[resolvedCourseId]
            ?: Instant.now().minus(DEFAULT_DELTA_DAYS, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString()

        return ApiErrorHandler.safeApiCall(
            apiCall = { api.getCoverageDelta(resolvedCourseId, resolvedSince) },
            transform = { it.data.toDeltaInfo() }
        )
    }

    private fun noActiveCourseMessage(): String =
        appContext?.getString(R.string.coverage_error_no_active_course).orEmpty()

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
        if (topic.topicId <= 0L) return emptyList()

        val remoteNotes = try {
            api.getNotes(courseId).data
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }

        if (remoteNotes != null) {
            val localNotes = noteDao?.let { dao ->
                runCatching {
                    dao.cacheRemoteNotes(
                        courseId,
                        remoteNotes.map { dto ->
                            NoteEntity(
                                serverId = dto.id,
                                courseId = dto.courseId,
                                title = dto.title,
                                classDate = dto.classDate,
                                status = dto.status ?: STATUS_READY,
                                createdAt = System.currentTimeMillis()
                            )
                        }
                    )
                    dao.notesForCourse(courseId)
                }.getOrNull()
            }.orEmpty()

            val localByServerId = localNotes.mapNotNull { entity -> entity.serverId?.let { it to entity } }.toMap()
            remoteNotes.forEach { dto ->
                val localEntity = localByServerId[dto.id] ?: return@forEach
                noteTopicCache?.setTopicId(localEntity.id, dto.syllabusTopicId)
            }

            val remoteRefs = remoteNotes
                .filter { it.syllabusTopicId == topic.topicId }
                .map { dto ->
                    val localEntity = localByServerId[dto.id]
                    TopicNoteRef(
                        noteId = localEntity?.id ?: dto.id,
                        title = localEntity?.title ?: dto.title,
                        meta = localEntity?.classDate ?: dto.classDate
                    )
                }

            val localOnlyRefs = localNotes
                .filter { it.serverId == null && noteTopicCache?.getTopicId(it.id) == topic.topicId }
                .map { entity ->
                    TopicNoteRef(
                        noteId = entity.id,
                        title = entity.title,
                        meta = entity.classDate
                    )
                }

            return remoteRefs + localOnlyRefs
        }

        val dao = noteDao ?: return emptyList()
        val cache = noteTopicCache ?: return emptyList()
        return runCatching {
            dao.notesForCourse(courseId)
                .filter { cache.getTopicId(it.id) == topic.topicId }
                .map { entity ->
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
        const val STATUS_READY = "READY"
    }
}

package cr.ac.una.glifo.data.repository

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.dummy.DummyTeacherProvider
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.CreateSyllabusTopicRequest
import cr.ac.una.glifo.data.dto.PublishSyllabusRequest
import cr.ac.una.glifo.data.mapper.toSyllabusUnitRows
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TeacherRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: GlifoApi,
    private val activeCourseStore: ActiveCourseStore
) : TeacherRepository {
    override suspend fun getGlossaryEntries(): Result<List<GlossaryEntry>> =
        Result.Success(DummyTeacherProvider.getGlossaryEntries())

    override suspend fun addGlossaryEntry(term: String, notation: String, isSymbol: Boolean): Result<Unit> {
        DummyTeacherProvider.addGlossaryEntry(term, notation, isSymbol)
        return Result.Success(Unit)
    }

    override suspend fun getGlossarySuggestions(): Result<List<GlossarySuggestion>> =
        Result.Success(DummyTeacherProvider.getGlossarySuggestions())

    override suspend fun getSyllabusFileName(): Result<String> {
        val courseId = when (val resolved = resolveActiveCourseId()) {
            is Result.Success -> resolved.value
            is Result.Failure -> return Result.Failure(resolved.error)
        }
        return ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourse(courseId) },
            transform = { "programa-${it.data.code}.pdf" }
        )
    }

    override suspend fun isSyllabusPublished(): Result<Boolean> {
        val courseId = when (val resolved = resolveActiveCourseId()) {
            is Result.Success -> resolved.value
            is Result.Failure -> return Result.Failure(resolved.error)
        }
        return ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourse(courseId) },
            transform = { it.data.syllabusStatus.equals("READY", ignoreCase = true) }
        )
    }

    override suspend fun getSyllabusUnits(): Result<List<SyllabusUnitRow>> {
        val courseId = when (val resolved = resolveActiveCourseId()) {
            is Result.Success -> resolved.value
            is Result.Failure -> return Result.Failure(resolved.error)
        }
        return ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourseTopics(courseId) },
            transform = { it.data.toSyllabusUnitRows() }
        )
    }

    override suspend fun addSyllabusTopic(label: String): Result<Unit> {
        val trimmed = label.trim()
        if (trimmed.isBlank()) {
            return Result.Failure(
                AppError.Validation(
                    field = "title",
                    issue = appContext.getString(R.string.teacher_syllabus_topic_title_required)
                )
            )
        }
        val courseId = when (val resolved = resolveActiveCourseId()) {
            is Result.Success -> resolved.value
            is Result.Failure -> return Result.Failure(resolved.error)
        }
        return ApiErrorHandler.safeApiCall(
            apiCall = {
                val existing = api.getCourseTopics(courseId).data
                val firstRootId = existing.firstOrNull { it.parentId == null }?.id
                val nextOrder = (existing.maxOfOrNull { it.orderIndex } ?: 0) + 1
                val (code, title) = parseTopicLabel(trimmed, nextOrder)
                api.addCourseTopic(
                    courseId = courseId,
                    request = CreateSyllabusTopicRequest(
                        code = code,
                        title = title,
                        orderIndex = nextOrder,
                        parentId = firstRootId
                    )
                )
            },
            transform = {}
        )
    }

    override suspend fun publishSyllabus(topics: List<String>): Result<Unit> {
        val cleanTopics = topics.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanTopics.isEmpty()) {
            return Result.Failure(
                AppError.Validation(
                    field = "topics",
                    issue = appContext.getString(R.string.teacher_syllabus_empty_topics_error)
                )
            )
        }
        val courseId = when (val resolved = resolveActiveCourseId()) {
            is Result.Success -> resolved.value
            is Result.Failure -> return Result.Failure(resolved.error)
        }
        return ApiErrorHandler.safeApiCall(
            apiCall = {
                val requests = cleanTopics.mapIndexed { index, raw ->
                    val order = index + 1
                    val (code, title) = parseTopicLabel(raw, order)
                    CreateSyllabusTopicRequest(
                        code = code,
                        title = title,
                        orderIndex = order
                    )
                }
                api.publishSyllabus(
                    courseId = courseId,
                    request = PublishSyllabusRequest(
                        sourceUri = "programa-curso-$courseId.pdf",
                        topics = requests
                    )
                )
            },
            transform = {}
        )
    }

    override suspend fun getGaps(): Result<List<TeacherGapTopic>> =
        Result.Success(DummyTeacherProvider.getGaps())

    private fun resolveActiveCourseId(): Result<Long> {
        val activeId = activeCourseStore.activeCourseId.value
        return if (activeId != null) {
            Result.Success(activeId)
        } else {
            Result.Failure(
                AppError.Validation(
                    field = "course",
                    issue = appContext.getString(R.string.teacher_syllabus_no_active_course)
                )
            )
        }
    }

    private fun parseTopicLabel(raw: String, fallbackIndex: Int): Pair<String, String> {
        val parts = raw.split(Regex("\\s+"), limit = 2)
        val firstToken = parts.firstOrNull().orEmpty()
        return if (parts.size == 2 && firstToken.any { it.isDigit() } && firstToken.length <= 20) {
            firstToken to parts[1].trim()
        } else {
            "T$fallbackIndex" to raw
        }
    }
}


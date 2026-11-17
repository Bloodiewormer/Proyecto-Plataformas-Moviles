package cr.ac.una.glifo.data.repository

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.CreateGlossaryEntryRequest
import cr.ac.una.glifo.data.dto.CreateSyllabusTopicRequest
import cr.ac.una.glifo.data.dto.PublishSyllabusRequest
import cr.ac.una.glifo.data.dto.ResolveGlossarySuggestionRequest
import cr.ac.una.glifo.data.mapper.TeacherGapsMapper
import cr.ac.una.glifo.data.mapper.toDomain
import cr.ac.una.glifo.data.mapper.toSyllabusUnitRows
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.model.TeacherGroupCoverage
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Glosario (#33) y temario (#32) van contra el backend y siguen al curso activo, igual que
 * el resto de pantallas del docente. Las brechas siguen en el proveedor dummy.
 */
class TeacherRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: GlifoApi,
    private val activeCourseStore: ActiveCourseStore
) : TeacherRepository {

    override suspend fun getGlossaryEntries(): Result<List<GlossaryEntry>> = withActiveCourse { courseId ->
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourseGlossary(courseId) },
            transform = { response -> response.data.entries.map { it.toDomain() } }
        )
    }

    override suspend fun addGlossaryEntry(term: String, notation: String, isSymbol: Boolean): Result<Unit> {
        if (term.isBlank() || notation.isBlank()) {
            return Result.Failure(AppError.Validation("term", "Término y notación son obligatorios"))
        }
        return withActiveCourse { courseId ->
            val request = CreateGlossaryEntryRequest(
                term = term.trim(),
                canonicalForm = notation.trim(),
                kind = if (isSymbol) "SYMBOL" else "TERM"
            )
            ApiErrorHandler.safeApiCall(apiCall = { api.addGlossaryEntry(courseId, request) }, transform = {})
        }
    }

    override suspend fun getGlossarySuggestions(): Result<List<GlossarySuggestion>> = withActiveCourse { courseId ->
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getGlossarySuggestions(courseId) },
            transform = { response -> response.data.map { it.toDomain() } }
        )
    }

    override suspend fun resolveGlossarySuggestion(suggestionId: Long, approve: Boolean): Result<Unit> =
        withActiveCourse { courseId ->
            val request = ResolveGlossarySuggestionRequest(status = if (approve) "APPROVED" else "REJECTED")
            ApiErrorHandler.safeApiCall(
                apiCall = { api.resolveGlossarySuggestion(courseId, suggestionId, request) },
                transform = {}
            )
        }

    override suspend fun getSyllabusFileName(): Result<String> = withActiveCourse { courseId ->
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourse(courseId) },
            transform = { "programa-${it.data.code}.pdf" }
        )
    }

    override suspend fun isSyllabusPublished(): Result<Boolean> = withActiveCourse { courseId ->
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourse(courseId) },
            transform = { it.data.syllabusStatus?.uppercase() in PUBLISHED_SYLLABUS_STATUSES }
        )
    }

    override suspend fun getSyllabusUnits(): Result<List<SyllabusUnitRow>> = withActiveCourse { courseId ->
        ApiErrorHandler.safeApiCall(
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
        return withActiveCourse { courseId ->
            ApiErrorHandler.safeApiCall(
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
        return withActiveCourse { courseId ->
            ApiErrorHandler.safeApiCall(
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
    }

    // Antes venía de DummyTeacherProvider (temas de otro curso). Ahora sale de GET coverage/gaps.
    override suspend fun getGaps(): Result<List<TeacherGapTopic>> =
        when (val group = getGroupCoverage()) {
            is Result.Success -> Result.Success(group.value.topics)
            is Result.Failure -> group
        }

    override suspend fun getGroupCoverage(): Result<TeacherGroupCoverage> = withActiveCourse { courseId ->
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourseGaps(courseId) },
            transform = { response -> TeacherGapsMapper.toGroupCoverage(response.data) }
        )
    }

    /** Sin curso activo no hay a qué glosario ni temario apuntar: se le pide al docente que elija uno. */
    private suspend fun <T> withActiveCourse(block: suspend (Long) -> Result<T>): Result<T> {
        val courseId = activeCourseStore.activeCourseId.value
            ?: return Result.Failure(
                AppError.Validation(
                    field = "course",
                    issue = appContext.getString(R.string.teacher_syllabus_no_active_course)
                )
            )
        return block(courseId)
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

    private companion object {
        /**
         * El backend marca el temario publicado como PARSED (CourseMappers.kt) y Mockoon como
         * PUBLISHED. Antes se comparaba con "READY", que nadie envía, y un temario publicado
         * se veía vacío.
         */
        val PUBLISHED_SYLLABUS_STATUSES = setOf("PARSED", "PUBLISHED")
    }
}

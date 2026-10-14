package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.dummy.DummyTeacherProvider
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.CreateGlossaryEntryRequest
import cr.ac.una.glifo.data.dto.ResolveGlossarySuggestionRequest
import cr.ac.una.glifo.data.mapper.toDomain
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.repository.TeacherRepository
import javax.inject.Inject

/**
 * El glosario y sus sugerencias van contra el backend (issue #33) y siguen al curso activo,
 * igual que el resto de pantallas del docente. Temario y brechas siguen en el proveedor
 * dummy hasta que se conecten sus endpoints.
 */
class TeacherRepositoryImpl @Inject constructor(
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

    override suspend fun getSyllabusFileName(): Result<String> =
        Result.Success(DummyTeacherProvider.getSyllabusFileName())

    override suspend fun getSyllabusUnits(): Result<List<SyllabusUnitRow>> =
        Result.Success(DummyTeacherProvider.getSyllabusUnits())

    override suspend fun addSyllabusTopic(label: String): Result<Unit> {
        DummyTeacherProvider.addSyllabusTopic(label)
        return Result.Success(Unit)
    }

    override suspend fun getGaps(): Result<List<TeacherGapTopic>> =
        Result.Success(DummyTeacherProvider.getGaps())

    /** Sin curso activo no hay a qué glosario apuntar: se le pide al docente que elija uno. */
    private suspend fun <T> withActiveCourse(block: suspend (Long) -> Result<T>): Result<T> {
        val courseId = activeCourseStore.activeCourseId.value
            ?: return Result.Failure(AppError.Validation("course", "Elegí un curso para ver su glosario"))
        return block(courseId)
    }
}

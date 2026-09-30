package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.local.dao.LocalCourseContextDao
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.mapper.toDomain
import cr.ac.una.glifo.data.mapper.toEntity
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ContextSource
import cr.ac.una.glifo.domain.model.LocalCourseContext
import cr.ac.una.glifo.domain.repository.CourseContextRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caché versionada del contexto del curso en Room (D-17, issue #27).
 *
 * El servidor manda el glosario junto con `glossary_version` y `syllabus_version`. Si las dos
 * coinciden con la copia local no se reescribe nada (solo se marca como vigente); si alguna
 * cambió, la fila se reemplaza completa. Sin red se usa lo que haya en Room, que es lo que
 * permite que el pipeline puntúe con el glosario del curso estando offline.
 */
@Singleton
class CourseContextRepositoryImpl @Inject constructor(
    private val api: GlifoApi,
    private val dao: LocalCourseContextDao
) : CourseContextRepository {

    override suspend fun refresh(courseId: Long): Result<LocalCourseContext> {
        val remote = ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourseGlossary(courseId) },
            transform = { it.data }
        )

        return when (remote) {
            is Result.Success -> {
                val response = remote.value
                val now = System.currentTimeMillis()
                val local = dao.versions(courseId)
                val isCurrent = local != null &&
                    local.glossaryVersion == response.glossaryVersion &&
                    local.syllabusVersion == response.syllabusVersion

                if (isCurrent) dao.touch(courseId, now) else dao.upsert(response.toEntity(now))

                val stored = dao.forCourse(courseId) ?: response.toEntity(now)
                Result.Success(stored.toDomain(ContextSource.Network))
            }
            is Result.Failure -> {
                val cached = dao.forCourse(courseId)
                if (cached != null) Result.Success(cached.toDomain(ContextSource.Cache)) else remote
            }
        }
    }

    override suspend fun getCached(courseId: Long): LocalCourseContext? =
        dao.forCourse(courseId)?.toDomain(ContextSource.Cache)
}

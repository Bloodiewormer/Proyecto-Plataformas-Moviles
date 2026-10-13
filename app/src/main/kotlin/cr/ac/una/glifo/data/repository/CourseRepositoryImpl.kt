package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.dummy.DummyCourseProvider
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.CreateCourseRequest
import cr.ac.una.glifo.data.dto.JoinCourseRequest
import cr.ac.una.glifo.data.mapper.toDomain
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.repository.CourseRepository
import javax.inject.Inject

/**
 * CourseRepository implementation consuming Retrofit + Gson [GlifoApi] against Mockoon/Backend.
 * Maps network operations to domain [Result] and [AppError] via [ApiErrorHandler].
 */
class CourseRepositoryImpl : CourseRepository {

    private val api: GlifoApi
    private val activeCourseStore: ActiveCourseStore?

    @Inject
    constructor(api: GlifoApi, activeCourseStore: ActiveCourseStore) {
        this.api = api
        this.activeCourseStore = activeCourseStore
    }

    constructor(api: GlifoApi) {
        this.api = api
        this.activeCourseStore = null
    }

    override suspend fun getStudentCourses(): Result<List<Course>> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourses() },
            transform = { response -> response.data.map { it.toDomain() } }
        )

    override suspend fun getTeacherCourses(): Result<List<Course>> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourses() },
            transform = { response -> response.data.map { it.toDomain() } }
        )

    override suspend fun getCourseDetail(courseId: Long): Result<CourseDetail> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getCourse(courseId) },
            transform = { it.data.toDomain() }
        )

    override suspend fun getCourseStats(courseId: Long): Result<CourseStats> =
        Result.Success(DummyCourseProvider.getStats(courseId))

    override suspend fun findCourseByJoinCode(joinCode: String): Result<Course> {
        val trimmed = joinCode.trim()
        if (trimmed.isBlank()) {
            return Result.Failure(AppError.Validation("joinCode", "El código no puede estar vacío"))
        }
        return when (val coursesResult = getStudentCourses()) {
            is Result.Success -> {
                val course = coursesResult.value.find { it.joinCode.equals(trimmed, ignoreCase = true) }
                if (course != null) {
                    Result.Success(course)
                } else {
                    Result.Failure(AppError.Validation("joinCode", "Código de curso inválido"))
                }
            }
            is Result.Failure -> Result.Failure(coursesResult.error)
        }
    }

    override suspend fun createCourse(name: String, code: String, term: String): Result<Course> {
        if (name.isBlank() || code.isBlank()) {
            return Result.Failure(AppError.Validation("name", "Nombre y código son obligatorios"))
        }
        return ApiErrorHandler.safeApiCall(
            apiCall = { api.createCourse(CreateCourseRequest(name = name.trim(), code = code.trim(), term = term.trim())) },
            transform = { response ->
                val course = response.data.toDomain()
                activeCourseStore?.set(course.id, course.name)
                course
            }
        )
    }

    override suspend fun joinCourse(joinCode: String): Result<Unit> {
        val trimmed = joinCode.trim()
        if (trimmed.isBlank()) {
            return Result.Failure(AppError.Validation("joinCode", "Código de curso inválido"))
        }
        return ApiErrorHandler.safeApiCall(
            apiCall = { api.joinCourse(JoinCourseRequest(trimmed)) },
            transform = { response ->
                // Activa el curso recién matriculado usando course_id y course_name de EnrollmentResponse (#32 d).
                activeCourseStore?.set(
                    courseId = response.data.courseId,
                    name = response.data.courseName?.takeIf { it.isNotBlank() }
                )
            }
        )
    }
}


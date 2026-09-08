package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.dummy.DummyCourseProvider
import cr.ac.una.glifo.data.mapper.toDomain
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.repository.CourseRepository
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor() : CourseRepository {

    override suspend fun getStudentCourses(): Result<List<Course>> =
        Result.Success(DummyCourseProvider.getCourses.map { it.toDomain() })

    override suspend fun getTeacherCourses(): Result<List<Course>> =
        Result.Success(DummyCourseProvider.getTeacherCourses.map { it.toDomain() })

    override suspend fun getCourseDetail(courseId: Long): Result<CourseDetail> {
        val detail = DummyCourseProvider.getCourseDetail(courseId)
            ?: return Result.Failure(AppError.Validation("courseId", "Curso no encontrado"))
        return Result.Success(detail.toDomain())
    }

    override suspend fun getCourseStats(courseId: Long): Result<CourseStats> =
        Result.Success(DummyCourseProvider.getStats(courseId))

    override suspend fun findCourseByJoinCode(joinCode: String): Result<Course> {
        val course = DummyCourseProvider.getCourseByJoinCode(joinCode)
            ?: return Result.Failure(AppError.Validation("joinCode", "Código de curso inválido"))
        return Result.Success(course.toDomain())
    }

    override suspend fun createCourse(name: String, code: String, term: String): Result<Course> {
        if (name.isBlank() || code.isBlank()) {
            return Result.Failure(AppError.Validation("name", "Nombre y código son obligatorios"))
        }
        return Result.Success(DummyCourseProvider.createCourse(name, code, term).toDomain())
    }
}

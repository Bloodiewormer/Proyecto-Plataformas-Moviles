package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.SyllabusTopic

interface CourseRepository {
    suspend fun getStudentCourses(): Result<List<Course>>
    suspend fun getTeacherCourses(): Result<List<Course>>
    suspend fun getCourseDetail(courseId: Long): Result<CourseDetail>
    suspend fun getCourseStats(courseId: Long): Result<CourseStats>
    suspend fun getCourseTopics(courseId: Long): Result<List<SyllabusTopic>> = Result.Success(emptyList())
    suspend fun findCourseByJoinCode(joinCode: String): Result<Course>
    suspend fun createCourse(name: String, code: String, term: String): Result<Course>
    suspend fun joinCourse(joinCode: String): Result<Unit>
}

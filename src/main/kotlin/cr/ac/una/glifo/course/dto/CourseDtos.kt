package cr.ac.una.glifo.course.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateCourseRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
    @field:NotBlank val term: String
)

data class JoinCourseRequest(
    @field:NotBlank val joinCode: String
)

data class CourseResponse(
    val id: Long,
    val name: String,
    val code: String,
    val term: String,
    val joinCode: String,
    val syllabusStatus: String
)

data class EnrollmentResponse(
    val id: Long,
    val courseId: Long,
    val courseName: String,
    val status: String,
    val joinedAt: Instant
)

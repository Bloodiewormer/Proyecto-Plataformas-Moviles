package cr.ac.una.glifo.course.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import java.time.Instant

// Los nombres del JSON van en snake_case (@JsonProperty) porque así los lee la app
// (CourseDtos.kt del frontend usa @SerializedName("join_code"), etc.).

data class CreateCourseRequest(
    @field:NotBlank(message = "Nombre y código son obligatorios") val code: String,
    @field:NotBlank(message = "Nombre y código son obligatorios") val name: String,
    @field:NotBlank(message = "El período es obligatorio") val term: String
)

data class JoinCourseRequest(
    @JsonProperty("join_code")
    @field:NotBlank(message = "Código de curso inválido")
    val joinCode: String
)

data class CourseResponse(
    val id: Long,
    val name: String,
    val code: String,
    val term: String,
    @JsonProperty("join_code") val joinCode: String,
    @JsonProperty("syllabus_status") val syllabusStatus: String
)

data class EnrollmentResponse(
    val id: Long,
    @JsonProperty("course_id") val courseId: Long,
    @JsonProperty("course_name") val courseName: String,
    val status: String,
    @JsonProperty("joined_at") val joinedAt: Instant
)

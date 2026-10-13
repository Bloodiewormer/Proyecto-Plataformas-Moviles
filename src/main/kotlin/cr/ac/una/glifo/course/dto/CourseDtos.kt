package cr.ac.una.glifo.course.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

// Los nombres del JSON van en snake_case (@JsonProperty) porque así los lee la app
// (CourseDtos.kt del frontend usa @SerializedName("join_code"), etc.).
//
// DTO de entrada (*Request): solo lo que el cliente puede enviar; el id, el dueño, el
// código de matrícula y las fechas los decide el backend. Los @Size coinciden con el largo
// de cada columna en la entidad Course, así un texto demasiado largo es un 400 y no un
// error de base de datos.

data class CreateCourseRequest(
    @field:NotBlank(message = "Nombre y código son obligatorios")
    @field:Size(max = 20, message = "El código del curso admite como máximo 20 caracteres")
    val code: String,
    @field:NotBlank(message = "Nombre y código son obligatorios")
    @field:Size(max = 150, message = "El nombre del curso admite como máximo 150 caracteres")
    val name: String,
    @field:NotBlank(message = "El período es obligatorio")
    @field:Size(max = 20, message = "El período admite como máximo 20 caracteres")
    val term: String
)

data class JoinCourseRequest(
    @JsonProperty("join_code")
    @field:NotBlank(message = "Código de curso inválido")
    @field:Size(max = 10, message = "Código de curso inválido")
    val joinCode: String
)

// DTO de salida (*Response): lo que el cliente ve del curso, sin el dueño ni datos internos.

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

package cr.ac.una.glifo.course.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
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

/** DTO de entrada para agregar un tema o subtema al temario del curso (#32). */
data class CreateSyllabusTopicRequest(
    @field:NotBlank(message = "El código del tema es obligatorio")
    val code: String,
    @field:NotBlank(message = "El título del tema es obligatorio")
    val title: String,
    @JsonProperty("order_index")
    val orderIndex: Int = 1,
    @JsonProperty("parent_id")
    val parentId: Long? = null,
    @JsonProperty("parent_code")
    val parentCode: String? = null
)

/** DTO de entrada para publicar el temario estructurado de un curso (#32). */
data class PublishSyllabusRequest(
    @JsonProperty("source_uri")
    val sourceUri: String? = null,
    @field:NotEmpty(message = "El temario debe incluir al menos un tema")
    @field:Valid
    val topics: List<CreateSyllabusTopicRequest>
)

/** DTO de salida que representa un tema o unidad del temario (#32). */
data class SyllabusTopicResponse(
    val id: Long,
    @JsonProperty("course_id") val courseId: Long,
    @JsonProperty("parent_id") val parentId: Long?,
    val code: String,
    val title: String,
    @JsonProperty("order_index") val orderIndex: Int
)

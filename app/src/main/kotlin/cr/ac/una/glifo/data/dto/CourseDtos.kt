package cr.ac.una.glifo.data.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("data") val data: T,
    @SerializedName("meta") val meta: Map<String, Any>? = null
)

data class CourseResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("join_code") val joinCode: String
)

data class CourseDetailResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("join_code") val joinCode: String,
    @SerializedName("syllabus_status") val syllabusStatus: String?
)

data class CreateCourseRequest(
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("term") val term: String
)

data class JoinCourseRequest(
    @SerializedName("join_code") val joinCode: String
)

data class EnrollmentResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("status") val status: String
)

data class NoteResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("class_date") val classDate: String,
    /** DRAFT · PROCESSING · READY · ARCHIVED; el mock no lo manda. */
    @SerializedName("status") val status: String? = null
)

data class NoteDetailResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("class_date") val classDate: String,
    @SerializedName("status") val status: String? = null,
    /** Reconstrucción IA-01 (§5.5) como JSON; null mientras no exista. */
    @SerializedName("content") val content: JsonElement? = null
)

data class CreateNoteRequest(
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("class_date") val classDate: String
)

/** `POST notes/{id}/pages`: la página ya procesada en el teléfono (issue #26). */
data class CreateNotePageRequest(
    @SerializedName("page_index") val pageIndex: Int,
    @SerializedName("perceptual_hash") val perceptualHash: String,
    @SerializedName("level_reached") val levelReached: String,
    @SerializedName("overall_confidence") val overallConfidence: Float,
    /** Regiones con bbox normalizado a 0..1 (§5.6). */
    @SerializedName("regions") val regions: JsonElement,
    @SerializedName("quality_metrics") val qualityMetrics: JsonElement? = null
)

data class NotePageResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("note_id") val noteId: Long,
    @SerializedName("page_index") val pageIndex: Int
)

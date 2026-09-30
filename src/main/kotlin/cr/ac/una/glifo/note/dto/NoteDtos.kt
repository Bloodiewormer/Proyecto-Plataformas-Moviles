package cr.ac.una.glifo.note.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

// Los nombres del JSON van en snake_case (@JsonProperty) porque así los lee la app
// (CourseDtos.kt del frontend: NoteResponse, CreateNoteRequest, …).

data class CreateNoteRequest(
    @JsonProperty("course_id") val courseId: Long,
    @JsonProperty("syllabus_topic_id") val syllabusTopicId: Long? = null,
    @JsonProperty("class_date")
    @field:NotBlank(message = "La fecha de la clase es obligatoria")
    val classDate: String,
    @field:NotBlank(message = "El título es obligatorio") val title: String
)

data class NoteResponse(
    val id: Long,
    @JsonProperty("course_id") val courseId: Long,
    @JsonProperty("syllabus_topic_id") val syllabusTopicId: Long?,
    @JsonProperty("class_date") val classDate: String,
    val title: String,
    val status: String,
    @JsonProperty("created_at") val createdAt: String
)

data class CreateNotePageRequest(
    @JsonProperty("page_index") val pageIndex: Int
)

data class NotePageResponse(
    val id: Long,
    @JsonProperty("note_id") val noteId: Long,
    @JsonProperty("page_index") val pageIndex: Int,
    @JsonProperty("storage_uri") val storageUri: String,
    @JsonProperty("level_reached") val levelReached: String,
    @JsonProperty("overall_confidence") val overallConfidence: Float,
    @JsonProperty("processed_at") val processedAt: String?
)

package cr.ac.una.glifo.note.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRawValue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import tools.jackson.databind.JsonNode

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
    @JsonProperty("created_at") val createdAt: String,
    /**
     * Reconstrucción IA-01 (notes.content, JSONB) tal cual está guardada. Solo va en el
     * detalle; en el listado queda en null para no arrastrar el documento de cada apunte.
     */
    @JsonRawValue val content: String? = null
)

/**
 * Página ya procesada en el teléfono (N0 + N1 + clasificador) que la cola offline sube
 * (issue #26). La imagen no viaja: el almacenamiento de recortes sigue abierto
 * (Glifo_UML_Modeling.md §12.4, ítem 3), así que solo se sube el resultado del pipeline.
 */
data class CreateNotePageRequest(
    @JsonProperty("page_index")
    @field:PositiveOrZero(message = "El índice de página no puede ser negativo")
    val pageIndex: Int,
    @JsonProperty("perceptual_hash")
    @field:Pattern(regexp = "^[0-9a-f]{16}$", message = "El hash perceptual debe tener 16 caracteres hexadecimales")
    val perceptualHash: String,
    @JsonProperty("level_reached") val levelReached: String = "N1",
    @JsonProperty("overall_confidence")
    @field:DecimalMin("0.0") @field:DecimalMax("1.0")
    val overallConfidence: Float = 0f,
    @JsonProperty("storage_uri") val storageUri: String? = null,
    val regions: JsonNode? = null,
    @JsonProperty("quality_metrics") val qualityMetrics: JsonNode? = null
)

data class NotePageResponse(
    val id: Long,
    @JsonProperty("note_id") val noteId: Long,
    @JsonProperty("page_index") val pageIndex: Int,
    @JsonProperty("perceptual_hash") val perceptualHash: String,
    @JsonProperty("storage_uri") val storageUri: String,
    @JsonProperty("level_reached") val levelReached: String,
    @JsonProperty("overall_confidence") val overallConfidence: Float,
    @JsonProperty("processed_at") val processedAt: String?,
    @JsonRawValue val regions: String? = null
)

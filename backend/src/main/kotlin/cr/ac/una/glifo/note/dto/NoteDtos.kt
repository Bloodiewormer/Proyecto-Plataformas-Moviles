package cr.ac.una.glifo.note.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRawValue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import tools.jackson.databind.JsonNode

// Los nombres del JSON van en snake_case (@JsonProperty) porque así los lee la app
// (CourseDtos.kt del frontend: NoteResponse, CreateNoteRequest, …).
//
// DTOs de entrada (*Request): solo los campos que el cliente puede enviar. El dueño sale
// del token, y el estado inicial, el contenido y las fechas de creación los pone el
// backend. Los @Size coinciden con el largo de cada columna de la entidad.

/** Formato ISO de las fechas de clase (AAAA-MM-DD), el mismo que manda la app. */
private const val ISO_DATE = "^\\d{4}-\\d{2}-\\d{2}$"

data class CreateNoteRequest(
    @JsonProperty("course_id")
    @field:Positive(message = "El curso es obligatorio")
    val courseId: Long,
    @JsonProperty("syllabus_topic_id")
    @field:Positive(message = "El tema del temario no es válido")
    val syllabusTopicId: Long? = null,
    @JsonProperty("class_date")
    @field:NotBlank(message = "La fecha de la clase es obligatoria")
    @field:Pattern(regexp = ISO_DATE, message = "La fecha de la clase debe tener el formato AAAA-MM-DD")
    val classDate: String,
    @field:NotBlank(message = "El título es obligatorio")
    @field:Size(max = 200, message = "El título admite como máximo 200 caracteres")
    val title: String
)

/**
 * PUT /notes/{id}: reemplaza los datos editables del apunte. El curso no cambia (un apunte
 * no se mueve de curso) y el estado tiene su propio endpoint con reglas de transición.
 */
data class UpdateNoteRequest(
    @JsonProperty("syllabus_topic_id")
    @field:Positive(message = "El tema del temario no es válido")
    val syllabusTopicId: Long? = null,
    @JsonProperty("class_date")
    @field:NotBlank(message = "La fecha de la clase es obligatoria")
    @field:Pattern(regexp = ISO_DATE, message = "La fecha de la clase debe tener el formato AAAA-MM-DD")
    val classDate: String,
    @field:NotBlank(message = "El título es obligatorio")
    @field:Size(max = 200, message = "El título admite como máximo 200 caracteres")
    val title: String
)

/** PATCH /notes/{id}/status: solo cambia el estado del ciclo de vida (NoteStatus). */
data class UpdateNoteStatusRequest(
    @field:NotBlank(message = "El estado es obligatorio")
    @field:Pattern(
        regexp = "^(DRAFT|PROCESSING|READY|ARCHIVED)$",
        message = "El estado debe ser DRAFT, PROCESSING, READY o ARCHIVED"
    )
    val status: String
)

// DTO de salida.

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
    @JsonProperty("level_reached")
    @field:Pattern(
        regexp = "^(N0|N1|N1_5|N2|N3|UNRESOLVED)$",
        message = "El nivel de procesamiento debe ser N0, N1, N1_5, N2, N3 o UNRESOLVED"
    )
    val levelReached: String = "N1",
    @JsonProperty("overall_confidence")
    @field:DecimalMin(value = "0.0", message = "La confianza debe estar entre 0 y 1")
    @field:DecimalMax(value = "1.0", message = "La confianza debe estar entre 0 y 1")
    val overallConfidence: Float = 0f,
    @JsonProperty("storage_uri")
    @field:Size(max = 512, message = "La ubicación de la imagen admite como máximo 512 caracteres")
    val storageUri: String? = null,
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

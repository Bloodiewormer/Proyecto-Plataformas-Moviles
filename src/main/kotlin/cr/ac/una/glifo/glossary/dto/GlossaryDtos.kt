package cr.ac.una.glifo.glossary.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant

// JSON en snake_case: CourseGlossaryResponse y GlossaryEntryResponse son las mismas que lee
// la app (GlossaryDtos.kt del frontend) y que devolvía Mockoon en courses/:id/glossary.
// Los @Size coinciden con el largo de cada columna, así un texto largo es un 400 y no un
// error de base de datos.

// ---------- Entrada ----------

/** POST /courses/{id}/glossary: el docente agrega una entrada al glosario canónico. */
data class CreateGlossaryEntryRequest(
    @field:NotBlank(message = "El término es obligatorio")
    @field:Size(max = 120, message = "El término admite como máximo 120 caracteres")
    val term: String,
    @JsonProperty("canonical_form")
    @field:NotBlank(message = "La forma canónica es obligatoria")
    @field:Size(max = 255, message = "La forma canónica admite como máximo 255 caracteres")
    val canonicalForm: String,
    /** Coincide con el CHECK ck_course_glossary_kind. */
    @field:Pattern(regexp = "TERM|SYMBOL|NOTATION", message = "El tipo debe ser TERM, SYMBOL o NOTATION")
    val kind: String = "TERM"
)

/** POST /courses/{id}/glossary/suggestions: el estudiante propone una corrección (D-15). */
data class CreateGlossarySuggestionRequest(
    @JsonProperty("original_text")
    @field:NotBlank(message = "El texto original es obligatorio")
    @field:Size(max = 255, message = "El texto original admite como máximo 255 caracteres")
    val originalText: String,
    @JsonProperty("suggested_correction")
    @field:NotBlank(message = "La corrección sugerida es obligatoria")
    @field:Size(max = 255, message = "La corrección sugerida admite como máximo 255 caracteres")
    val suggestedCorrection: String,
    /** Apunte donde el estudiante hizo la corrección, si la hizo desde uno. */
    @JsonProperty("note_id")
    @field:Positive(message = "El apunte no es válido")
    val noteId: Long? = null
)

/** PATCH /courses/{id}/glossary/suggestions/{sid}: el docente aprueba o rechaza. */
data class ResolveGlossarySuggestionRequest(
    @field:NotBlank(message = "La decisión es obligatoria")
    @field:Pattern(regexp = "APPROVED|REJECTED", message = "La decisión debe ser APPROVED o REJECTED")
    val status: String,
    /** Tipo con que entra al glosario si se aprueba. */
    @field:Pattern(regexp = "TERM|SYMBOL|NOTATION", message = "El tipo debe ser TERM, SYMBOL o NOTATION")
    val kind: String = "TERM"
)

// ---------- Salida ----------

data class CourseGlossaryResponse(
    @JsonProperty("course_id") val courseId: Long,
    @JsonProperty("course_name") val courseName: String,
    @JsonProperty("syllabus_version") val syllabusVersion: Int,
    @JsonProperty("glossary_version") val glossaryVersion: Int,
    val entries: List<GlossaryEntryResponse>
)

data class GlossaryEntryResponse(
    val id: Long,
    val term: String,
    @JsonProperty("canonical_form") val canonicalForm: String,
    val kind: String
)

data class GlossarySuggestionResponse(
    val id: Long,
    @JsonProperty("course_id") val courseId: Long,
    /** Nombre de quien sugirió, para que el docente sepa de dónde viene. */
    @JsonProperty("suggested_by") val suggestedBy: String,
    @JsonProperty("note_id") val noteId: Long?,
    @JsonProperty("original_text") val originalText: String,
    @JsonProperty("suggested_correction") val suggestedCorrection: String,
    val status: String,
    @JsonProperty("created_at") val createdAt: Instant,
    @JsonProperty("resolved_at") val resolvedAt: Instant?
)

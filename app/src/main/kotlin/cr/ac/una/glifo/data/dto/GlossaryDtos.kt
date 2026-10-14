package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName

/** `GET courses/{id}/glossary`: glosario canónico del curso con sus versiones (D-17). */
data class CourseGlossaryResponse(
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("course_name") val courseName: String,
    @SerializedName("syllabus_version") val syllabusVersion: Int,
    @SerializedName("glossary_version") val glossaryVersion: Int,
    @SerializedName("entries") val entries: List<GlossaryEntryResponse>
)

data class GlossaryEntryResponse(
    @SerializedName("term") val term: String,
    @SerializedName("canonical_form") val canonicalForm: String,
    @SerializedName("kind") val kind: String,
    /** El backend real lo manda; Mockoon no, por eso es opcional. */
    @SerializedName("id") val id: Long? = null
)

/** `POST courses/{id}/glossary`: el docente agrega una entrada (issue #33). */
data class CreateGlossaryEntryRequest(
    @SerializedName("term") val term: String,
    @SerializedName("canonical_form") val canonicalForm: String,
    @SerializedName("kind") val kind: String
)

/** `GET courses/{id}/glossary/suggestions`: correcciones de estudiantes pendientes (D-15). */
data class GlossarySuggestionResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("suggested_by") val suggestedBy: String,
    @SerializedName("note_id") val noteId: Long?,
    @SerializedName("original_text") val originalText: String,
    @SerializedName("suggested_correction") val suggestedCorrection: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("resolved_at") val resolvedAt: String?
)

/** `PATCH courses/{id}/glossary/suggestions/{sid}`: APPROVED o REJECTED. */
data class ResolveGlossarySuggestionRequest(
    @SerializedName("status") val status: String,
    @SerializedName("kind") val kind: String = "TERM"
)

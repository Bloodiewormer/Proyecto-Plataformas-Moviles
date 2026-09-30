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
    @SerializedName("kind") val kind: String
)

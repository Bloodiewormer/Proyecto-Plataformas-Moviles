package cr.ac.una.glifo.data.dto

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Cuerpo de petición para `POST courses/{courseId}/study-items/generate` (IA-02, #44).
 */
data class GenerateStudyItemsRequest(
    @SerializedName("topic_ids") val topicIds: List<Long>,
    @SerializedName("items_per_topic") val itemsPerTopic: Int = 3,
    @SerializedName("regenerate") val regenerate: Boolean = false
)

/**
 * Respuesta de `study_items` (IA-02, #44).
 * [kind] ∈ `FLASHCARD`, `MULTIPLE_CHOICE`, `TRUE_FALSE`.
 * [payload] es un objeto JSON en camelCase despachado manualmente según [kind] (D-19).
 */
data class StudyItemResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("syllabus_topic_id") val syllabusTopicId: Long,
    @SerializedName("kind") val kind: String,
    @SerializedName("payload") val payload: JsonObject,
    @SerializedName("created_at") val createdAt: String? = null
)

data class FlashcardDto(
    @SerializedName("id") val id: Long,
    @SerializedName("topic_label") val topicLabel: String,
    @SerializedName("prompt") val prompt: String,
    @SerializedName("answer") val answer: String,
    @SerializedName("source_note") val sourceNote: String
)

data class QuizQuestionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("question") val question: String,
    @SerializedName("options") val options: List<String>,
    @SerializedName("correct_option_index") val correctOptionIndex: Int,
    @SerializedName("explanation") val explanation: String
)

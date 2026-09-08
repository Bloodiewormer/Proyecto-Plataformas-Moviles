package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName

data class StudyItemResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("syllabus_topic_id") val syllabusTopicId: Long,
    @SerializedName("kind") val kind: String,
    @SerializedName("payload") val payload: String,
    @SerializedName("created_at") val createdAt: String
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

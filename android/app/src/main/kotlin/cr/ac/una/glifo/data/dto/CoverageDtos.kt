package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs para los endpoints reales de cobertura del temario (#43):
 * - `GET /api/v1/courses/{courseId}/coverage`
 * - `POST /api/v1/courses/{courseId}/coverage/evaluate`
 * - `GET /api/v1/courses/{courseId}/coverage/delta?since=`
 */
data class CoverageResponseDto(
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("coverage_pct") val coveragePct: Float = 0f,
    @SerializedName("state_counts") val stateCounts: Map<String, Int> = emptyMap(),
    @SerializedName("topics") val topics: List<TopicCoverageResponseDto> = emptyList()
)

data class TopicCoverageResponseDto(
    @SerializedName("topic_id") val topicId: Long,
    @SerializedName("parent_id") val parentId: Long? = null,
    @SerializedName("code") val code: String,
    @SerializedName("title") val title: String,
    @SerializedName("state") val state: String? = null,
    @SerializedName("score") val score: Float = 0f,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CoverageDeltaResponseDto(
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("since") val since: String,
    @SerializedName("changed_counts") val changedCounts: Map<String, Int> = emptyMap(),
    @SerializedName("changed_topics") val changedTopics: List<TopicCoverageResponseDto> = emptyList()
)

/** GET courses/{id}/coverage/gaps (docente dueño): brechas del grupo por tema. */
data class CourseGapsResponseDto(
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("enrolled_students") val enrolledStudents: Int = 0,
    @SerializedName("coverage_pct") val coveragePct: Float = 0f,
    @SerializedName("topics") val topics: List<TopicGapDto> = emptyList()
)

data class TopicGapDto(
    @SerializedName("topic_id") val topicId: Long,
    @SerializedName("code") val code: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("students_with_gap") val studentsWithGap: Int = 0,
    @SerializedName("solid_count") val solidCount: Int = 0,
    @SerializedName("partial_count") val partialCount: Int = 0,
    @SerializedName("absent_count") val absentCount: Int = 0,
    @SerializedName("uncertain_count") val uncertainCount: Int = 0,
    @SerializedName("average_pct") val averagePct: Float = 0f
)

package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName

data class CoverageSummaryResponse(
    @SerializedName("topic_count") val topicCount: Int,
    @SerializedName("coverage_percent") val coveragePercent: Int,
    @SerializedName("v") val v: Float,
    @SerializedName("r") val r: Float,
    @SerializedName("e") val e: Float,
    @SerializedName("u") val u: Float,
    @SerializedName("solid_count") val solidCount: Int,
    @SerializedName("partial_count") val partialCount: Int,
    @SerializedName("absent_count") val absentCount: Int,
    @SerializedName("doubtful_count") val doubtfulCount: Int,
    @SerializedName("unit_label") val unitLabel: String,
    @SerializedName("delta_label") val deltaLabel: String
)

data class CoverageTopicResponse(
    @SerializedName("state") val state: String,
    @SerializedName("name") val name: String,
    @SerializedName("note") val note: String?,
    @SerializedName("score") val score: String
)

data class TopicDetailResponse(
    @SerializedName("name") val name: String,
    @SerializedName("state") val state: String,
    @SerializedName("state_label") val stateLabel: String,
    @SerializedName("confidence") val confidence: String,
    @SerializedName("checklist") val checklist: List<TopicChecklistItemDto> = emptyList(),
    @SerializedName("note_refs") val noteRefs: List<TopicNoteRefDto> = emptyList()
)

data class TopicChecklistItemDto(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("label") val label: String,
    @SerializedName("meta") val meta: String
)

data class TopicNoteRefDto(
    @SerializedName("note_id") val noteId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("meta") val meta: String
)

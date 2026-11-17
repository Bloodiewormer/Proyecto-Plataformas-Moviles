package cr.ac.una.glifo.domain.model

import cr.ac.una.glifo.core.common.UiText

data class CoverageTopic(
    val state: ConfidenceState,
    val name: String,
    val note: String?,
    val score: String,
    val topicId: Long = 0L,
    val code: String = "",
    val title: String = name,
    val updatedAt: String? = null,
    val noteText: UiText? = note?.let(UiText::Plain)
)

data class CoverageSummary(
    val topicCount: Int,
    val coveragePercent: Int,
    val v: Float,
    val r: Float,
    val e: Float,
    val u: Float,
    val solidCount: Int,
    val partialCount: Int,
    val absentCount: Int,
    val doubtfulCount: Int,
    val unitLabel: String,
    val deltaLabel: String,
    val unitText: UiText = UiText.Plain(unitLabel),
    val deltaText: UiText = UiText.Plain(deltaLabel)
)

data class TopicChecklistItem(val ok: Boolean, val label: String, val meta: String)
data class TopicNoteRef(val noteId: Long, val title: String, val meta: String)

data class TopicDetail(
    val name: String,
    val state: ConfidenceState,
    val stateLabel: String,
    val confidence: String,
    val checklist: List<TopicChecklistItem>,
    val noteRefs: List<TopicNoteRef>,
    val stateLabelText: UiText = UiText.Plain(stateLabel)
)

data class CovBarValues(val v: Float, val r: Float, val e: Float, val u: Float)
data class CoverageImprovement(val label: String, val from: ConfidenceState, val to: ConfidenceState)
data class CoverageMissing(val label: String, val state: ConfidenceState, val meta: String, val needsReview: Boolean)

data class CoverageDeltaInfo(
    val dateRange: String,
    val fromPercent: Int = 0,
    val toPercent: Int = 0,
    val fromBar: CovBarValues = CovBarValues(0f, 0f, 0f, 0f),
    val toBar: CovBarValues = CovBarValues(0f, 0f, 0f, 0f),
    val deltaPoints: Int = 0,
    val newNotesCount: Int = 0,
    val newPagesCount: Int = 0,
    val improved: List<CoverageImprovement> = emptyList(),
    val stillMissing: List<CoverageMissing> = emptyList(),
    val sinceIso: String = "",
    val sinceDisplay: String = dateRange,
    val changedCounts: Map<String, Int> = emptyMap(),
    val changedTopics: List<CoverageTopic> = emptyList(),
    val dateRangeText: UiText = UiText.Plain(dateRange)
)

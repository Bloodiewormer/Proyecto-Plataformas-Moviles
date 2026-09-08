package cr.ac.una.glifo.domain.model

data class CoverageTopic(val state: ConfidenceState, val name: String, val note: String?, val score: String)

data class CoverageSummary(
    val topicCount: Int,
    val coveragePercent: Int,
    val v: Float, val r: Float, val e: Float, val u: Float,
    val solidCount: Int,
    val partialCount: Int,
    val absentCount: Int,
    val doubtfulCount: Int,
    val unitLabel: String,
    val deltaLabel: String
)

data class TopicChecklistItem(val ok: Boolean, val label: String, val meta: String)
data class TopicNoteRef(val noteId: Long, val title: String, val meta: String)

data class TopicDetail(
    val name: String,
    val state: ConfidenceState,
    val stateLabel: String,
    val confidence: String,
    val checklist: List<TopicChecklistItem>,
    val noteRefs: List<TopicNoteRef>
)

data class CovBarValues(val v: Float, val r: Float, val e: Float, val u: Float)
data class CoverageImprovement(val label: String, val from: ConfidenceState, val to: ConfidenceState)
data class CoverageMissing(val label: String, val state: ConfidenceState, val meta: String, val needsReview: Boolean)

data class CoverageDeltaInfo(
    val dateRange: String,
    val fromPercent: Int,
    val toPercent: Int,
    val fromBar: CovBarValues,
    val toBar: CovBarValues,
    val deltaPoints: Int,
    val newNotesCount: Int,
    val newPagesCount: Int,
    val improved: List<CoverageImprovement>,
    val stillMissing: List<CoverageMissing>
)

package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.CoverageSummaryResponse
import cr.ac.una.glifo.data.dto.CoverageTopicResponse
import cr.ac.una.glifo.data.dto.TopicChecklistItemDto
import cr.ac.una.glifo.data.dto.TopicDetailResponse
import cr.ac.una.glifo.data.dto.TopicNoteRefDto
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.CoverageSummary
import cr.ac.una.glifo.domain.model.CoverageTopic
import cr.ac.una.glifo.domain.model.TopicChecklistItem
import cr.ac.una.glifo.domain.model.TopicDetail
import cr.ac.una.glifo.domain.model.TopicNoteRef

// DTO -> Domain
fun CoverageSummaryResponse.toDomain(): CoverageSummary = CoverageSummary(
    topicCount = topicCount,
    coveragePercent = coveragePercent,
    v = v,
    r = r,
    e = e,
    u = u,
    solidCount = solidCount,
    partialCount = partialCount,
    absentCount = absentCount,
    doubtfulCount = doubtfulCount,
    unitLabel = unitLabel,
    deltaLabel = deltaLabel
)

fun CoverageTopicResponse.toDomain(): CoverageTopic {
    val confidenceState = when (state.uppercase()) {
        "VERIFIED" -> ConfidenceState.Verified
        "REPAIRED" -> ConfidenceState.Repaired
        "ESCALATED" -> ConfidenceState.Escalated
        else -> ConfidenceState.Uncertain
    }
    return CoverageTopic(
        state = confidenceState,
        name = name,
        note = note,
        score = score
    )
}

fun TopicDetailResponse.toDomain(): TopicDetail {
    val confidenceState = when (state.uppercase()) {
        "VERIFIED" -> ConfidenceState.Verified
        "REPAIRED" -> ConfidenceState.Repaired
        "ESCALATED" -> ConfidenceState.Escalated
        else -> ConfidenceState.Uncertain
    }
    return TopicDetail(
        name = name,
        state = confidenceState,
        stateLabel = stateLabel,
        confidence = confidence,
        checklist = checklist.map { it.toDomain() },
        noteRefs = noteRefs.map { it.toDomain() }
    )
}

fun TopicChecklistItemDto.toDomain(): TopicChecklistItem = TopicChecklistItem(
    ok = ok,
    label = label,
    meta = meta
)

fun TopicNoteRefDto.toDomain(): TopicNoteRef = TopicNoteRef(
    noteId = noteId,
    title = title,
    meta = meta
)

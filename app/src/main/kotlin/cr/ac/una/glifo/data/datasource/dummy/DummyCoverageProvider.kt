package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.CovBarValues
import cr.ac.una.glifo.domain.model.CoverageDeltaInfo
import cr.ac.una.glifo.domain.model.CoverageImprovement
import cr.ac.una.glifo.domain.model.CoverageMissing
import cr.ac.una.glifo.domain.model.CoverageSummary
import cr.ac.una.glifo.domain.model.CoverageTopic
import cr.ac.una.glifo.domain.model.TopicChecklistItem
import cr.ac.una.glifo.domain.model.TopicDetail
import cr.ac.una.glifo.domain.model.TopicNoteRef

/** No DTO counterpart yet (coverage math is meant to run through `engine/coverage/` once
 * that lands) — emits domain models directly, same rationale as [DummyCourseProvider]'s stats. */
object DummyCoverageProvider {

    fun getSummary(courseId: Long): CoverageSummary {
        val stats = DummyCourseProvider.getStats(courseId)
        return CoverageSummary(
            topicCount = if (stats.topicCount > 0) stats.topicCount else 33,
            coveragePercent = stats.coveragePercent,
            v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain,
            solidCount = 18, partialCount = 7, absentCount = 5, doubtfulCount = 3,
            unitLabel = "UNIDAD 3 · SERIES",
            deltaLabel = "+2 temas desde el 8 de agosto"
        )
    }

    private val topics = listOf(
        CoverageTopic(ConfidenceState.Verified, "3.1 Sucesiones", null, "0.92"),
        CoverageTopic(ConfidenceState.Verified, "3.2 Series geométricas", null, "0.88"),
        CoverageTopic(ConfidenceState.Repaired, "3.3 Comparación", "Falta el caso del límite", "0.54"),
        CoverageTopic(ConfidenceState.Escalated, "3.4 Series de potencias", "Sin evidencia en tus apuntes", "0.11"),
        CoverageTopic(ConfidenceState.Uncertain, "3.5 Radio de convergencia", "Apunte ilegible en esa sección", "—")
    )

    fun getTopics(courseId: Long): List<CoverageTopic> = topics

    private val topicDetails = listOf(
        TopicDetail(
            name = "3.3 Comparación",
            state = ConfidenceState.Repaired,
            stateLabel = "PARCIAL",
            confidence = "0.54",
            checklist = listOf(
                TopicChecklistItem(true, "Enunciado del criterio", "2 apuntes"),
                TopicChecklistItem(true, "Ejemplo resuelto", "1 apunte"),
                TopicChecklistItem(false, "Criterio de comparación en el límite", "sin evidencia"),
                TopicChecklistItem(false, "Condición de convergencia", "sin evidencia")
            ),
            noteRefs = listOf(
                TopicNoteRef(2L, "Criterio de comparación", "7 ago · página 2"),
                TopicNoteRef(3L, "Series de potencias", "5 ago · página 1")
            )
        ),
        TopicDetail(
            name = "3.4 Series de potencias",
            state = ConfidenceState.Escalated,
            stateLabel = "AUSENTE",
            confidence = "0.11",
            checklist = listOf(
                TopicChecklistItem(false, "Enunciado general", "sin evidencia"),
                TopicChecklistItem(false, "Radio de convergencia", "sin evidencia"),
                TopicChecklistItem(false, "Ejemplo resuelto", "sin evidencia")
            ),
            noteRefs = emptyList()
        ),
        TopicDetail(
            name = "3.5 Radio de convergencia",
            state = ConfidenceState.Uncertain,
            stateLabel = "DUDOSO",
            confidence = "—",
            checklist = listOf(
                TopicChecklistItem(true, "Hay apunte del tema", "1 apunte"),
                TopicChecklistItem(false, "Apunte legible", "ilegible")
            ),
            noteRefs = listOf(TopicNoteRef(3L, "Series de potencias", "2 ago · página 3"))
        )
    )

    fun getTopicDetail(name: String): TopicDetail = topicDetails.find { it.name == name } ?: topicDetails.first()

    fun getDelta(): CoverageDeltaInfo = CoverageDeltaInfo(
        dateRange = "Del 8 al 15 de agosto",
        fromPercent = 62,
        toPercent = 68,
        fromBar = CovBarValues(48f, 18f, 20f, 14f),
        toBar = CovBarValues(54f, 20f, 14f, 12f),
        deltaPoints = 6,
        newNotesCount = 4,
        newPagesCount = 11,
        improved = listOf(
            CoverageImprovement("3.2 Series geométricas", ConfidenceState.Escalated, ConfidenceState.Verified),
            CoverageImprovement("3.3 Comparación", ConfidenceState.Escalated, ConfidenceState.Repaired),
            CoverageImprovement("2.4 Integrales impropias", ConfidenceState.Repaired, ConfidenceState.Verified)
        ),
        stillMissing = listOf(
            CoverageMissing("3.4 Series de potencias", ConfidenceState.Escalated, "Sin cambios desde el 29 de julio", needsReview = false),
            CoverageMissing("3.5 Radio de convergencia", ConfidenceState.Uncertain, "Hay apunte, pero ilegible", needsReview = true)
        )
    )
}

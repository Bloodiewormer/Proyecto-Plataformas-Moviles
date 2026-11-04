package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.CoverageDeltaResponseDto
import cr.ac.una.glifo.data.dto.CoverageResponseDto
import cr.ac.una.glifo.data.dto.TopicCoverageResponseDto
import cr.ac.una.glifo.domain.model.ConfidenceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverageMapperTest {

    @Test
    fun `maps all four backend coverage states to ConfidenceState`() {
        assertEquals(ConfidenceState.Verified, CoverageMapper.mapState("SOLID"))
        assertEquals(ConfidenceState.Repaired, CoverageMapper.mapState("PARTIAL"))
        assertEquals(ConfidenceState.Escalated, CoverageMapper.mapState("ABSENT"))
        assertEquals(ConfidenceState.Uncertain, CoverageMapper.mapState("UNCERTAIN"))
        assertEquals(ConfidenceState.Uncertain, CoverageMapper.mapState(null))
    }

    @Test
    fun `toSummary and toTopics map state counts, percentages and leaf topics excluding parent containers`() {
        val dto = CoverageResponseDto(
            courseId = 1L,
            coveragePct = 55.0f,
            stateCounts = mapOf(
                "SOLID" to 2,
                "PARTIAL" to 1,
                "ABSENT" to 1,
                "UNCERTAIN" to 1
            ),
            topics = listOf(
                TopicCoverageResponseDto(
                    topicId = 100L,
                    parentId = null,
                    code = "U1",
                    title = "Límites y continuidad",
                    state = null,
                    score = 0.55f,
                    updatedAt = "2026-10-28T18:30:00Z"
                ),
                TopicCoverageResponseDto(
                    topicId = 101L,
                    parentId = 100L,
                    code = "U1.1",
                    title = "Definición formal de límite",
                    state = "SOLID",
                    score = 0.92f,
                    updatedAt = "2026-10-28T18:30:00Z"
                ),
                TopicCoverageResponseDto(
                    topicId = 102L,
                    parentId = 100L,
                    code = "U1.2",
                    title = "Límites laterales",
                    state = "PARTIAL",
                    score = 0.65f,
                    updatedAt = "2026-10-28T18:30:00Z"
                ),
                TopicCoverageResponseDto(
                    topicId = 103L,
                    parentId = 100L,
                    code = "U1.3",
                    title = "Continuidad en un punto",
                    state = "UNCERTAIN",
                    score = 0.30f,
                    updatedAt = "2026-10-28T18:30:00Z"
                ),
                TopicCoverageResponseDto(
                    topicId = 104L,
                    parentId = 100L,
                    code = "U1.4",
                    title = "Teorema del valor intermedio",
                    state = "ABSENT",
                    score = 0.0f,
                    updatedAt = null
                )
            )
        )

        val summary = dto.toSummary()
        assertEquals(5, summary.topicCount)
        assertEquals(55, summary.coveragePercent)
        assertEquals(2, summary.solidCount)
        assertEquals(1, summary.partialCount)
        assertEquals(1, summary.absentCount)
        assertEquals(1, summary.doubtfulCount)
        assertEquals(40f, summary.v, 0.01f)
        assertEquals(20f, summary.r, 0.01f)
        assertEquals(20f, summary.e, 0.01f)
        assertEquals(20f, summary.u, 0.01f)

        val topics = dto.toTopics()
        assertEquals(4, topics.size)
        assertEquals(ConfidenceState.Verified, topics[0].state)
        assertEquals("U1.1 Definición formal de límite", topics[0].name)
        assertEquals("0.92", topics[0].score)
        assertEquals(ConfidenceState.Repaired, topics[1].state)
        assertEquals(ConfidenceState.Uncertain, topics[2].state)
        assertEquals(ConfidenceState.Escalated, topics[3].state)
    }

    @Test
    fun `toDeltaInfo maps changed topics and handles empty delta without inventing percentages`() {
        val deltaDto = CoverageDeltaResponseDto(
            courseId = 1L,
            since = "2026-10-21T00:00:00Z",
            changedCounts = mapOf("SOLID" to 1, "PARTIAL" to 1, "ABSENT" to 0, "UNCERTAIN" to 0),
            changedTopics = listOf(
                TopicCoverageResponseDto(
                    topicId = 101L,
                    parentId = 100L,
                    code = "U1.1",
                    title = "Definición formal de límite",
                    state = "SOLID",
                    score = 0.90f,
                    updatedAt = "2026-10-28T18:35:00Z"
                )
            )
        )

        val deltaInfo = deltaDto.toDeltaInfo()
        assertTrue(deltaInfo.changedTopics.isNotEmpty())
        assertEquals("2026-10-21T00:00:00Z", deltaInfo.sinceIso)
        assertEquals("2026-10-21", deltaInfo.sinceDisplay)
        assertEquals(1, deltaInfo.changedTopics.size)
        assertEquals(0, deltaInfo.fromPercent)
        assertEquals(0, deltaInfo.toPercent)

        val emptyDelta = CoverageDeltaResponseDto(
            courseId = 1L,
            since = "2026-10-28T00:00:00Z",
            changedCounts = emptyMap(),
            changedTopics = emptyList()
        ).toDeltaInfo()

        assertFalse(emptyDelta.changedTopics.isNotEmpty())
        assertTrue(emptyDelta.changedTopics.isEmpty())
    }
}

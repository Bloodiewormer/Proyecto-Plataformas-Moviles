package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiText
import cr.ac.una.glifo.data.dto.CoverageDeltaResponseDto
import cr.ac.una.glifo.data.dto.CoverageResponseDto
import cr.ac.una.glifo.data.dto.TopicCoverageResponseDto
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.CoverageTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class CoverageMapperTest {

    @Test
    fun `maps all four backend coverage states to ConfidenceState`() {
        assertEquals(ConfidenceState.Verified, CoverageMapper.mapState("SOLID"))
        assertEquals(ConfidenceState.Repaired, CoverageMapper.mapState("PARTIAL"))
        assertEquals(ConfidenceState.Escalated, CoverageMapper.mapState("ABSENT"))
        assertEquals(ConfidenceState.Uncertain, CoverageMapper.mapState("UNCERTAIN"))
        assertEquals(ConfidenceState.Uncertain, CoverageMapper.mapState(null))
        assertEquals(R.string.coverage_state_solid, CoverageMapper.stateLabelRes(ConfidenceState.Verified))
        assertEquals(R.string.coverage_state_partial, CoverageMapper.stateLabelRes(ConfidenceState.Repaired))
        assertEquals(R.string.coverage_state_absent, CoverageMapper.stateLabelRes(ConfidenceState.Escalated))
        assertEquals(R.string.coverage_state_uncertain, CoverageMapper.stateLabelRes(ConfidenceState.Uncertain))
    }

    @Test
    fun `toSummary and toTopics map state counts, percentages, localized UiText and leaf topics`() {
        val zone = ZoneId.of("America/Costa_Rica")
        val locale = Locale.US
        val expectedUpdated = LocalDate.of(2026, 10, 28)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

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

        val summary = dto.toSummary(zoneId = zone, locale = locale)
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
        assertEquals(UiText.Plain("U1 · LÍMITES Y CONTINUIDAD"), summary.unitText)
        assertEquals(
            UiText.Resource(R.string.coverage_delta_latest_update, expectedUpdated),
            summary.deltaText
        )

        val noParentDto = dto.copy(topics = dto.topics.drop(1).map { it.copy(updatedAt = null) })
        val noParentSummary = noParentDto.toSummary(zoneId = zone, locale = locale)
        assertEquals(UiText.Resource(R.string.coverage_unit_default), noParentSummary.unitText)
        assertEquals(UiText.Resource(R.string.coverage_delta_view_recent), noParentSummary.deltaText)

        val topics = dto.toTopics(zoneId = zone, locale = locale)
        assertEquals(4, topics.size)
        assertEquals(ConfidenceState.Verified, topics[0].state)
        assertEquals("U1.1 Definición formal de límite", topics[0].name)
        assertEquals("0.92", topics[0].score)
        assertEquals(
            UiText.Resource(R.string.coverage_topic_updated_at, expectedUpdated),
            topics[0].noteText
        )
        assertEquals(ConfidenceState.Repaired, topics[1].state)
        assertEquals(ConfidenceState.Uncertain, topics[2].state)
        assertEquals(ConfidenceState.Escalated, topics[3].state)
        assertEquals(null, topics[3].noteText)
    }

    @Test
    fun `toDeltaInfo formats since date using supplied timezone and locale`() {
        val costaRica = ZoneId.of("America/Costa_Rica")
        val utc = ZoneId.of("UTC")
        val esLocale = Locale.forLanguageTag("es")
        val enLocale = Locale.US

        val deltaDto = CoverageDeltaResponseDto(
            courseId = 1L,
            since = "2026-10-21T02:30:00Z",
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

        val crInfo = deltaDto.toDeltaInfo(zoneId = costaRica, locale = enLocale)
        val expectedCr = LocalDate.of(2026, 10, 20)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(enLocale))
        assertTrue(crInfo.changedTopics.isNotEmpty())
        assertEquals("2026-10-21T02:30:00Z", crInfo.sinceIso)
        assertEquals(expectedCr, crInfo.sinceDisplay)
        assertEquals(UiText.Resource(R.string.coverage_delta_since, expectedCr), crInfo.dateRangeText)
        assertEquals(1, crInfo.changedTopics.size)
        assertEquals(0, crInfo.fromPercent)
        assertEquals(0, crInfo.toPercent)

        val utcEsInfo = deltaDto.toDeltaInfo(zoneId = utc, locale = esLocale)
        val expectedUtcEs = LocalDate.of(2026, 10, 21)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(esLocale))
        assertEquals(expectedUtcEs, utcEsInfo.sinceDisplay)
        assertEquals(UiText.Resource(R.string.coverage_delta_since, expectedUtcEs), utcEsInfo.dateRangeText)

        val detail = CoverageMapper.toTopicDetail(
            CoverageTopic(state = ConfidenceState.Repaired, name = "U1.2", note = null, score = "0.65")
        )
        assertEquals(UiText.Resource(R.string.coverage_state_partial), detail.stateLabelText)

        val emptyDelta = CoverageDeltaResponseDto(
            courseId = 1L,
            since = "2026-10-28T00:00:00Z",
            changedCounts = emptyMap(),
            changedTopics = emptyList()
        ).toDeltaInfo(zoneId = utc, locale = enLocale)

        assertFalse(emptyDelta.changedTopics.isNotEmpty())
        assertTrue(emptyDelta.changedTopics.isEmpty())
    }
}

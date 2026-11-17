package cr.ac.una.glifo.data.mapper

import androidx.annotation.StringRes
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiText
import cr.ac.una.glifo.data.dto.CoverageDeltaResponseDto
import cr.ac.una.glifo.data.dto.CoverageResponseDto
import cr.ac.una.glifo.data.dto.TopicCoverageResponseDto
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.CovBarValues
import cr.ac.una.glifo.domain.model.CoverageDeltaInfo
import cr.ac.una.glifo.domain.model.CoverageSummary
import cr.ac.una.glifo.domain.model.CoverageTopic
import cr.ac.una.glifo.domain.model.TopicDetail
import cr.ac.una.glifo.domain.model.TopicNoteRef
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Mapeo del contrato real de cobertura (#43):
 * - `SOLID -> ConfidenceState.Verified`
 * - `PARTIAL -> ConfidenceState.Repaired`
 * - `ABSENT -> ConfidenceState.Escalated`
 * - `UNCERTAIN -> ConfidenceState.Uncertain`
 */
object CoverageMapper {

    /**
     * El backend manda fechas UTC (2026-11-10T00:59:00Z). Cortarlas con take(10) mostraba el día
     * en UTC: de noche en Costa Rica salía la fecha de mañana. Se pasa a la zona y locale del teléfono.
     */
    private fun String.toLocalDisplayDate(
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String = runCatching {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val localDate = runCatching {
            Instant.parse(this).atZone(zoneId).toLocalDate()
        }.recoverCatching {
            OffsetDateTime.parse(this).atZoneSameInstant(zoneId).toLocalDate()
        }.recoverCatching {
            LocalDate.parse(take(10))
        }.getOrThrow()
        localDate.format(formatter)
    }.getOrElse { take(10) }

    fun mapState(state: String?): ConfidenceState = when (state?.uppercase(Locale.ROOT)) {
        "SOLID" -> ConfidenceState.Verified
        "PARTIAL" -> ConfidenceState.Repaired
        "ABSENT" -> ConfidenceState.Escalated
        "UNCERTAIN" -> ConfidenceState.Uncertain
        else -> ConfidenceState.Uncertain
    }

    @StringRes
    fun stateLabelRes(state: ConfidenceState): Int = when (state) {
        ConfidenceState.Verified -> R.string.coverage_state_solid
        ConfidenceState.Repaired -> R.string.coverage_state_partial
        ConfidenceState.Escalated -> R.string.coverage_state_absent
        ConfidenceState.Uncertain -> R.string.coverage_state_uncertain
    }

    fun stateLabelText(state: ConfidenceState): UiText = UiText.Resource(stateLabelRes(state))

    fun stateLabel(state: ConfidenceState): String = when (state) {
        ConfidenceState.Verified -> "SÓLIDO"
        ConfidenceState.Repaired -> "PARCIAL"
        ConfidenceState.Escalated -> "AUSENTE"
        ConfidenceState.Uncertain -> "INCIERTO"
    }

    fun toSummary(
        dto: CoverageResponseDto,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): CoverageSummary {
        val solid = dto.stateCounts["SOLID"] ?: 0
        val partial = dto.stateCounts["PARTIAL"] ?: 0
        val absent = dto.stateCounts["ABSENT"] ?: 0
        val uncertain = dto.stateCounts["UNCERTAIN"] ?: 0
        val leafTotal = solid + partial + absent + uncertain
        val totalTopics = if (leafTotal > 0) leafTotal else dto.topics.count { it.state != null }

        val v = if (totalTopics == 0) 0f else solid * 100f / totalTopics
        val r = if (totalTopics == 0) 0f else partial * 100f / totalTopics
        val e = if (totalTopics == 0) 0f else absent * 100f / totalTopics
        val u = if (totalTopics == 0) 0f else uncertain * 100f / totalTopics

        val normalizedPct = if (dto.coveragePct in 0.0001f..1.0f && (solid + partial) > 0 && ((solid * 100f) / totalTopics.coerceAtLeast(1)) > 5f) {
            (dto.coveragePct * 100f).roundToInt()
        } else {
            dto.coveragePct.roundToInt()
        }

        val firstParent = dto.topics.firstOrNull { it.state == null }
        val unitLabel = if (firstParent != null) {
            "${firstParent.code} · ${firstParent.title}".uppercase(locale)
        } else {
            "TEMARIO DEL CURSO"
        }
        val unitText = if (firstParent != null) {
            UiText.Plain(unitLabel)
        } else {
            UiText.Resource(R.string.coverage_unit_default)
        }

        val latestUpdate = dto.topics.mapNotNull { it.updatedAt }.maxOrNull()?.toLocalDisplayDate(zoneId, locale)
        val deltaLabel = if (latestUpdate != null) {
            "Última actualización: $latestUpdate · Ver cambios"
        } else {
            "Ver cambios recientes del temario"
        }
        val deltaText = if (latestUpdate != null) {
            UiText.Resource(R.string.coverage_delta_latest_update, latestUpdate)
        } else {
            UiText.Resource(R.string.coverage_delta_view_recent)
        }

        return CoverageSummary(
            topicCount = totalTopics,
            coveragePercent = normalizedPct.coerceIn(0, 100),
            v = v,
            r = r,
            e = e,
            u = u,
            solidCount = solid,
            partialCount = partial,
            absentCount = absent,
            doubtfulCount = uncertain,
            unitLabel = unitLabel,
            deltaLabel = deltaLabel,
            unitText = unitText,
            deltaText = deltaText
        )
    }

    fun toTopics(
        dto: CoverageResponseDto,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): List<CoverageTopic> {
        val leafDtos = dto.topics.filter { it.state != null }.ifEmpty { dto.topics }
        return leafDtos.map { it.toDomainTopic(zoneId, locale) }
    }

    fun TopicCoverageResponseDto.toDomainTopic(
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): CoverageTopic {
        val mappedState = mapState(state)
        val fullName = "${code.trim()} ${title.trim()}".trim()
        val formattedDate = updatedAt?.takeIf { it.isNotBlank() }?.toLocalDisplayDate(zoneId, locale)
        val noteText = formattedDate?.let { "Actualizado: $it" }
        val noteUiText = formattedDate?.let { UiText.Resource(R.string.coverage_topic_updated_at, it) }
        return CoverageTopic(
            state = mappedState,
            name = fullName,
            note = noteText,
            score = String.format(Locale.US, "%.2f", score),
            topicId = topicId,
            code = code,
            title = title,
            updatedAt = updatedAt,
            noteText = noteUiText
        )
    }

    fun toCourseStats(dto: CoverageResponseDto): CourseStats {
        val summary = toSummary(dto)
        return CourseStats(
            courseId = dto.courseId,
            instructorName = "",
            coveragePercent = summary.coveragePercent,
            verified = summary.v,
            repaired = summary.r,
            escalated = summary.e,
            uncertain = summary.u,
            studentCount = 0,
            noteCount = 0,
            topicCount = summary.topicCount,
            pendingCount = 0,
            weeklyDeltaPoints = 0,
            hasSyllabus = summary.topicCount > 0 || dto.topics.isNotEmpty(),
            solidCount = summary.solidCount,
            partialCount = summary.partialCount,
            absentCount = summary.absentCount,
            uncertainCount = summary.doubtfulCount
        )
    }

    fun toDeltaInfo(
        dto: CoverageDeltaResponseDto,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): CoverageDeltaInfo {
        val changedTopics = dto.changedTopics.map { it.toDomainTopic(zoneId, locale) }
        val solid = dto.changedCounts["SOLID"] ?: 0
        val partial = dto.changedCounts["PARTIAL"] ?: 0
        val absent = dto.changedCounts["ABSENT"] ?: 0
        val uncertain = dto.changedCounts["UNCERTAIN"] ?: 0
        val totalChanged = (solid + partial + absent + uncertain).coerceAtLeast(changedTopics.size)

        val toBar = if (totalChanged == 0) {
            CovBarValues(0f, 0f, 0f, 0f)
        } else {
            CovBarValues(
                v = solid * 100f / totalChanged,
                r = partial * 100f / totalChanged,
                e = absent * 100f / totalChanged,
                u = uncertain * 100f / totalChanged
            )
        }

        val sinceDisplay = dto.since.takeIf { it.isNotBlank() }?.toLocalDisplayDate(zoneId, locale).orEmpty()
        return CoverageDeltaInfo(
            dateRange = "Desde $sinceDisplay",
            fromBar = toBar,
            toBar = toBar,
            sinceIso = dto.since,
            sinceDisplay = sinceDisplay,
            changedCounts = dto.changedCounts,
            changedTopics = changedTopics,
            dateRangeText = UiText.Resource(R.string.coverage_delta_since, sinceDisplay)
        )
    }

    fun toTopicDetail(
        topic: CoverageTopic,
        noteRefs: List<TopicNoteRef> = emptyList()
    ): TopicDetail {
        return TopicDetail(
            name = topic.name,
            state = topic.state,
            stateLabel = stateLabel(topic.state),
            confidence = topic.score,
            checklist = emptyList(),
            noteRefs = noteRefs,
            stateLabelText = stateLabelText(topic.state)
        )
    }
}

fun CoverageResponseDto.toSummary(
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): CoverageSummary = CoverageMapper.toSummary(this, zoneId, locale)

fun CoverageResponseDto.toTopics(
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): List<CoverageTopic> = CoverageMapper.toTopics(this, zoneId, locale)

fun CoverageResponseDto.toCourseStats(): CourseStats = CoverageMapper.toCourseStats(this)

fun CoverageDeltaResponseDto.toDeltaInfo(
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): CoverageDeltaInfo = CoverageMapper.toDeltaInfo(this, zoneId, locale)

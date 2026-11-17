package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.SyllabusDocumentResponse
import cr.ac.una.glifo.domain.model.SyllabusDocument
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object SyllabusMapper {

    private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun toDomain(
        dto: SyllabusDocumentResponse,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SyllabusDocument = SyllabusDocument(
        courseId = dto.courseId,
        fileName = dto.fileName?.trim()?.takeIf { it.isNotEmpty() },
        sizeBytes = dto.sizeBytes,
        formattedSize = formatFileSize(dto.sizeBytes),
        parsedAtDisplay = toLocalDisplayDate(dto.parsedAt, zoneId),
        syllabusVersion = dto.syllabusVersion,
        units = dto.topics.toSyllabusUnitRows()
    )

    fun toLocalDisplayDate(
        rawInstant: String?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        val trimmed = rawInstant?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val instant = runCatching { Instant.parse(trimmed) }
            .recoverCatching { OffsetDateTime.parse(trimmed).toInstant() }
            .getOrNull()
        return instant?.atZone(zoneId)?.toLocalDate()?.format(displayDateFormatter)
            ?: trimmed.take(10)
    }

    fun formatFileSize(sizeBytes: Long?): String? {
        if (sizeBytes == null || sizeBytes < 0L) return null
        return when {
            sizeBytes < 1024L -> "$sizeBytes B"
            sizeBytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }
    }
}

fun SyllabusDocumentResponse.toDomain(
    zoneId: ZoneId = ZoneId.systemDefault()
): SyllabusDocument = SyllabusMapper.toDomain(this, zoneId)

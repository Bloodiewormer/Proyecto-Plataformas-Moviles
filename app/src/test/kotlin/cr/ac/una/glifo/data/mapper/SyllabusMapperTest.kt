package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.SyllabusDocumentResponse
import cr.ac.una.glifo.data.dto.SyllabusTopicResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SyllabusMapperTest {

    @Test
    fun `toDomain maps document metadata, formatted size, local date and nested topics`() {
        val dto = SyllabusDocumentResponse(
            courseId = 10L,
            fileName = "  EIF411-programa.pdf  ",
            sizeBytes = 1_572_864L,
            parsedAt = "2026-11-10T18:30:00Z",
            syllabusVersion = 2,
            topics = listOf(
                SyllabusTopicResponse(id = 1L, courseId = 10L, parentId = null, code = "U1", title = "Fundamentos", orderIndex = 1),
                SyllabusTopicResponse(id = 2L, courseId = 10L, parentId = 1L, code = "1.1", title = "Arquitectura", orderIndex = 2),
                SyllabusTopicResponse(id = 3L, courseId = 10L, parentId = 1L, code = "1.2", title = "Ciclo de vida", orderIndex = 3)
            )
        )

        val domain = SyllabusMapper.toDomain(dto, ZoneId.of("America/Costa_Rica"))

        assertEquals(10L, domain.courseId)
        assertEquals("EIF411-programa.pdf", domain.fileName)
        assertEquals(1_572_864L, domain.sizeBytes)
        assertEquals("1.5 MB", domain.formattedSize)
        assertEquals("10/11/2026", domain.parsedAtDisplay)
        assertEquals(2, domain.syllabusVersion)
        assertTrue(domain.hasSyllabus)
        assertTrue(domain.isPublished)
        assertEquals(1, domain.units.size)
        assertEquals("U1 · Fundamentos", domain.units[0].label)
        assertEquals(2, domain.units[0].topicCount)
        assertEquals(listOf("1.1 Arquitectura", "1.2 Ciclo de vida"), domain.units[0].topics.map { it.label })
    }

    @Test
    fun `toLocalDisplayDate converts UTC instant to target timezone`() {
        val formattedCostaRica = SyllabusMapper.toLocalDisplayDate(
            rawInstant = "2026-11-10T02:30:00Z",
            zoneId = ZoneId.of("America/Costa_Rica")
        )
        val formattedUtc = SyllabusMapper.toLocalDisplayDate(
            rawInstant = "2026-11-10T02:30:00Z",
            zoneId = ZoneId.of("UTC")
        )

        assertEquals("09/11/2026", formattedCostaRica)
        assertEquals("10/11/2026", formattedUtc)
    }

    @Test
    fun `default version with manual topics but no parsed_at is not published`() {
        val dto = SyllabusDocumentResponse(
            courseId = 6L,
            fileName = null,
            sizeBytes = null,
            parsedAt = null,
            syllabusVersion = 1,
            topics = listOf(SyllabusTopicResponse(id = 1L, courseId = 6L, parentId = null, code = "U1", title = "Limites", orderIndex = 1))
        )

        val domain = SyllabusMapper.toDomain(dto, ZoneId.of("UTC"))

        assertTrue(domain.hasSyllabus)
        assertFalse(domain.isPublished)
    }

    @Test
    fun `null size and missing optional metadata map cleanly without fabricating values`() {
        val dto = SyllabusDocumentResponse(
            courseId = 5L,
            fileName = null,
            sizeBytes = null,
            parsedAt = null,
            syllabusVersion = 0,
            topics = emptyList()
        )

        val domain = SyllabusMapper.toDomain(dto, ZoneId.of("UTC"))

        assertNull(domain.fileName)
        assertNull(domain.sizeBytes)
        assertNull(domain.formattedSize)
        assertNull(domain.parsedAtDisplay)
        assertEquals(0, domain.syllabusVersion)
        assertTrue(domain.units.isEmpty())
        assertFalse(domain.hasSyllabus)
        assertFalse(domain.isPublished)
    }

    @Test
    fun `formatFileSize handles bytes, kilobytes, megabytes and invalid sizes`() {
        assertNull(SyllabusMapper.formatFileSize(null))
        assertNull(SyllabusMapper.formatFileSize(-1L))
        assertEquals("512 B", SyllabusMapper.formatFileSize(512L))
        assertEquals("2.0 KB", SyllabusMapper.formatFileSize(2048L))
        assertEquals("1.0 MB", SyllabusMapper.formatFileSize(1024L * 1024L))
    }

    @Test
    fun `flat topics use localized default unit label when context is given`() {
        val context = io.mockk.mockk<android.content.Context> {
            io.mockk.every { getString(cr.ac.una.glifo.R.string.syllabus_unit_default) } returns "1 · Course topics"
        }
        val dto = SyllabusDocumentResponse(
            courseId = 10L,
            fileName = null,
            sizeBytes = null,
            parsedAt = null,
            syllabusVersion = 1,
            topics = listOf(SyllabusTopicResponse(id = 1L, courseId = 10L, parentId = null, code = "1.1", title = "Limits", orderIndex = 1))
        )

        val domain = SyllabusMapper.toDomain(dto, ZoneId.of("UTC"), context)

        assertEquals("1 · Course topics", domain.units.single().label)
    }
}

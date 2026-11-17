package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.CourseGapsResponseDto
import cr.ac.una.glifo.data.dto.TopicGapDto
import cr.ac.una.glifo.domain.model.ConfidenceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Brechas del grupo (GET coverage/gaps) → Inicio docente y Brechas. */
class TeacherGapsMapperTest {

    private fun topic(id: Long, code: String, gap: Int, solid: Int, partial: Int, absent: Int, uncertain: Int = 0) =
        TopicGapDto(
            topicId = id, code = code, title = "Tema $id", studentsWithGap = gap,
            solidCount = solid, partialCount = partial, absentCount = absent, uncertainCount = uncertain
        )

    @Test
    fun `solo salen los temas con brecha y el color depende de la parte del grupo`() {
        val dto = CourseGapsResponseDto(
            courseId = 1L, enrolledStudents = 10, coveragePct = 54.6f,
            topics = listOf(
                topic(1, "1.1", gap = 7, solid = 3, partial = 2, absent = 5),
                topic(2, "1.2", gap = 4, solid = 6, partial = 4, absent = 0),
                topic(3, "1.3", gap = 1, solid = 9, partial = 1, absent = 0),
                topic(4, "1.4", gap = 0, solid = 10, partial = 0, absent = 0)
            )
        )

        val group = TeacherGapsMapper.toGroupCoverage(dto)

        assertEquals(10, group.enrolledStudents)
        assertEquals(55, group.coveragePercent)
        assertEquals(listOf("1.1 Tema 1", "1.2 Tema 2", "1.3 Tema 3"), group.topics.map { it.label })
        assertEquals(
            listOf(ConfidenceState.Escalated, ConfidenceState.Uncertain, ConfidenceState.Repaired),
            group.topics.map { it.state }
        )
        assertEquals("7/10", group.topics[0].fraction)
        assertEquals(70f, group.topics[0].percent, 0.01f)
        // 40 celdas: 28 sólidas, 7 parciales, 5 ausentes.
        assertEquals(70f, group.solidShare, 0.01f)
        assertEquals(17.5f, group.partialShare, 0.01f)
        assertEquals(12.5f, group.absentShare, 0.01f)
        assertEquals(0f, group.uncertainShare, 0.01f)
    }

    @Test
    fun `un curso sin inscritos no divide entre cero`() {
        val group = TeacherGapsMapper.toGroupCoverage(
            CourseGapsResponseDto(courseId = 1L, enrolledStudents = 0, coveragePct = 0f, topics = listOf(topic(1, "1.1", 0, 0, 0, 0)))
        )

        assertTrue(group.topics.isEmpty())
        assertEquals(0f, group.solidShare, 0.0f)
        assertEquals(0, group.coveragePercent)
    }
}

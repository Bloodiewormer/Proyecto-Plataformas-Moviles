package cr.ac.una.glifo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllabusTopicPickerTest {

    @Test
    fun `empty topic list yields empty picker items`() {
        assertTrue(buildTopicPickerItems(emptyList()).isEmpty())
    }

    @Test
    fun `buildTopicPickerItems orders units as headers with indented child topics sorted by orderIndex`() {
        val u2 = SyllabusTopic(id = 20L, courseId = 1L, parentId = null, code = "U2", title = "Derivatives", orderIndex = 2)
        val u1 = SyllabusTopic(id = 10L, courseId = 1L, parentId = null, code = "U1", title = "Limits", orderIndex = 1)
        val u1t2 = SyllabusTopic(id = 12L, courseId = 1L, parentId = 10L, code = "1.2", title = "Continuity", orderIndex = 2)
        val u1t1 = SyllabusTopic(id = 11L, courseId = 1L, parentId = 10L, code = "1.1", title = "One-sided limits", orderIndex = 1)
        val u2t1 = SyllabusTopic(id = 21L, courseId = 1L, parentId = 20L, code = "2.1", title = "Chain rule", orderIndex = 1)
        val standalone = SyllabusTopic(id = 30L, courseId = 1L, parentId = null, code = "U3", title = "Integrals", orderIndex = 3)

        val items = buildTopicPickerItems(listOf(u2t1, u1t2, standalone, u2, u1, u1t1))

        assertEquals(
            listOf(
                TopicPickerItem.UnitHeader(u1),
                TopicPickerItem.TopicOption(u1t1, indented = true),
                TopicPickerItem.TopicOption(u1t2, indented = true),
                TopicPickerItem.UnitHeader(u2),
                TopicPickerItem.TopicOption(u2t1, indented = true),
                TopicPickerItem.TopicOption(standalone, indented = false)
            ),
            items
        )
    }

    @Test
    fun `buildTopicPickerItems includes orphan and cyclic topics without dropping entries`() {
        val orphan = SyllabusTopic(id = 50L, courseId = 1L, parentId = 999L, code = "4.1", title = "Orphan", orderIndex = 1)
        val cycleA = SyllabusTopic(id = 60L, courseId = 1L, parentId = 61L, code = "5.1", title = "Cycle A", orderIndex = 1)
        val cycleB = SyllabusTopic(id = 61L, courseId = 1L, parentId = 60L, code = "5.2", title = "Cycle B", orderIndex = 2)

        val items = buildTopicPickerItems(listOf(cycleB, orphan, cycleA))

        val ids = items.map {
            when (it) {
                is TopicPickerItem.UnitHeader -> it.topic.id
                is TopicPickerItem.TopicOption -> it.topic.id
            }
        }
        assertEquals(listOf(50L, 60L, 61L), ids)
    }

    @Test
    fun `displayLabel and unitHeaderLabel avoid duplicating code when title already includes it`() {
        val duplicated = SyllabusTopic(id = 1L, courseId = 1L, parentId = null, code = "1.1", title = "1.1 Limits", orderIndex = 1)
        val normal = SyllabusTopic(id = 2L, courseId = 1L, parentId = 1L, code = "1.2", title = "Continuity", orderIndex = 2)

        assertEquals("1.1 Limits", duplicated.displayLabel())
        assertEquals("1.2 Continuity", normal.displayLabel())
        assertEquals("1.2 · Continuity", normal.unitHeaderLabel())
    }
}

package cr.ac.una.glifo.data.mapper

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.data.dto.CourseDetailResponse
import cr.ac.una.glifo.data.dto.CourseResponse
import cr.ac.una.glifo.data.dto.CreateCourseRequest
import cr.ac.una.glifo.data.dto.SyllabusTopicResponse
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.SyllabusTopic
import cr.ac.una.glifo.domain.model.SyllabusTopicRow
import cr.ac.una.glifo.domain.model.SyllabusUnitRow

// DTO -> Domain
fun CourseResponse.toDomain(): Course = Course(
    id = id,
    name = name,
    code = code,
    joinCode = joinCode,
    term = term
)

fun CourseDetailResponse.toDomain(): CourseDetail = CourseDetail(
    id = id,
    name = name,
    code = code,
    joinCode = joinCode,
    syllabusStatus = syllabusStatus
)

fun SyllabusTopicResponse.toDomain(): SyllabusTopic = SyllabusTopic(
    id = id,
    courseId = courseId,
    parentId = parentId,
    code = code,
    title = title,
    orderIndex = orderIndex
)

// Domain -> Request DTO
fun Course.toCreateRequest(term: String = "I"): CreateCourseRequest = CreateCourseRequest(
    name = name,
    code = code,
    term = term
)

/** Agrupa la lista plana/jerárquica de temas del backend en unidades y subtemas para la UI (#32). */
fun List<SyllabusTopicResponse>.toSyllabusUnitRows(context: Context? = null): List<SyllabusUnitRow> {
    if (isEmpty()) return emptyList()

    val sorted = sortedBy { it.orderIndex }
    val roots = sorted.filter { it.parentId == null }
    val childrenByParent = sorted.filter { it.parentId != null }.groupBy { it.parentId }

    if (childrenByParent.isNotEmpty() && roots.isNotEmpty()) {
        return roots.map { unit ->
            val subtopics = childrenByParent[unit.id].orEmpty().map { child ->
                val label = if (child.title.startsWith(child.code)) child.title else "${child.code} ${child.title}".trim()
                SyllabusTopicRow(label = label, hasIssue = false)
            }
            val unitLabel = if (unit.title.startsWith(unit.code)) unit.title else "${unit.code} · ${unit.title}"
            SyllabusUnitRow(
                label = unitLabel,
                topicCount = subtopics.size,
                topics = subtopics
            )
        }
    }

    // Si todos los temas son de primer nivel (sin jerarquía padre-hijo), los agrupa en una unidad
    val rows = sorted.map { topic ->
        val label = if (topic.title.startsWith(topic.code)) topic.title else "${topic.code} ${topic.title}".trim()
        SyllabusTopicRow(label = label, hasIssue = false)
    }
    return listOf(
        SyllabusUnitRow(
            label = context?.getString(R.string.syllabus_unit_default) ?: "1 · Temas del curso",
            topicCount = rows.size,
            topics = rows
        )
    )
}

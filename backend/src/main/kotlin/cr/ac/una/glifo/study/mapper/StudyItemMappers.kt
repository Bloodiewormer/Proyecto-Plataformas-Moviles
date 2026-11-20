package cr.ac.una.glifo.study.mapper

import cr.ac.una.glifo.study.dto.StudyItemResponse
import cr.ac.una.glifo.study.entity.StudyItem
import tools.jackson.databind.json.JsonMapper

private val jsonMapper = JsonMapper.builder().findAndAddModules().build()

fun StudyItem.toResponse(): StudyItemResponse = StudyItemResponse(
    id = id,
    courseId = course?.id ?: 0L,
    syllabusTopicId = syllabusTopic?.id ?: 0L,
    kind = kind,
    payload = jsonMapper.readTree(payload),
    createdAt = createdAt,
    userId = user?.id
)

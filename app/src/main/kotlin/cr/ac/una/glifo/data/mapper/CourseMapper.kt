package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.CourseDetailResponse
import cr.ac.una.glifo.data.dto.CourseResponse
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseDetail

fun CourseResponse.toDomain(): Course = Course(
    id = id,
    name = name,
    code = code,
    joinCode = joinCode
)

fun CourseDetailResponse.toDomain(): CourseDetail = CourseDetail(
    id = id,
    name = name,
    code = code,
    joinCode = joinCode,
    syllabusStatus = syllabusStatus
)

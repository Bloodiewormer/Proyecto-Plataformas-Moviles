package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.core.database.entity.CourseEntity
import cr.ac.una.glifo.data.dto.CourseDetailResponse
import cr.ac.una.glifo.data.dto.CourseResponse
import cr.ac.una.glifo.data.dto.CreateCourseRequest
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseDetail

// DTO -> Domain
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

// Entity (Room) -> Domain
fun CourseEntity.toDomain(): Course = Course(
    id = id,
    name = name,
    code = code,
    joinCode = joinCode
)

fun CourseEntity.toDetailDomain(): CourseDetail = CourseDetail(
    id = id,
    name = name,
    code = code,
    joinCode = joinCode,
    syllabusStatus = syllabusStatus
)

// Domain -> Entity (Room)
fun Course.toEntity(syllabusStatus: String? = null, isSynced: Boolean = true): CourseEntity = CourseEntity(
    id = id,
    name = name,
    code = code,
    joinCode = joinCode,
    syllabusStatus = syllabusStatus,
    isSynced = isSynced
)

fun CourseDetail.toEntity(isSynced: Boolean = true): CourseEntity = CourseEntity(
    id = id,
    name = name,
    code = code,
    joinCode = joinCode,
    syllabusStatus = syllabusStatus,
    isSynced = isSynced
)

// Domain -> Request DTO
fun Course.toCreateRequest(term: String = "I"): CreateCourseRequest = CreateCourseRequest(
    name = name,
    code = code,
    term = term
)

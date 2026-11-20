package cr.ac.una.glifo.course.mapper

import cr.ac.una.glifo.course.dto.CourseResponse
import cr.ac.una.glifo.course.dto.EnrollmentResponse
import cr.ac.una.glifo.course.dto.SyllabusTopicResponse
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.Enrollment
import cr.ac.una.glifo.course.entity.SyllabusTopic

fun Course.toResponse(): CourseResponse = CourseResponse(
    id = this.id,
    name = this.name,
    code = this.code,
    term = this.term,
    joinCode = this.joinCode,
    syllabusStatus = if (this.syllabusParsedAt != null) "PARSED" else "PENDING"
)

fun Enrollment.toResponse(): EnrollmentResponse = EnrollmentResponse(
    id = this.id,
    courseId = this.course?.id ?: 0,
    courseName = this.course?.name ?: "",
    status = this.status,
    joinedAt = this.joinedAt
)

/** Convierte un tema del temario JPA a su DTO de salida (#32). */
fun SyllabusTopic.toResponse(): SyllabusTopicResponse = SyllabusTopicResponse(
    id = this.id,
    courseId = this.course?.id ?: 0,
    parentId = this.parent?.id,
    code = this.code,
    title = this.title,
    orderIndex = this.orderIndex
)

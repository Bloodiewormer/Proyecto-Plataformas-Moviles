package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.core.database.entity.NoteEntity
import cr.ac.una.glifo.data.dto.CreateNoteRequest
import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.domain.model.Note

// DTO -> Domain
fun NoteResponse.toDomain(): Note = Note(
    id = id,
    courseId = courseId,
    title = title,
    classDate = classDate
)

// Entity (Room) -> Domain
fun NoteEntity.toDomain(): Note = Note(
    id = id,
    courseId = courseId,
    title = title,
    classDate = classDate
)

// Domain -> Entity (Room)
fun Note.toEntity(
    userId: Long = 1L,
    syllabusTopicId: Long? = null,
    status: String = "READY",
    content: String? = null,
    contentGeneratedAt: String? = null,
    createdAt: String = classDate
): NoteEntity = NoteEntity(
    id = id,
    userId = userId,
    courseId = courseId,
    syllabusTopicId = syllabusTopicId,
    classDate = classDate,
    title = title,
    status = status,
    content = content,
    contentGeneratedAt = contentGeneratedAt,
    createdAt = createdAt
)

// Domain -> Request DTO
fun Note.toCreateRequest(): CreateNoteRequest = CreateNoteRequest(
    courseId = courseId,
    title = title,
    classDate = classDate
)

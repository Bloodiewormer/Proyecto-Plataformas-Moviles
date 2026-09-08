package cr.ac.una.glifo.data.mapper

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

// Domain -> Request DTO
fun Note.toCreateRequest(): CreateNoteRequest = CreateNoteRequest(
    courseId = courseId,
    title = title,
    classDate = classDate
)

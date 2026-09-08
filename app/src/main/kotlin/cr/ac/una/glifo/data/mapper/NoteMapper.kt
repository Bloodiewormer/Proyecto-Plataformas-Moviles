package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.domain.model.Note

fun NoteResponse.toDomain(): Note = Note(
    id = id,
    courseId = courseId,
    title = title,
    classDate = classDate
)

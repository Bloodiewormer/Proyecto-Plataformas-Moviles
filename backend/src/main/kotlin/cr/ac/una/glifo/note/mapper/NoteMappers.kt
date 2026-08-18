package cr.ac.una.glifo.note.mapper

import cr.ac.una.glifo.note.dto.NotePageResponse
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage

fun Note.toResponse() = NoteResponse(
    id = id,
    courseId = course.id,
    syllabusTopicId = syllabusTopic?.id,
    classDate = classDate.toString(),
    title = title,
    status = status,
    createdAt = createdAt.toString()
)

fun NotePage.toResponse() = NotePageResponse(
    id = id,
    noteId = note.id,
    pageIndex = pageIndex,
    storageUri = storageUri,
    levelReached = levelReached,
    overallConfidence = overallConfidence,
    processedAt = processedAt?.toString()
)

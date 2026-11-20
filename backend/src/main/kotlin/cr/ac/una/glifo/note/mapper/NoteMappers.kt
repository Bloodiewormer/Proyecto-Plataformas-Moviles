package cr.ac.una.glifo.note.mapper

import cr.ac.una.glifo.note.dto.NotePageResponse
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage

/** [includeContent] solo en el detalle: el listado no arrastra el documento IA-01. */
fun Note.toResponse(includeContent: Boolean = false, pageCount: Int? = null) = NoteResponse(
    id = id,
    courseId = course.id,
    syllabusTopicId = syllabusTopic?.id,
    classDate = classDate.toString(),
    title = title,
    status = status,
    createdAt = createdAt.toString(),
    pageCount = pageCount,
    content = if (includeContent) content else null
)

fun NotePage.toResponse() = NotePageResponse(
    id = id,
    noteId = note.id,
    pageIndex = pageIndex,
    perceptualHash = perceptualHash,
    storageUri = storageUri,
    levelReached = levelReached,
    overallConfidence = overallConfidence,
    processedAt = processedAt?.toString(),
    regions = regions
)

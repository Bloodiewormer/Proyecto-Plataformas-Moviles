package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.CreateNoteRequest
import cr.ac.una.glifo.data.datasource.local.entity.NoteEntity
import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.domain.model.Note

// DTO -> Domain
fun NoteResponse.toDomain(): Note = Note(
    id = id,
    courseId = courseId,
    title = title,
    classDate = classDate,
    syllabusTopicId = syllabusTopicId
)

// Domain -> Request DTO
fun Note.toCreateRequest(): CreateNoteRequest = CreateNoteRequest(
    courseId = courseId,
    title = title,
    classDate = classDate,
    syllabusTopicId = syllabusTopicId
)

/** A server note as the local cache stores it; keeps the server creation time so lists sort by it. */
fun NoteResponse.toCachedEntity(defaultStatus: String): NoteEntity = NoteEntity(
    serverId = id,
    courseId = courseId,
    title = title,
    classDate = classDate,
    status = status ?: defaultStatus,
    createdAt = createdAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?: System.currentTimeMillis(),
    remotePageCount = pageCount ?: 0
)

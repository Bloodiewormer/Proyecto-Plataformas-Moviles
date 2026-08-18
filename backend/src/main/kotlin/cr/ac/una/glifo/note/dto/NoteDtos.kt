package cr.ac.una.glifo.note.dto

data class CreateNoteRequest(
    val courseId: Long,
    val syllabusTopicId: Long?,
    val classDate: String,
    val title: String
)

data class NoteResponse(
    val id: Long,
    val courseId: Long,
    val syllabusTopicId: Long?,
    val classDate: String,
    val title: String,
    val status: String,
    val createdAt: String
)

data class CreateNotePageRequest(
    val pageIndex: Int
)

data class NotePageResponse(
    val id: Long,
    val noteId: Long,
    val pageIndex: Int,
    val storageUri: String,
    val levelReached: String,
    val overallConfidence: Float,
    val processedAt: String?
)

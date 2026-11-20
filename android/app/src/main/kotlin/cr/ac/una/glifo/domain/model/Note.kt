package cr.ac.una.glifo.domain.model

data class Note(
    val id: Long,
    val courseId: Long,
    val title: String,
    val classDate: String,
    val syllabusTopicId: Long? = null
)

data class NoteMeta(
    val note: Note,
    val chips: List<ConfidenceState>,
    val pageCount: Int,
    val displayDate: String
)

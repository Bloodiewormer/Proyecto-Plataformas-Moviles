package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.domain.model.ConfidenceState

/** Pairs a [NoteResponse] DTO with the extra display-only fields the note list needs. */
data class DummyNoteMetaEntry(
    val note: NoteResponse,
    val chips: List<ConfidenceState>,
    val pageCount: Int,
    val displayDate: String
)

object DummyNoteProvider {
    private val entries = listOf(
        DummyNoteMetaEntry(
            NoteResponse(id = 1L, courseId = 1L, title = "Integrales impropias", classDate = "2026-08-12"),
            listOf(ConfidenceState.Verified, ConfidenceState.Repaired, ConfidenceState.Escalated),
            pageCount = 3, displayDate = "12 ago"
        ),
        DummyNoteMetaEntry(
            NoteResponse(id = 2L, courseId = 1L, title = "Criterio de comparación", classDate = "2026-08-07"),
            listOf(ConfidenceState.Verified, ConfidenceState.Uncertain),
            pageCount = 2, displayDate = "7 ago"
        ),
        DummyNoteMetaEntry(
            NoteResponse(id = 3L, courseId = 1L, title = "Series de potencias", classDate = "2026-08-02"),
            listOf(ConfidenceState.Verified, ConfidenceState.Repaired),
            pageCount = 4, displayDate = "2 ago"
        ),
        DummyNoteMetaEntry(
            NoteResponse(id = 4L, courseId = 1L, title = "Prueba de la integral", classDate = "2026-07-28"),
            listOf(ConfidenceState.Verified),
            pageCount = 2, displayDate = "28 jul"
        )
    )

    fun getNotesForCourse(courseId: Long): List<NoteResponse> = entries.map { it.note }

    fun getNoteMetaEntries(): List<DummyNoteMetaEntry> = entries

    fun findNote(noteId: Long): NoteResponse = entries.find { it.note.id == noteId }?.note ?: entries.first().note

    fun findEntry(noteId: Long): DummyNoteMetaEntry = entries.find { it.note.id == noteId } ?: entries.first()

    fun getChips(noteId: Long): List<ConfidenceState> =
        entries.find { it.note.id == noteId }?.chips ?: entries.first().chips
}

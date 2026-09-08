package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.dummy.DummyNoteContentProvider
import cr.ac.una.glifo.data.datasource.dummy.DummyNoteProvider
import cr.ac.una.glifo.data.mapper.toDomain
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.Note
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.repository.NoteRepository
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor() : NoteRepository {

    override suspend fun getNotesForCourse(courseId: Long): Result<List<Note>> =
        Result.Success(DummyNoteProvider.getNotesForCourse(courseId).map { it.toDomain() })

    override suspend fun getNoteMetaList(): Result<List<NoteMeta>> =
        Result.Success(
            DummyNoteProvider.getNoteMetaEntries().map { entry ->
                NoteMeta(
                    note = entry.note.toDomain(),
                    chips = entry.chips,
                    pageCount = entry.pageCount,
                    displayDate = entry.displayDate
                )
            }
        )

    override suspend fun getNote(noteId: Long): Result<Note> =
        Result.Success(DummyNoteProvider.findNote(noteId).toDomain())

    override suspend fun getNoteMeta(noteId: Long): Result<NoteMeta> {
        val entry = DummyNoteProvider.findEntry(noteId)
        return Result.Success(
            NoteMeta(
                note = entry.note.toDomain(),
                chips = entry.chips,
                pageCount = entry.pageCount,
                displayDate = entry.displayDate
            )
        )
    }

    override suspend fun getChips(noteId: Long): Result<List<ConfidenceState>> =
        Result.Success(DummyNoteProvider.getChips(noteId))

    override suspend fun getNoteContent(noteId: Long): Result<NoteContent> {
        val entry = DummyNoteContentProvider.get(noteId)
        val note = DummyNoteProvider.findNote(entry.noteId).toDomain()
        return Result.Success(
            NoteContent(
                note = note,
                pageLabel = entry.pageLabel,
                heading = entry.heading,
                bodyBeforeHighlight = entry.bodyBeforeHighlight,
                highlighted = entry.highlighted,
                bodyAfterHighlight = entry.bodyAfterHighlight,
                verifiedCount = entry.verifiedCount,
                repairedCount = entry.repairedCount,
                escalatedCount = entry.escalatedCount,
                uncertainCount = entry.uncertainCount,
                formula = entry.formula,
                secondParagraphBefore = entry.secondParagraphBefore,
                secondParagraphHighlighted = entry.secondParagraphHighlighted,
                uncertainFragmentLabel = entry.uncertainFragmentLabel,
                correction = entry.correction,
                provenance = entry.provenance
            )
        )
    }

    override suspend fun saveCorrection(noteId: Long, regionIndex: Int, correctedText: String): Result<Unit> {
        // Dummy datasource is read-only for corrections today — accepted but not persisted.
        // Real implementation replaces this with a call through GlifoApi + local queueing.
        return Result.Success(Unit)
    }
}

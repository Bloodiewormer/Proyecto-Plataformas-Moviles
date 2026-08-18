package cr.ac.una.glifo.note.repository

import cr.ac.una.glifo.note.entity.NotePage
import org.springframework.data.jpa.repository.JpaRepository

interface NotePageRepository : JpaRepository<NotePage, Long> {
    fun findByNoteIdOrderByPageIndex(noteId: Long): List<NotePage>
}

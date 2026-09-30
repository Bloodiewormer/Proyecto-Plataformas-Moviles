package cr.ac.una.glifo.note.repository

import cr.ac.una.glifo.note.entity.NotePage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotePageRepository : JpaRepository<NotePage, Long> {
    fun findByNoteIdOrderByPageIndex(noteId: Long): List<NotePage>
    fun findByPerceptualHash(perceptualHash: String): NotePage?
    fun existsByNoteIdAndPerceptualHash(noteId: Long, perceptualHash: String): Boolean
    fun countByNoteId(noteId: Long): Long
    fun existsByNoteIdAndPageIndex(noteId: Long, pageIndex: Int): Boolean
}

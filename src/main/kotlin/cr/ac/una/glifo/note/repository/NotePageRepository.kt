package cr.ac.una.glifo.note.repository

import cr.ac.una.glifo.note.entity.NotePage
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NotePageRepository : JpaRepository<NotePage, Long> {
    fun findByNoteIdOrderByPageIndex(noteId: Long): List<NotePage>
    fun findByPerceptualHash(perceptualHash: String): NotePage?
    fun existsByNoteIdAndPerceptualHash(noteId: Long, perceptualHash: String): Boolean
    fun countByNoteId(noteId: Long): Long
    fun existsByNoteIdAndPageIndex(noteId: Long, pageIndex: Int): Boolean
    fun findByNoteIdAndPageIndex(noteId: Long, pageIndex: Int): NotePage?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from NotePage p where p.note.id = :noteId and p.pageIndex = :pageIndex")
    fun findForUpdate(noteId: Long, pageIndex: Int): NotePage?
}

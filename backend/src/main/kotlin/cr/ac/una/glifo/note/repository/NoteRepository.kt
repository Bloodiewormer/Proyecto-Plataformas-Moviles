package cr.ac.una.glifo.note.repository

import cr.ac.una.glifo.note.entity.Note
import org.springframework.data.jpa.repository.JpaRepository

interface NoteRepository : JpaRepository<Note, Long> {
    fun findByUserIdAndCourseId(userId: Long, courseId: Long): List<Note>
    fun findByUserIdAndStatus(userId: Long, status: String): List<Note>
    fun findByUserId(userId: Long): List<Note>
}

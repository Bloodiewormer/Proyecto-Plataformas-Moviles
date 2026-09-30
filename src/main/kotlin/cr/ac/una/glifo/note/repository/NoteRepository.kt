package cr.ac.una.glifo.note.repository

import cr.ac.una.glifo.note.entity.Note
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NoteRepository : JpaRepository<Note, Long> {
    fun findByUserId(userId: Long): List<Note>
    fun findByCourseId(courseId: Long): List<Note>
    fun findByUserIdAndCourseId(userId: Long, courseId: Long): List<Note>
    fun findByUserIdAndStatus(userId: Long, status: String): List<Note>
    fun countByUserId(userId: Long): Long
    fun countByCourseId(courseId: Long): Long
}

package cr.ac.una.glifo.course.repository

import cr.ac.una.glifo.course.entity.SyllabusTopic
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SyllabusTopicRepository : JpaRepository<SyllabusTopic, Long> {
    @Query("SELECT t FROM SyllabusTopic t WHERE t.course.id = :courseId ORDER BY t.orderIndex ASC, t.id ASC")
    fun findByCourseIdOrderByOrderIndex(courseId: Long): List<SyllabusTopic>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SyllabusTopic t WHERE t.course.id = :courseId")
    fun deleteAllByCourseId(courseId: Long)
}

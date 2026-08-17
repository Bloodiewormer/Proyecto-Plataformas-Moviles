package cr.ac.una.glifo.course.repository

import cr.ac.una.glifo.course.entity.SyllabusTopic
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SyllabusTopicRepository : JpaRepository<SyllabusTopic, Long> {
    fun findByCourseIdOrderByOrderIndex(courseId: Long): List<SyllabusTopic>
}

package cr.ac.una.glifo.glossary.repository

import cr.ac.una.glifo.glossary.entity.CourseGlossary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseGlossaryRepository : JpaRepository<CourseGlossary, Long> {
    fun findByCourseId(courseId: Long): List<CourseGlossary>
    fun findByCourseIdAndKind(courseId: Long, kind: String): List<CourseGlossary>
}

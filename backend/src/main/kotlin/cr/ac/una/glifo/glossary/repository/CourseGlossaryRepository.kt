package cr.ac.una.glifo.glossary.repository

import cr.ac.una.glifo.glossary.entity.CourseGlossary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseGlossaryRepository : JpaRepository<CourseGlossary, Long> {
    fun findByCourseId(courseId: Long): List<CourseGlossary>
    fun findByCourseIdAndKind(courseId: Long, kind: String): List<CourseGlossary>
    fun findByCourseIdOrderByTerm(courseId: Long): List<CourseGlossary>
    fun findByIdAndCourseId(id: Long, courseId: Long): CourseGlossary?

    /** Mismo término y mismo kind en el curso: es un duplicado (§4.3, ítem abierto 13). */
    fun existsByCourseIdAndTermIgnoreCaseAndKind(courseId: Long, term: String, kind: String): Boolean
}

package cr.ac.una.glifo.glossary.repository

import cr.ac.una.glifo.glossary.entity.GlossarySuggestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GlossarySuggestionRepository : JpaRepository<GlossarySuggestion, Long> {
    fun findByCourseIdAndStatus(courseId: Long, status: String): List<GlossarySuggestion>
    fun findByUserId(userId: Long): List<GlossarySuggestion>
}

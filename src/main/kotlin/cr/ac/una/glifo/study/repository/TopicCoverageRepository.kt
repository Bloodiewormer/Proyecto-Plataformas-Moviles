package cr.ac.una.glifo.study.repository

import cr.ac.una.glifo.study.entity.TopicCoverage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface TopicCoverageRepository : JpaRepository<TopicCoverage, Long> {
    fun findByUserIdAndSyllabusTopicIdIn(userId: Long, topicIds: List<Long>): List<TopicCoverage>
    fun findByUserId(userId: Long): List<TopicCoverage>

    /** Cobertura de varios estudiantes en los temas de un curso (brechas del grupo, docente). */
    fun findBySyllabusTopicIdInAndUserIdIn(topicIds: List<Long>, userIds: List<Long>): List<TopicCoverage>

    /** Base del delta de cobertura: solo los temas que cambiaron después de [since] (§4.5). */
    fun findByUserIdAndSyllabusTopicIdInAndUpdatedAtAfter(
        userId: Long,
        topicIds: List<Long>,
        since: Instant
    ): List<TopicCoverage>
}

package cr.ac.una.glifo.study.repository

import cr.ac.una.glifo.study.entity.TopicCoverage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TopicCoverageRepository : JpaRepository<TopicCoverage, Long> {
    fun findByUserIdAndSyllabusTopicIdIn(userId: Long, topicIds: List<Long>): List<TopicCoverage>
    fun findByUserId(userId: Long): List<TopicCoverage>
}

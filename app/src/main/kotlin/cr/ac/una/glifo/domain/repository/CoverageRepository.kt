package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CoverageDeltaInfo
import cr.ac.una.glifo.domain.model.CoverageSummary
import cr.ac.una.glifo.domain.model.CoverageTopic
import cr.ac.una.glifo.domain.model.TopicDetail

interface CoverageRepository {
    suspend fun getSummary(courseId: Long): Result<CoverageSummary>
    suspend fun getTopics(courseId: Long): Result<List<CoverageTopic>>
    suspend fun getTopicDetail(name: String): Result<TopicDetail>
    suspend fun getDelta(): Result<CoverageDeltaInfo>
}

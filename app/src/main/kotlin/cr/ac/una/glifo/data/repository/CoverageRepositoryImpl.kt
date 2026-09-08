package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.dummy.DummyCoverageProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CoverageDeltaInfo
import cr.ac.una.glifo.domain.model.CoverageSummary
import cr.ac.una.glifo.domain.model.CoverageTopic
import cr.ac.una.glifo.domain.model.TopicDetail
import cr.ac.una.glifo.domain.repository.CoverageRepository
import javax.inject.Inject

class CoverageRepositoryImpl @Inject constructor() : CoverageRepository {
    override suspend fun getSummary(courseId: Long): Result<CoverageSummary> =
        Result.Success(DummyCoverageProvider.getSummary(courseId))

    override suspend fun getTopics(courseId: Long): Result<List<CoverageTopic>> =
        Result.Success(DummyCoverageProvider.getTopics(courseId))

    override suspend fun getTopicDetail(name: String): Result<TopicDetail> =
        Result.Success(DummyCoverageProvider.getTopicDetail(name))

    override suspend fun getDelta(): Result<CoverageDeltaInfo> =
        Result.Success(DummyCoverageProvider.getDelta())
}

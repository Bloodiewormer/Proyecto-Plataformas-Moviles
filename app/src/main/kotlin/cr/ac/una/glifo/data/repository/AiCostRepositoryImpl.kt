package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.dummy.DummyAiCostProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AiCostStats
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.repository.AiCostRepository
import javax.inject.Inject

class AiCostRepositoryImpl @Inject constructor() : AiCostRepository {
    override suspend fun getStats(role: GlifoRole): Result<AiCostStats> =
        Result.Success(DummyAiCostProvider.getStats(role))
}

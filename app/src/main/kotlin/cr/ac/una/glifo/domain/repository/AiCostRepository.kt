package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AiCostStats
import cr.ac.una.glifo.domain.model.GlifoRole

interface AiCostRepository {
    suspend fun getStats(role: GlifoRole, courseId: Long? = null): Result<AiCostStats>
}

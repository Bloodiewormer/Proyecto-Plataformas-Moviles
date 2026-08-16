package cr.ac.una.glifo.pipeline.escalation

import cr.ac.una.glifo.pipeline.model.PageRegion
import cr.ac.una.glifo.pipeline.model.RegionResult
import javax.inject.Inject

class EscalationPolicy @Inject constructor(
    private val steps: List<EscalationStep>
) {
    fun resolve(region: PageRegion): RegionResult {
        // Chain of Responsibility implementation
        throw NotImplementedError()
    }
}

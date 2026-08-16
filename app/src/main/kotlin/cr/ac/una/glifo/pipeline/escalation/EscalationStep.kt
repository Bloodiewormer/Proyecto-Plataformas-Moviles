package cr.ac.una.glifo.pipeline.escalation

import cr.ac.una.glifo.pipeline.model.PageRegion
import cr.ac.una.glifo.pipeline.model.ProcessingLevel
import cr.ac.una.glifo.pipeline.model.RegionResult

interface EscalationStep {
    fun canHandle(region: PageRegion): Boolean
    fun handle(region: PageRegion): RegionResult
    fun level(): ProcessingLevel
}

package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.mapper.pageAnalysisFromSnapshotJson
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AiCostBreakdownRow
import cr.ac.una.glifo.domain.model.AiCostStats
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.repository.AiCostRepository
import javax.inject.Inject

/**
 * Uso de IA medido con lo que de verdad pasó en este teléfono: cada página que corrió el
 * pipeline queda en Room (#28) con sus regiones y a qué nivel las mandó el enrutador.
 *
 * Todavía no existe ninguna llamada pagada: N1.5, N2 y las funciones IA-0x son del Lab 6
 * (#38, #41–#44) y su registro es `ai_calls` en el backend (#46). Hasta entonces la cifra
 * honesta de llamadas pagadas es 0, y lo que se puede mostrar es cuánto resolvió el
 * teléfono solo y cuánto quedó en cola para los niveles pagados.
 */
class AiCostRepositoryImpl @Inject constructor(
    private val processedPageDao: ProcessedPageDao
) : AiCostRepository {

    override suspend fun getStats(role: GlifoRole): Result<AiCostStats> {
        val analyses = processedPageDao.all().mapNotNull { pageAnalysisFromSnapshotJson(it.analysisJson) }
        val regions = analyses.flatMap { it.regions }
        val total = regions.size

        fun row(state: ConfidenceState, label: String): AiCostBreakdownRow {
            val count = regions.count { it.confidenceState == state }
            val percent = if (total == 0) 0f else count * 100f / total
            return AiCostBreakdownRow(state, label, percent, count.toString())
        }

        val breakdown = if (total == 0) emptyList() else listOf(
            row(ConfidenceState.Verified, "N1 · ML Kit, en el teléfono"),
            row(ConfidenceState.Repaired, "Enrutadas a N1.5 · OCR matemático"),
            row(ConfidenceState.Escalated, "Enrutadas a N2 · visión"),
            row(ConfidenceState.Uncertain, "Bajo la compuerta de N1")
        )
        val resolvedLocally = regions.count { it.confidenceState == ConfidenceState.Verified }

        return Result.Success(
            AiCostStats(
                periodLabel = "Este teléfono · ${analyses.size} página(s) · $total regiones",
                headlinePercent = if (total == 0) 0 else resolvedLocally * 100 / total,
                isSystemWide = false,
                breakdown = breakdown,
                paidCalls = emptyList(),
                footerNote = if (analyses.isEmpty()) {
                    "Todavía no hay capturas procesadas en este teléfono."
                } else {
                    "Las ${analyses.size} página(s) se procesaron sin ninguna llamada pagada. " +
                        "N1.5 y N2 todavía no llaman a ningún modelo, así que lo enrutado ahí queda pendiente."
                }
            )
        )
    }
}

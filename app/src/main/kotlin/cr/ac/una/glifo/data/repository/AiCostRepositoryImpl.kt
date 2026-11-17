package cr.ac.una.glifo.data.repository

import androidx.annotation.StringRes
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiText
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.data.datasource.local.dao.ProcessedPageDao
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.mapper.AiUsageMapper
import cr.ac.una.glifo.data.mapper.pageAnalysisFromSnapshotJson
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AiCostBreakdownRow
import cr.ac.una.glifo.domain.model.AiCostStats
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.repository.AiCostRepository
import javax.inject.Inject

/**
 * Combina las métricas locales de páginas/regiones procesadas en el teléfono (`ProcessedPageDao`, #28)
 * con las métricas reales del backend (`ai_calls` / `ai_avoided_calls`, #46 / #47):
 * llamadas pagadas por función (`IA_01`, `IA_02`, `IA_04`, `OCR_M`/N1.5, `IA_00`/N2),
 * tokens, costo estimado y llamadas evitadas con su motivo.
 *
 * Si no hay red (`AppError.NoConnection`), devuelve las métricas locales junto con el aviso
 * de que los datos del servidor no están disponibles, sin inventar ceros.
 */
class AiCostRepositoryImpl : AiCostRepository {

    private val processedPageDao: ProcessedPageDao
    private val api: GlifoApi
    private val activeCourseStore: ActiveCourseStore?

    @Inject
    constructor(
        processedPageDao: ProcessedPageDao,
        api: GlifoApi,
        activeCourseStore: ActiveCourseStore
    ) {
        this.processedPageDao = processedPageDao
        this.api = api
        this.activeCourseStore = activeCourseStore
    }

    constructor(
        processedPageDao: ProcessedPageDao,
        api: GlifoApi
    ) {
        this.processedPageDao = processedPageDao
        this.api = api
        this.activeCourseStore = null
    }

    override suspend fun getStats(role: GlifoRole, courseId: Long?): Result<AiCostStats> {
        val analyses = processedPageDao.all().mapNotNull { pageAnalysisFromSnapshotJson(it.analysisJson) }
        val regions = analyses.flatMap { it.regions }
        val total = regions.size

        fun row(state: ConfidenceState, label: String, @StringRes labelRes: Int): AiCostBreakdownRow {
            val count = regions.count { it.confidenceState == state }
            val percent = if (total == 0) 0f else count * 100f / total
            return AiCostBreakdownRow(
                state = state,
                label = label,
                percent = percent,
                count = count.toString(),
                labelText = UiText.Resource(labelRes)
            )
        }

        val breakdown = if (total == 0) emptyList() else listOf(
            row(ConfidenceState.Verified, "N1 · ML Kit, en el teléfono", R.string.ai_cost_breakdown_n1),
            row(ConfidenceState.Repaired, "Enrutadas a N1.5 · OCR matemático", R.string.ai_cost_breakdown_n15),
            row(ConfidenceState.Escalated, "Enrutadas a N2 · visión", R.string.ai_cost_breakdown_n2),
            row(ConfidenceState.Uncertain, "Bajo la compuerta de N1", R.string.ai_cost_breakdown_uncertain)
        )
        val resolvedLocally = regions.count { it.confidenceState == ConfidenceState.Verified }
        val headlinePercent = if (total == 0) 0 else resolvedLocally * 100 / total

        val resolvedCourseId = courseId ?: activeCourseStore?.activeCourseId?.value
        val remote = ApiErrorHandler.safeApiCall(
            apiCall = {
                if (role == GlifoRole.Teacher && resolvedCourseId != null) {
                    api.getCourseAiUsage(resolvedCourseId)
                } else {
                    api.getMyAiUsage(courseId)
                }
            },
            transform = { it.data }
        )

        return when (remote) {
            is Result.Success -> Result.Success(
                AiUsageMapper.toAiCostStats(
                    dto = remote.value,
                    localPagesCount = analyses.size,
                    localRegionsCount = total,
                    headlinePercent = headlinePercent,
                    breakdown = breakdown,
                    isSystemWide = role == GlifoRole.Admin
                )
            )
            is Result.Failure -> {
                if (remote.error is AppError.NoConnection) {
                    Result.Success(
                        AiCostStats(
                            periodLabel = "Este teléfono · ${analyses.size} página(s) · $total regiones",
                            headlinePercent = headlinePercent,
                            isSystemWide = role == GlifoRole.Admin,
                            breakdown = breakdown,
                            paidCalls = emptyList(),
                            footerNote = "Sin conexión al servidor: los datos de llamadas pagadas, tokens y costos del servidor no están disponibles. Mostrando únicamente métricas locales.",
                            serverDataAvailable = false,
                            periodText = UiText.Resource(R.string.ai_cost_period_this_phone, analyses.size, total),
                            footerText = UiText.Resource(R.string.ai_cost_offline_footer)
                        )
                    )
                } else {
                    remote
                }
            }
        }
    }
}

package cr.ac.una.glifo.domain.model

data class AiCostBreakdownRow(val state: ConfidenceState, val label: String, val percent: Float, val count: String)
data class AiCostPaidCall(val label: String, val count: String)

data class AiCostStats(
    val periodLabel: String,
    val headlinePercent: Int,
    val isSystemWide: Boolean,
    val quotaUsed: Int? = null,
    val quotaTotal: Int? = null,
    val breakdown: List<AiCostBreakdownRow>,
    val paidCalls: List<AiCostPaidCall>,
    val footerNote: String
) {
    val quotaPercent: Int?
        get() = if (quotaUsed != null && quotaTotal != null && quotaTotal > 0) (quotaUsed * 100 / quotaTotal) else null
    val totalPaidCalls: Int
        get() = paidCalls.sumOf { it.count.toIntOrNull() ?: 0 }
}

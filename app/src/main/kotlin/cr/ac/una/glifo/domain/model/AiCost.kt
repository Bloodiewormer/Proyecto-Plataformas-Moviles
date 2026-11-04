package cr.ac.una.glifo.domain.model

data class AiCostBreakdownRow(
    val state: ConfidenceState,
    val label: String,
    val percent: Float,
    val count: String
)

data class AiCostPaidCall(
    val label: String,
    val count: String,
    val callType: String = "",
    val inputTokens: Long = 0L,
    val outputTokens: Long = 0L,
    val totalTokens: Long = 0L,
    val estimatedCostLabel: String = AiCostStats.COST_NOT_CONFIGURED_ES,
    val isCostConfigured: Boolean = false
)

data class AiCostAvoidedCall(
    val callType: String,
    val reason: String,
    val count: Long
)

data class AiCostStats(
    val periodLabel: String,
    val headlinePercent: Int,
    val isSystemWide: Boolean,
    val quotaUsed: Int? = null,
    val quotaTotal: Int? = null,
    val breakdown: List<AiCostBreakdownRow>,
    val paidCalls: List<AiCostPaidCall>,
    val footerNote: String,
    val serverDataAvailable: Boolean = true,
    val totalCalls: Long = paidCalls.sumOf { it.count.toLongOrNull() ?: 0L },
    val successCalls: Long = 0L,
    val failedCalls: Long = 0L,
    val avoidedCallsCount: Long = 0L,
    val avoidedCalls: List<AiCostAvoidedCall> = emptyList(),
    val inputTokens: Long = 0L,
    val outputTokens: Long = 0L,
    val totalTokens: Long = 0L,
    val estimatedCostLabel: String = COST_NOT_CONFIGURED_ES,
    val isCostConfigured: Boolean = false
) {
    val quotaPercent: Int?
        get() = if (quotaUsed != null && quotaTotal != null && quotaTotal > 0) (quotaUsed * 100 / quotaTotal) else null
    val totalPaidCalls: Int
        get() = paidCalls.sumOf { it.count.toIntOrNull() ?: 0 }

    companion object {
        const val COST_NOT_CONFIGURED_ES = "Costo no configurado"
    }
}

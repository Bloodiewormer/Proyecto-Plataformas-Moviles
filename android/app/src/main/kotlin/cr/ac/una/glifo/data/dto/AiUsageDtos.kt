package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * DTOs para los endpoints de telemetría y costo de IA (#46 / #47):
 * - `GET /api/v1/ai-usage/me`
 * - `GET /api/v1/courses/{courseId}/ai-usage`
 */
data class AiUsageResponseDto(
    @SerializedName("scope") val scope: String,
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("course_id") val courseId: Long? = null,
    @SerializedName("currency") val currency: String? = "USD",
    @SerializedName("from") val from: String? = null,
    @SerializedName("to") val to: String? = null,
    @SerializedName("totals") val totals: AiUsageTotalsDto,
    @SerializedName("by_call_type") val byCallType: List<AiUsageByCallTypeDto>? = emptyList()
)

data class AiUsageTotalsDto(
    @SerializedName("total_calls") val totalCalls: Long = 0L,
    @SerializedName(value = "successful_calls", alternate = ["success_calls"]) val successCalls: Long = 0L,
    @SerializedName("failed_calls") val failedCalls: Long = 0L,
    @SerializedName("rejected_outputs") val rejectedOutputs: Long = 0L,
    @SerializedName("avoided_calls") val avoidedCalls: Long = 0L,
    @SerializedName("input_tokens") val inputTokens: Long? = 0L,
    @SerializedName("output_tokens") val outputTokens: Long? = 0L,
    @SerializedName("total_tokens") val totalTokens: Long? = null,
    @SerializedName("unknown_token_calls") val unknownTokenCalls: Long = 0L,
    @SerializedName("estimated_cost_usd") val estimatedCostUsd: BigDecimal? = null,
    @SerializedName("unknown_cost_calls") val unknownCostCalls: Long = 0L,
    @SerializedName(value = "average_latency_ms", alternate = ["total_latency_ms", "avg_latency_ms"]) val totalLatencyMs: Long? = 0L
) {
    val resolvedInputTokens: Long get() = inputTokens ?: 0L
    val resolvedOutputTokens: Long get() = outputTokens ?: 0L
    val resolvedTotalTokens: Long get() = totalTokens ?: (resolvedInputTokens + resolvedOutputTokens)
}

data class AiUsageByCallTypeDto(
    @SerializedName("call_type") val callType: String,
    @SerializedName("usage") val usage: AiUsageTotalsDto? = null,
    @SerializedName("total_calls") val totalCalls: Long = 0L,
    @SerializedName(value = "successful_calls", alternate = ["success_calls"]) val successCalls: Long = 0L,
    @SerializedName("failed_calls") val failedCalls: Long = 0L,
    @SerializedName("avoided_calls") val avoidedCalls: Long = 0L,
    @SerializedName("avoided_by_reason") val avoidedByReason: Map<String, Long>? = emptyMap(),
    @SerializedName("input_tokens") val inputTokens: Long? = 0L,
    @SerializedName("output_tokens") val outputTokens: Long? = 0L,
    @SerializedName("total_tokens") val totalTokens: Long? = null,
    @SerializedName("estimated_cost_usd") val estimatedCostUsd: BigDecimal? = null,
    @SerializedName("unknown_cost_calls") val unknownCostCalls: Long = 0L,
    @SerializedName(value = "average_latency_ms", alternate = ["avg_latency_ms"]) val avgLatencyMs: Long? = null
) {
    val resolvedTotalCalls: Long get() = usage?.totalCalls ?: totalCalls
    val resolvedSuccessCalls: Long get() = usage?.successCalls ?: successCalls
    val resolvedFailedCalls: Long get() = usage?.failedCalls ?: failedCalls
    val resolvedAvoidedCalls: Long get() = usage?.avoidedCalls ?: avoidedCalls
    val resolvedInputTokens: Long get() = usage?.resolvedInputTokens ?: (inputTokens ?: 0L)
    val resolvedOutputTokens: Long get() = usage?.resolvedOutputTokens ?: (outputTokens ?: 0L)
    val resolvedTotalTokens: Long get() = usage?.resolvedTotalTokens ?: (totalTokens ?: (resolvedInputTokens + resolvedOutputTokens))
    val resolvedEstimatedCostUsd: BigDecimal? get() = if (usage != null) usage.estimatedCostUsd else estimatedCostUsd
    val resolvedUnknownCostCalls: Long get() = usage?.unknownCostCalls ?: unknownCostCalls
}

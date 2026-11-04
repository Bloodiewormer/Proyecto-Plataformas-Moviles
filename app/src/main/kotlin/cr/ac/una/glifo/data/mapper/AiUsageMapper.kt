package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.AiUsageByCallTypeDto
import cr.ac.una.glifo.data.dto.AiUsageResponseDto
import cr.ac.una.glifo.domain.model.AiCostAvoidedCall
import cr.ac.una.glifo.domain.model.AiCostBreakdownRow
import cr.ac.una.glifo.domain.model.AiCostPaidCall
import cr.ac.una.glifo.domain.model.AiCostStats
import java.math.BigDecimal

/**
 * Mapea la respuesta de `/ai-usage/me` y `/courses/{courseId}/ai-usage` (#46 / #47)
 * combinándola con las métricas locales de páginas/regiones procesadas en el teléfono.
 */
object AiUsageMapper {

    private val ORDERED_CALL_TYPES = listOf(
        "IA_01" to "IA-01 · Reconstrucción de apunte",
        "IA_02" to "IA-02 · Material de estudio",
        "IA_04" to "IA-04 · Cobertura del temario",
        "OCR_M" to "OCR_M (N1.5) · OCR matemático",
        "IA_00" to "IA_00 (N2) · Reparación por visión"
    )

    fun formatCostLabel(estimatedCostUsd: BigDecimal?, unknownCostCalls: Long): Pair<String, Boolean> {
        if (estimatedCostUsd == null || unknownCostCalls > 0L) {
            return AiCostStats.COST_NOT_CONFIGURED_ES to false
        }
        val normalized = if (estimatedCostUsd.compareTo(BigDecimal.ZERO) == 0) {
            "0.00"
        } else {
            estimatedCostUsd.stripTrailingZeros().toPlainString()
        }
        return "$$normalized USD" to true
    }

    fun labelForCallType(callType: String): String {
        return ORDERED_CALL_TYPES.firstOrNull { it.first == callType }?.second ?: callType
    }

    fun toPaidCalls(byCallType: List<AiUsageByCallTypeDto>): List<AiCostPaidCall> {
        val byKey = byCallType.associateBy { it.callType }
        val standardRows = ORDERED_CALL_TYPES.map { (code, label) ->
            val dto = byKey[code]
            if (dto != null) {
                val (costLabel, configured) = formatCostLabel(dto.resolvedEstimatedCostUsd, dto.resolvedUnknownCostCalls)
                AiCostPaidCall(
                    label = label,
                    count = dto.resolvedTotalCalls.toString(),
                    callType = code,
                    inputTokens = dto.resolvedInputTokens,
                    outputTokens = dto.resolvedOutputTokens,
                    totalTokens = dto.resolvedTotalTokens,
                    estimatedCostLabel = costLabel,
                    isCostConfigured = configured
                )
            } else {
                AiCostPaidCall(
                    label = label,
                    count = "0",
                    callType = code,
                    inputTokens = 0L,
                    outputTokens = 0L,
                    totalTokens = 0L,
                    estimatedCostLabel = "$0.00 USD",
                    isCostConfigured = true
                )
            }
        }
        val knownCodes = ORDERED_CALL_TYPES.map { it.first }.toSet()
        val extraRows = byCallType
            .filter { it.callType !in knownCodes }
            .map { dto ->
                val (costLabel, configured) = formatCostLabel(dto.resolvedEstimatedCostUsd, dto.resolvedUnknownCostCalls)
                AiCostPaidCall(
                    label = labelForCallType(dto.callType),
                    count = dto.resolvedTotalCalls.toString(),
                    callType = dto.callType,
                    inputTokens = dto.resolvedInputTokens,
                    outputTokens = dto.resolvedOutputTokens,
                    totalTokens = dto.resolvedTotalTokens,
                    estimatedCostLabel = costLabel,
                    isCostConfigured = configured
                )
            }
        return standardRows + extraRows
    }

    fun toAvoidedCalls(byCallType: List<AiUsageByCallTypeDto>): List<AiCostAvoidedCall> {
        val result = mutableListOf<AiCostAvoidedCall>()
        for (entry in byCallType) {
            val reasons = entry.avoidedByReason.orEmpty()
            if (reasons.isNotEmpty()) {
                for ((reason, count) in reasons) {
                    if (count > 0L) {
                        result += AiCostAvoidedCall(
                            callType = entry.callType,
                            reason = reason,
                            count = count
                        )
                    }
                }
            } else if (entry.resolvedAvoidedCalls > 0L) {
                result += AiCostAvoidedCall(
                    callType = entry.callType,
                    reason = "AVOIDED",
                    count = entry.resolvedAvoidedCalls
                )
            }
        }
        return result
    }

    fun toAiCostStats(
        dto: AiUsageResponseDto,
        localPagesCount: Int,
        localRegionsCount: Int,
        headlinePercent: Int,
        breakdown: List<AiCostBreakdownRow>,
        isSystemWide: Boolean = false
    ): AiCostStats {
        val byCallType = dto.byCallType.orEmpty()
        val paidCalls = toPaidCalls(byCallType)
        val avoidedCalls = toAvoidedCalls(byCallType)
        val totals = dto.totals
        val (costLabel, costConfigured) = formatCostLabel(totals.estimatedCostUsd, totals.unknownCostCalls)

        val footerNote = buildString {
            append("Este teléfono procesó $localPagesCount página(s) ($localRegionsCount regiones). ")
            append("Servidor: ${totals.totalCalls} llamada(s) pagada(s), ${totals.avoidedCalls} evitada(s), ")
            append("${totals.resolvedTotalTokens} tokens.")
        }

        return AiCostStats(
            periodLabel = "Este teléfono · $localPagesCount página(s) · $localRegionsCount regiones",
            headlinePercent = headlinePercent,
            isSystemWide = isSystemWide,
            breakdown = breakdown,
            paidCalls = paidCalls,
            footerNote = footerNote,
            serverDataAvailable = true,
            totalCalls = totals.totalCalls,
            successCalls = totals.successCalls,
            failedCalls = totals.failedCalls,
            avoidedCallsCount = totals.avoidedCalls,
            avoidedCalls = avoidedCalls,
            inputTokens = totals.resolvedInputTokens,
            outputTokens = totals.resolvedOutputTokens,
            totalTokens = totals.resolvedTotalTokens,
            estimatedCostLabel = costLabel,
            isCostConfigured = costConfigured
        )
    }
}

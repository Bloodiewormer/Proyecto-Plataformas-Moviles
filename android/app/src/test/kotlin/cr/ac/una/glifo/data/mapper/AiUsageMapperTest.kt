package cr.ac.una.glifo.data.mapper

import com.google.gson.Gson
import cr.ac.una.glifo.data.dto.AiUsageByCallTypeDto
import cr.ac.una.glifo.data.dto.AiUsageResponseDto
import cr.ac.una.glifo.data.dto.AiUsageTotalsDto
import cr.ac.una.glifo.domain.model.AiCostStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class AiUsageMapperTest {

    private val gson = Gson()

    @Test
    fun `maps all five call types, tokens, avoided calls and configured cost from backend JSON`() {
        val json = """
            {
              "scope": "USER",
              "course_id": null,
              "currency": "USD",
              "totals": {
                "total_calls": 4,
                "successful_calls": 3,
                "failed_calls": 1,
                "rejected_outputs": 0,
                "input_tokens": 2450,
                "output_tokens": 310,
                "unknown_token_calls": 0,
                "estimated_cost_usd": "0.00055350",
                "unknown_cost_calls": 0,
                "average_latency_ms": 620,
                "avoided_calls": 3
              },
              "by_call_type": [
                {
                  "call_type": "OCR_M",
                  "usage": {
                    "total_calls": 1,
                    "successful_calls": 1,
                    "failed_calls": 0,
                    "input_tokens": 600,
                    "output_tokens": 20,
                    "estimated_cost_usd": "0.00010200",
                    "unknown_cost_calls": 0,
                    "avoided_calls": 2
                  }
                },
                {
                  "call_type": "IA_01",
                  "usage": {
                    "total_calls": 3,
                    "successful_calls": 2,
                    "failed_calls": 1,
                    "input_tokens": 1850,
                    "output_tokens": 290,
                    "estimated_cost_usd": "0.00045150",
                    "unknown_cost_calls": 0,
                    "avoided_calls": 1
                  }
                }
              ]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, AiUsageResponseDto::class.java)
        val stats = AiUsageMapper.toAiCostStats(
            dto = dto,
            localPagesCount = 2,
            localRegionsCount = 8,
            headlinePercent = 75,
            breakdown = emptyList()
        )

        assertTrue(stats.serverDataAvailable)
        assertEquals(4L, stats.totalCalls)
        assertEquals(3L, stats.successCalls)
        assertEquals(1L, stats.failedCalls)
        assertEquals(3L, stats.avoidedCallsCount)
        assertEquals(2450L, stats.inputTokens)
        assertEquals(310L, stats.outputTokens)
        assertEquals(2760L, stats.totalTokens)
        assertEquals("$0.0005535 USD", stats.estimatedCostLabel)
        assertTrue(stats.isCostConfigured)

        assertEquals(5, stats.paidCalls.size)
        val ia01 = stats.paidCalls.first { it.callType == "IA_01" }
        assertEquals("3", ia01.count)
        assertEquals(2140L, ia01.totalTokens)
        assertEquals("$0.0004515 USD", ia01.estimatedCostLabel)
        assertEquals(cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_usage_call_ia_01), ia01.labelText)
        assertEquals(
            cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_usage_call_ia_02),
            stats.paidCalls.first { it.callType == "IA_02" }.labelText
        )
        assertEquals(
            cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_usage_call_ia_04),
            stats.paidCalls.first { it.callType == "IA_04" }.labelText
        )

        val ocrM = stats.paidCalls.first { it.callType == "OCR_M" }
        assertEquals("1", ocrM.count)
        assertEquals(620L, ocrM.totalTokens)
        assertEquals(cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_usage_call_ocr_m), ocrM.labelText)
        assertEquals(
            cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_usage_call_ia_00),
            stats.paidCalls.first { it.callType == "IA_00" }.labelText
        )
        assertEquals(
            cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_cost_period_this_phone, 2, 8),
            stats.periodText
        )
        assertEquals(
            cr.ac.una.glifo.core.common.UiText.Resource(cr.ac.una.glifo.R.string.ai_cost_footer_summary, 2, 8, 4L, 3L, 2760L),
            stats.footerText
        )

        assertEquals(2, stats.avoidedCalls.size)
    }

    @Test
    fun `returns Costo no configurado when estimatedCostUsd is null or unknownCostCalls is positive`() {
        val dtoWithNullCost = AiUsageResponseDto(
            scope = "USER",
            totals = AiUsageTotalsDto(
                totalCalls = 2,
                successCalls = 2,
                inputTokens = 500,
                outputTokens = 100,
                estimatedCostUsd = null,
                unknownCostCalls = 2
            ),
            byCallType = listOf(
                AiUsageByCallTypeDto(
                    callType = "IA_04",
                    totalCalls = 2,
                    inputTokens = 500,
                    outputTokens = 100,
                    estimatedCostUsd = null,
                    unknownCostCalls = 2
                )
            )
        )

        val stats = AiUsageMapper.toAiCostStats(
            dto = dtoWithNullCost,
            localPagesCount = 1,
            localRegionsCount = 4,
            headlinePercent = 50,
            breakdown = emptyList()
        )

        assertEquals(AiCostStats.COST_NOT_CONFIGURED_ES, stats.estimatedCostLabel)
        assertFalse(stats.isCostConfigured)

        val ia04Row = stats.paidCalls.first { it.callType == "IA_04" }
        assertEquals(AiCostStats.COST_NOT_CONFIGURED_ES, ia04Row.estimatedCostLabel)
        assertFalse(ia04Row.isCostConfigured)

        val (partialUnknownLabel, partialConfigured) = AiUsageMapper.formatCostLabel(
            estimatedCostUsd = BigDecimal("0.0010"),
            unknownCostCalls = 1L
        )
        assertEquals(AiCostStats.COST_NOT_CONFIGURED_ES, partialUnknownLabel)
        assertFalse(partialConfigured)
    }
}

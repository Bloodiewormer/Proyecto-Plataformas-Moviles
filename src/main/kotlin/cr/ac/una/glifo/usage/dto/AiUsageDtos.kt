package cr.ac.una.glifo.usage.dto

import com.fasterxml.jackson.annotation.JsonProperty

// Solo DTOs de salida: el uso de IA sale de ai_calls (#46), el cliente nunca lo envía. Los
// nombres del JSON van en snake_case, como el resto de la API.
//
// null en un total significa "desconocido" (ninguna llamada informó ese dato), no cero; los
// contadores unknown_* dicen cuántas llamadas quedaron fuera de cada suma. El costo va como
// texto decimal (por ejemplo "0.00120000") para no perder precisión en el cliente.

/** `GET /ai-usage/me` y `GET /courses/{id}/ai-usage`. */
data class AiUsageResponse(
    /** USER: llamadas del usuario autenticado. COURSE: todas las del curso, agregadas. */
    val scope: String,
    @JsonProperty("course_id") val courseId: Long?,
    /** Moneda de estimated_cost_usd. */
    val currency: String,
    val totals: AiUsageTotals,
    @JsonProperty("by_call_type") val byCallType: List<AiUsageByCallType>
)

data class AiUsageTotals(
    /** Solicitudes que llegaron al proveedor (las no enviadas no se registran). */
    @JsonProperty("total_calls") val totalCalls: Long,
    /** El proveedor devolvió una respuesta utilizable. */
    @JsonProperty("successful_calls") val successfulCalls: Long,
    /** Falló la llamada al proveedor (HTTP, red, respuesta inutilizable). */
    @JsonProperty("failed_calls") val failedCalls: Long,
    /** Respuestas recibidas cuyo contenido no pasó la validación de IA-01/IA-02. */
    @JsonProperty("rejected_outputs") val rejectedOutputs: Long,
    @JsonProperty("input_tokens") val inputTokens: Long?,
    @JsonProperty("output_tokens") val outputTokens: Long?,
    @JsonProperty("unknown_token_calls") val unknownTokenCalls: Long,
    @JsonProperty("estimated_cost_usd") val estimatedCostUsd: String?,
    @JsonProperty("unknown_cost_calls") val unknownCostCalls: Long,
    @JsonProperty("average_latency_ms") val averageLatencyMs: Long?,
    /**
     * Solicitudes atendidas con un resultado ya guardado, sin llamar al proveedor
     * (ai_avoided_calls). No entran en total_calls y no tienen tokens ni costo estimado.
     */
    @JsonProperty("avoided_calls") val avoidedCalls: Long
)

data class AiUsageByCallType(
    /** OCR_M, IA_00, IA_01, IA_02, IA_03 o IA_04. */
    @JsonProperty("call_type") val callType: String,
    val usage: AiUsageTotals
)

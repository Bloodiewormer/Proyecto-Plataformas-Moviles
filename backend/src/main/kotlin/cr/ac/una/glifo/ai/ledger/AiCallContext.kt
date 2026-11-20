package cr.ac.una.glifo.ai.ledger

/** Valores de `ai_calls.call_type` (CHECK ck_ai_calls_type); no se agregan otros. */
enum class AiCallType { OCR_M, IA_00, IA_01, IA_02, IA_03, IA_04 }

/** Valores de `ai_calls.level` (CHECK ck_ai_calls_level). */
private val PIPELINE_LEVELS = setOf("N0", "N1", "N1_5", "N2", "N3", "UNRESOLVED")

/**
 * Quién inició la operación que llamó a la IA. Lo arma el servicio que ya conoce al usuario
 * autenticado y al curso; el cliente HTTP no lo deduce de SecurityContext.
 */
data class AiCallOrigin(
    val userId: Long,
    val courseId: Long?
)

/**
 * Contexto de auditoría de una llamada. [level] es el nivel de la escalera de percepción
 * que la originó; null para las funciones IA-0x que no están en esa escalera.
 */
data class AiCallContext(
    val origin: AiCallOrigin,
    val callType: AiCallType,
    val level: String?
) {
    init {
        require(level == null || level in PIPELINE_LEVELS) { "Nivel de procesamiento inválido: $level" }
    }
}

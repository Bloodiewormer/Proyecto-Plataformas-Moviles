package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.AiCall
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * Totales de ai_calls para un tipo de llamada. Las sumas de tokens y costo solo cuentan los
 * valores conocidos (SUM ignora NULL) y quedan en null si ninguna fila tenía ese dato; los
 * contadores `unknown*` dicen cuántas filas no lo tenían.
 */
data class AiUsageRow(
    val callType: String,
    val totalCalls: Long,
    val successfulCalls: Long,
    val failedCalls: Long,
    val rejectedOutputs: Long,
    val inputTokens: Long?,
    val outputTokens: Long?,
    val unknownTokenCalls: Long,
    val estimatedCost: BigDecimal?,
    val unknownCostCalls: Long,
    val averageLatencyMs: Double?
)

private const val USAGE_SELECT = """
    SELECT new cr.ac.una.glifo.ops.repository.AiUsageRow(
        a.callType,
        COUNT(a),
        SUM(CASE WHEN a.success = true THEN 1L ELSE 0L END),
        SUM(CASE WHEN a.success = false THEN 1L ELSE 0L END),
        SUM(CASE WHEN a.outputAccepted = false THEN 1L ELSE 0L END),
        SUM(a.inputTokens),
        SUM(a.outputTokens),
        SUM(CASE WHEN a.inputTokens IS NULL OR a.outputTokens IS NULL THEN 1L ELSE 0L END),
        SUM(a.estimatedCost),
        SUM(CASE WHEN a.estimatedCost IS NULL THEN 1L ELSE 0L END),
        AVG(a.latencyMs)
    )
    FROM AiCall a
"""

@Repository
interface AiCallRepository : JpaRepository<AiCall, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<AiCall>
    fun findByUserIdAndCallType(userId: Long, callType: String): List<AiCall>
    fun countByUserId(userId: Long): Long

    /** Uso propio; [courseId] null = todos los cursos (y las llamadas sin curso). */
    @Query(
        USAGE_SELECT + """
        WHERE a.user.id = :userId AND (:courseId IS NULL OR a.course.id = :courseId)
        GROUP BY a.callType ORDER BY a.callType
        """
    )
    fun usageByUser(@Param("userId") userId: Long, @Param("courseId") courseId: Long?): List<AiUsageRow>

    /** Uso de todos los usuarios dentro de un curso, solo agregado. */
    @Query(USAGE_SELECT + "WHERE a.course.id = :courseId GROUP BY a.callType ORDER BY a.callType")
    fun usageByCourse(@Param("courseId") courseId: Long): List<AiUsageRow>
}

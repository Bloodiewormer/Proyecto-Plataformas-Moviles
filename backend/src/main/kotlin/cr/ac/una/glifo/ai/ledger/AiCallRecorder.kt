package cr.ac.una.glifo.ai.ledger

import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.ops.entity.AiAvoidedCall
import cr.ac.una.glifo.ops.entity.AiCall
import cr.ac.una.glifo.ops.repository.AiAvoidedCallRepository
import cr.ac.una.glifo.ops.repository.AiCallRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/** Una fila de `ai_calls` ya calculada; ver AiCall para la semántica de cada campo. */
data class AiCallRecord(
    val context: AiCallContext,
    val model: String?,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val estimatedCostUsd: BigDecimal?,
    val latencyMs: Long,
    val success: Boolean,
    val outputAccepted: Boolean?,
    val errorCode: String?,
    val errorMessage: String?
)

/**
 * Guarda cada registro en su PROPIA transacción (REQUIRES_NEW): si la operación de negocio
 * que hizo la llamada se revierte después, la llamada ya pagada sigue registrada.
 *
 * Es un bean aparte de CostLedgerService a propósito: @Transactional solo se aplica cuando
 * se entra por el proxy de Spring, y una llamada interna (this.save) lo saltaría.
 */
@Component
class AiCallRecorder(
    private val aiCallRepository: AiCallRepository,
    private val aiAvoidedCallRepository: AiAvoidedCallRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun save(record: AiCallRecord): Long {
        val call = AiCall().apply {
            user = userRepository.getReferenceById(record.context.origin.userId)
            course = record.context.origin.courseId?.let { courseRepository.getReferenceById(it) }
            callType = record.context.callType.name
            level = record.context.level
            model = record.model?.take(100)
            inputTokens = record.inputTokens
            outputTokens = record.outputTokens
            estimatedCost = record.estimatedCostUsd
            latencyMs = record.latencyMs.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
            success = record.success
            outputAccepted = record.outputAccepted
            errorCode = record.errorCode
            errorMessage = record.errorMessage
        }
        // saveAndFlush: si el INSERT falla (FK, CHECK), falla acá y no al confirmar.
        return aiCallRepository.saveAndFlush(call).id
    }

    /** Una llamada que no se hizo porque se reutilizó un resultado guardado (ai_avoided_calls). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveAvoided(context: AiCallContext): Long {
        val avoided = AiAvoidedCall().apply {
            user = userRepository.getReferenceById(context.origin.userId)
            course = context.origin.courseId?.let { courseRepository.getReferenceById(it) }
            callType = context.callType.name
            level = context.level
        }
        return aiAvoidedCallRepository.saveAndFlush(avoided).id
    }
}

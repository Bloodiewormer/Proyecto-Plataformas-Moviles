package cr.ac.una.glifo.usage.service

import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.ops.repository.AiAvoidedCallRepository
import cr.ac.una.glifo.ops.repository.AiAvoidedRow
import cr.ac.una.glifo.ops.repository.AiCallRepository
import cr.ac.una.glifo.ops.repository.AiUsageRow
import cr.ac.una.glifo.usage.dto.AiUsageByCallType
import cr.ac.una.glifo.usage.dto.AiUsageResponse
import cr.ac.una.glifo.usage.dto.AiUsageTotals
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.math.roundToLong

/**
 * Lectura del uso de IA registrado en ai_calls y ai_avoided_calls (#46). Solo agrega: nunca
 * devuelve filas sueltas, así que el uso del curso no expone qué hizo cada estudiante.
 *
 * Las llamadas anteriores a #46 de una base actualizada pueden tener success = NULL: cuentan
 * en total_calls pero ni como exitosas ni como fallidas.
 */
@Service
class AiUsageService(
    private val aiCallRepository: AiCallRepository,
    private val aiAvoidedCallRepository: AiAvoidedCallRepository,
    private val courseRepository: CourseRepository
) {

    /** Uso del usuario autenticado; [courseId] opcional filtra por curso. */
    @Transactional(readOnly = true)
    fun getOwnUsage(userId: Long, courseId: Long?): AiUsageResponse =
        toResponse(
            "USER", courseId,
            aiCallRepository.usageByUser(userId, courseId),
            aiAvoidedCallRepository.avoidedByUser(userId, courseId)
        )

    /**
     * Uso agregado de un curso. Solo para el docente dueño: un curso inexistente es 404 y uno
     * ajeno 403, igual que el resto de las operaciones de docente sobre un curso.
     */
    @Transactional(readOnly = true)
    fun getCourseUsage(courseId: Long, userId: Long): AiUsageResponse {
        val course = courseRepository.findById(courseId)
            .orElseThrow { ResourceNotFoundException("Curso no encontrado") }
        if (course.owner?.id != userId) {
            throw AccessDeniedException("Solo el docente dueño del curso puede ver su uso de IA")
        }
        return toResponse(
            "COURSE", courseId,
            aiCallRepository.usageByCourse(courseId),
            aiAvoidedCallRepository.avoidedByCourse(courseId)
        )
    }

    private fun toResponse(scope: String, courseId: Long?, rows: List<AiUsageRow>, avoided: List<AiAvoidedRow>): AiUsageResponse {
        val avoidedByType = avoided.associate { it.callType to it.avoidedCalls }
        // Un tipo aparece si tuvo llamadas reales, evitadas o ambas.
        val callTypes = (rows.map { it.callType } + avoidedByType.keys).distinct().sorted()
        return AiUsageResponse(
            scope = scope,
            courseId = courseId,
            currency = "USD",
            totals = totalsOf(rows, avoidedByType.values.sum()),
            byCallType = callTypes.map { type ->
                AiUsageByCallType(type, totalsOf(rows.filter { it.callType == type }, avoidedByType[type] ?: 0L))
            }
        )
    }

    private fun totalsOf(rows: List<AiUsageRow>, avoidedCalls: Long): AiUsageTotals {
        val totalCalls = rows.sumOf { it.totalCalls }
        // Promedio ponderado por cantidad de llamadas de cada tipo.
        val averageLatency = if (totalCalls == 0L) null else
            rows.sumOf { (it.averageLatencyMs ?: 0.0) * it.totalCalls } / totalCalls
        return AiUsageTotals(
            totalCalls = totalCalls,
            successfulCalls = rows.sumOf { it.successfulCalls },
            failedCalls = rows.sumOf { it.failedCalls },
            rejectedOutputs = rows.sumOf { it.rejectedOutputs },
            inputTokens = sumKnown(rows.map { it.inputTokens }),
            outputTokens = sumKnown(rows.map { it.outputTokens }),
            unknownTokenCalls = rows.sumOf { it.unknownTokenCalls },
            estimatedCostUsd = rows.mapNotNull { it.estimatedCost }
                .takeIf { it.isNotEmpty() }
                ?.fold(BigDecimal.ZERO, BigDecimal::add)
                ?.toPlainString(),
            unknownCostCalls = rows.sumOf { it.unknownCostCalls },
            averageLatencyMs = averageLatency?.roundToLong(),
            avoidedCalls = avoidedCalls
        )
    }

    /** Suma lo conocido; null si ningún tipo tenía el dato. */
    private fun sumKnown(values: List<Long?>): Long? =
        values.filterNotNull().takeIf { it.isNotEmpty() }?.sum()
}

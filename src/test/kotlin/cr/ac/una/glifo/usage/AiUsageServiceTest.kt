package cr.ac.una.glifo.usage

import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.ops.repository.AiAvoidedCallRepository
import cr.ac.una.glifo.ops.repository.AiAvoidedRow
import cr.ac.una.glifo.ops.repository.AiCallRepository
import cr.ac.una.glifo.ops.repository.AiUsageRow
import cr.ac.una.glifo.usage.service.AiUsageService
import cr.ac.una.glifo.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pruebas UNITARIAS de AiUsageService: el repositorio devuelve filas ya agregadas simuladas. */
class AiUsageServiceTest {

    private lateinit var aiCallRepository: AiCallRepository
    private lateinit var aiAvoidedCallRepository: AiAvoidedCallRepository
    private lateinit var courseRepository: CourseRepository
    private lateinit var service: AiUsageService

    private val teacher = User().apply { id = 2L }
    private val course = Course().apply { id = 10L; owner = teacher }

    private fun row(
        callType: String,
        total: Long,
        success: Long,
        failed: Long,
        rejected: Long = 0,
        input: Long? = null,
        output: Long? = null,
        unknownTokens: Long = 0,
        cost: BigDecimal? = null,
        unknownCost: Long = 0,
        latency: Double? = null
    ) = AiUsageRow(callType, total, success, failed, rejected, input, output, unknownTokens, cost, unknownCost, latency)

    @BeforeEach
    fun setUp() {
        aiCallRepository = mock(AiCallRepository::class.java)
        courseRepository = mock(CourseRepository::class.java)
        aiAvoidedCallRepository = mock(AiAvoidedCallRepository::class.java)
        service = AiUsageService(aiCallRepository, aiAvoidedCallRepository, courseRepository)
    }

    @Test
    fun `los totales suman lo conocido y cuentan aparte lo desconocido`() {
        `when`(aiCallRepository.usageByUser(1L, null)).thenReturn(
            listOf(
                // IA_01: 2 llamadas exitosas con tokens y costo conocidos.
                row("IA_01", total = 2, success = 2, failed = 0, input = 400, output = 300, cost = BigDecimal("0.00120000"), latency = 500.0),
                // IA_02: 1 éxito con contenido rechazado y sin tarifa + 1 fallo sin tokens.
                row("IA_02", total = 2, success = 1, failed = 1, rejected = 1, input = 100, output = 50, unknownTokens = 1, unknownCost = 2, latency = 1000.0)
            )
        )

        val usage = service.getOwnUsage(1L, null)

        assertEquals("USER", usage.scope)
        assertNull(usage.courseId)
        assertEquals("USD", usage.currency)
        val totals = usage.totals
        assertEquals(4, totals.totalCalls)
        assertEquals(3, totals.successfulCalls)
        assertEquals(1, totals.failedCalls)
        assertEquals(1, totals.rejectedOutputs)
        assertEquals(500, totals.inputTokens)
        assertEquals(350, totals.outputTokens)
        assertEquals(1, totals.unknownTokenCalls)
        // Solo IA_01 tiene costo: el total no inventa el de IA_02, lo cuenta como desconocido.
        assertEquals("0.00120000", totals.estimatedCostUsd)
        assertEquals(2, totals.unknownCostCalls)
        // Promedio ponderado: (500 × 2 + 1000 × 2) / 4 = 750
        assertEquals(750, totals.averageLatencyMs)
        assertEquals(listOf("IA_01", "IA_02"), usage.byCallType.map { it.callType })
        assertNull(usage.byCallType[1].usage.estimatedCostUsd)
    }

    @Test
    fun `sin llamadas los totales son cero y lo desconocido es null, no cero`() {
        `when`(aiCallRepository.usageByUser(1L, 10L)).thenReturn(emptyList())

        val usage = service.getOwnUsage(1L, 10L)

        assertEquals(10L, usage.courseId)
        assertEquals(0, usage.totals.totalCalls)
        assertNull(usage.totals.inputTokens)
        assertNull(usage.totals.estimatedCostUsd)
        assertNull(usage.totals.averageLatencyMs)
        assertEquals(emptyList(), usage.byCallType)
    }

    @Test
    fun `el costo va como decimal plano aunque sea muy chico`() {
        `when`(aiCallRepository.usageByUser(1L, null)).thenReturn(
            listOf(row("IA_02", total = 1, success = 1, failed = 0, input = 1, output = 0, cost = BigDecimal("0.00000001")))
        )

        assertEquals("0.00000001", service.getOwnUsage(1L, null).totals.estimatedCostUsd)
    }

    @Test
    fun `las llamadas evitadas se cuentan aparte y no cambian los totales de llamadas reales`() {
        `when`(aiCallRepository.usageByUser(1L, null)).thenReturn(
            listOf(row("IA_02", total = 1, success = 1, failed = 0, input = 100, output = 50, cost = BigDecimal("0.00010000")))
        )
        `when`(aiAvoidedCallRepository.avoidedByUser(1L, null)).thenReturn(
            listOf(AiAvoidedRow("IA_01", 2), AiAvoidedRow("IA_02", 3))
        )

        val usage = service.getOwnUsage(1L, null)

        assertEquals(1, usage.totals.totalCalls)
        assertEquals("0.00010000", usage.totals.estimatedCostUsd)
        assertEquals(5, usage.totals.avoidedCalls)
        // IA_01 solo tuvo evitadas: aparece con cero llamadas reales y sin tokens, costo ni latencia.
        assertEquals(listOf("IA_01", "IA_02"), usage.byCallType.map { it.callType })
        val ia01 = usage.byCallType[0].usage
        assertEquals(0, ia01.totalCalls)
        assertEquals(2, ia01.avoidedCalls)
        assertNull(ia01.inputTokens)
        assertNull(ia01.estimatedCostUsd)
        assertNull(ia01.averageLatencyMs)
        assertEquals(3, usage.byCallType[1].usage.avoidedCalls)
    }

    @Test
    fun `el docente duenno ve el uso agregado de su curso`() {
        `when`(courseRepository.findById(10L)).thenReturn(Optional.of(course))
        `when`(aiCallRepository.usageByCourse(10L)).thenReturn(listOf(row("IA_02", total = 3, success = 3, failed = 0)))

        val usage = service.getCourseUsage(10L, userId = 2L)

        assertEquals("COURSE", usage.scope)
        assertEquals(10L, usage.courseId)
        assertEquals(3, usage.totals.totalCalls)
    }

    @Test
    fun `otro usuario no puede ver el uso del curso`() {
        `when`(courseRepository.findById(10L)).thenReturn(Optional.of(course))

        assertThrows<AccessDeniedException> { service.getCourseUsage(10L, userId = 99L) }
        verify(aiCallRepository, never()).usageByCourse(anyLong())
    }

    @Test
    fun `curso inexistente es ResourceNotFoundException`() {
        `when`(courseRepository.findById(404L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getCourseUsage(404L, userId = 2L) }
    }
}

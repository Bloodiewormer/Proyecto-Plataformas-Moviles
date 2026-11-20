package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.AiPricingProperties
import cr.ac.una.glifo.ai.config.AiPricingProperties.ModelRate
import cr.ac.una.glifo.ai.ledger.AiCallContext
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
import cr.ac.una.glifo.ai.ledger.AiCallRecord
import cr.ac.una.glifo.ai.ledger.AiCallRecorder
import cr.ac.una.glifo.ai.ledger.AiCallType
import cr.ac.una.glifo.ai.ledger.CostCalculator
import cr.ac.una.glifo.ai.ledger.CostLedgerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.dao.DataAccessResourceFailureException
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Pruebas UNITARIAS de CostLedgerService: el proveedor es una lambda simulada (sin OpenAI)
 * y el guardado un mock de AiCallRecorder, así se ve exactamente qué fila se arma.
 */
class CostLedgerServiceTest {

    private lateinit var recorder: AiCallRecorder
    private lateinit var ledger: CostLedgerService
    private var nowNanos = 0L

    private val context = AiCallContext(AiCallOrigin(userId = 1L, courseId = 10L), AiCallType.IA_02, level = null)
    private val pricing = AiPricingProperties(mapOf("modelo-a" to ModelRate(BigDecimal("0.50"), BigDecimal("2.00"))))

    private fun result(model: String = "modelo-a", input: Int? = 1200, output: Int? = 300) =
        LlmResult(content = "{}", model = model, inputTokens = input, outputTokens = output, latencyMs = 640L)

    private val placeholder = AiCallRecord(context, null, null, null, null, 0L, false, null, null, null)

    @BeforeEach
    fun setUp() {
        recorder = mock(AiCallRecorder::class.java)
        nowNanos = 0L
        ledger = CostLedgerService(recorder, CostCalculator(pricing)) { nowNanos }
    }

    private fun savedRecord(): AiCallRecord {
        val captor = ArgumentCaptor.forClass(AiCallRecord::class.java)
        verify(recorder, times(1)).save(captor.capture() ?: placeholder)
        return captor.value
    }

    @Test
    fun `respuesta aceptada guarda contexto, modelo, consumo, costo y latencia del proveedor`() {
        val value = ledger.track(context, call = { result() }, accept = { "ok" })

        assertEquals("ok", value)
        val record = savedRecord()
        assertEquals(context, record.context)
        assertEquals("modelo-a", record.model)
        assertEquals(1200, record.inputTokens)
        assertEquals(300, record.outputTokens)
        assertEquals(BigDecimal("0.00120000"), record.estimatedCostUsd)
        assertEquals(640L, record.latencyMs)
        assertEquals(true, record.success)
        assertEquals(true, record.outputAccepted)
        assertNull(record.errorCode)
        assertNull(record.errorMessage)
    }

    @Test
    fun `tokens no informados o modelo sin tarifa quedan como desconocidos`() {
        ledger.track(context, call = { result(model = "otro-modelo", input = null, output = null) }, accept = { })

        val record = savedRecord()
        assertNull(record.inputTokens)
        assertNull(record.outputTokens)
        assertNull(record.estimatedCostUsd)
        assertEquals(true, record.success)
    }

    @Test
    fun `fallo del proveedor guarda success false con latencia medida aunque no haya LlmResult`() {
        val original = AiProviderException("No se pudo obtener respuesta del proveedor de IA", cause = RuntimeException("boom"))

        val thrown = assertThrows<AiProviderException> {
            ledger.track<Unit>(context, call = { nowNanos += 1_234_000_000L; throw original }, accept = { })
        }

        assertSame(original, thrown)
        val record = savedRecord()
        assertEquals(false, record.success)
        assertNull(record.outputAccepted)
        assertEquals(1234L, record.latencyMs)
        assertNull(record.model)
        assertNull(record.inputTokens)
        assertNull(record.estimatedCostUsd)
        assertEquals("PROVIDER_ERROR", record.errorCode)
    }

    @Test
    fun `respuesta recibida pero rechazada por el esquema es success true y outputAccepted false`() {
        val rejection = AiProviderException("El JSON devuelto por la IA no contiene el arreglo 'items'")

        val thrown = assertThrows<AiProviderException> {
            ledger.track<Unit>(context, call = { result() }, accept = { throw rejection })
        }

        assertSame(rejection, thrown)
        val record = savedRecord()
        assertEquals(true, record.success)
        assertEquals(false, record.outputAccepted)
        assertEquals("OUTPUT_REJECTED", record.errorCode)
        assertEquals("El JSON devuelto por la IA no contiene el arreglo 'items'", record.errorMessage)
        // La llamada se pagó igual: consumo y costo se conservan.
        assertEquals(BigDecimal("0.00120000"), record.estimatedCostUsd)
    }

    @Test
    fun `IA sin configurar no es una llamada y no se registra`() {
        val notConfigured = AiProviderException.notConfigured()

        val thrown = assertThrows<AiProviderException> {
            ledger.track<Unit>(context, call = { throw notConfigured }, accept = { })
        }

        assertSame(notConfigured, thrown)
        verify(recorder, never()).save(any(AiCallRecord::class.java) ?: placeholder)
    }

    @Test
    fun `si falla la auditoria la generacion exitosa devuelve su resultado`() {
        `when`(recorder.save(any(AiCallRecord::class.java) ?: placeholder))
            .thenThrow(DataAccessResourceFailureException("Connection refused"))

        val value = ledger.track(context, call = { result() }, accept = { "items generados" })

        assertEquals("items generados", value)
        verify(recorder, times(1)).save(any(AiCallRecord::class.java) ?: placeholder)
    }

    @Test
    fun `si falla la auditoria de un fallo se relanza la excepcion original del proveedor`() {
        `when`(recorder.save(any(AiCallRecord::class.java) ?: placeholder))
            .thenThrow(DataAccessResourceFailureException("Connection refused"))
        val original = AiProviderException("El proveedor de IA devolvió una respuesta vacía")

        val thrown = assertThrows<AiProviderException> {
            ledger.track<Unit>(context, call = { throw original }, accept = { })
        }

        assertSame(original, thrown)
    }

    @Test
    fun `cada llamada real produce exactamente un registro`() {
        ledger.track(context, call = { result() }, accept = { })
        assertThrows<AiProviderException> {
            ledger.track<Unit>(context, call = { result() }, accept = { throw AiProviderException("x") })
        }
        assertThrows<AiProviderException> {
            ledger.track<Unit>(context, call = { throw AiProviderException("y") }, accept = { })
        }

        verify(recorder, times(3)).save(any(AiCallRecord::class.java) ?: placeholder)
    }

    @Test
    fun `el contexto rechaza niveles que no estan en el CHECK de ai_calls`() {
        assertThrows<IllegalArgumentException> { AiCallContext(context.origin, AiCallType.IA_00, level = "N4") }
        assertEquals("N2", AiCallContext(context.origin, AiCallType.IA_00, level = "N2").level)
    }
}

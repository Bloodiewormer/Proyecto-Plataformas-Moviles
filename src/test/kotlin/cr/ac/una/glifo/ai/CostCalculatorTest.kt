package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.config.AiPricingProperties
import cr.ac.una.glifo.ai.config.AiPricingProperties.ModelRate
import cr.ac.una.glifo.ai.ledger.CostCalculator
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Costo estimado de ai_calls (#46). Las tarifas son de EJEMPLO, elegidas para que las
 * cuentas se puedan seguir a mano; no son precios reales de ningún proveedor.
 */
class CostCalculatorTest {

    private val calculator = CostCalculator(
        AiPricingProperties(
            rates = mapOf(
                "modelo-a" to ModelRate(BigDecimal("0.50"), BigDecimal("2.00")),
                "modelo-b" to ModelRate(BigDecimal("3.00"), BigDecimal("12.00")),
                "solo-entrada" to ModelRate(inputUsdPerMillion = BigDecimal("1.00")),
                "negativa" to ModelRate(BigDecimal("-1.00"), BigDecimal("1.00"))
            )
        )
    )

    @Test
    fun `costo = entrada por tarifa mas salida por tarifa, por millon de tokens`() {
        // 1200 × 0.50 / 1e6 + 300 × 2.00 / 1e6 = 0.0006 + 0.0006 = 0.0012 USD
        assertEquals(BigDecimal("0.00120000"), calculator.estimate("modelo-a", 1200, 300))
    }

    @Test
    fun `cada modelo usa su propia tarifa`() {
        // 1200 × 3 / 1e6 + 300 × 12 / 1e6 = 0.0036 + 0.0036 = 0.0072 USD
        assertEquals(BigDecimal("0.00720000"), calculator.estimate("modelo-b", 1200, 300))
    }

    @Test
    fun `se redondea una sola vez a 8 decimales con HALF_UP`() {
        // 3 × 0.50 / 1e6 + 1 × 2 / 1e6 = 0.0000035 → 0.00000350 (cabe exacto)
        assertEquals(BigDecimal("0.00000350"), calculator.estimate("modelo-a", 3, 1))
        // Tarifa 0.005 USD/millón: 1 token cuesta 0.000000005, que no cabe en 8 decimales → 0.00000001 (HALF_UP)
        val tiny = CostCalculator(AiPricingProperties(mapOf("m" to ModelRate(BigDecimal("0.005"), BigDecimal.ZERO))))
        assertEquals(BigDecimal("0.00000001"), tiny.estimate("m", 1, 0))
        assertEquals(8, calculator.estimate("modelo-a", 7, 7)!!.scale())
    }

    @Test
    fun `cero tokens informados es un costo conocido de cero`() {
        assertEquals(BigDecimal("0.00000000"), calculator.estimate("modelo-a", 0, 0))
    }

    @Test
    fun `sin tarifa el costo es desconocido, no cero`() {
        assertNull(calculator.estimate("modelo-sin-tarifa", 1200, 300))
        assertNull(calculator.estimate("solo-entrada", 1200, 300))
        assertNull(calculator.estimate("negativa", 1200, 300))
        assertNull(CostCalculator(AiPricingProperties()).estimate("modelo-a", 1200, 300))
    }

    @Test
    fun `sin consumo informado o sin modelo el costo es desconocido`() {
        assertNull(calculator.estimate("modelo-a", null, 300))
        assertNull(calculator.estimate("modelo-a", 1200, null))
        assertNull(calculator.estimate(null, 1200, 300))
        assertNull(calculator.estimate(" ", 1200, 300))
    }

    @Test
    fun `una version fechada usa la tarifa de su modelo y nada mas`() {
        assertEquals(BigDecimal("0.00120000"), calculator.estimate("modelo-a-2024-07-18", 1200, 300))
        // No es una versión fechada de modelo-a: no hereda su tarifa.
        assertNull(calculator.estimate("modelo-a-mini", 1200, 300))
        assertNull(calculator.estimate("modelo-a-2024", 1200, 300))
    }
}

package cr.ac.una.glifo.ai.ledger

import cr.ac.una.glifo.ai.config.AiPricingProperties
import java.math.BigDecimal
import java.math.RoundingMode

/** Decimales del costo guardado; coincide con `ai_calls.estimated_cost numeric(12,8)`. */
const val COST_SCALE = 8

/** Sufijo de las versiones fechadas de un modelo, como `gpt-4o-mini-2024-07-18`. */
private val DATED_SNAPSHOT_SUFFIX = Regex("-\\d{4}-\\d{2}-\\d{2}$")

/**
 * Costo estimado en USD de una llamada:
 *
 *     costo = tokensEntrada × tarifaEntrada / 1e6 + tokensSalida × tarifaSalida / 1e6
 *
 * Se calcula exacto con BigDecimal y se redondea una sola vez al final a [COST_SCALE]
 * decimales con HALF_UP. Devuelve null (desconocido) si falta el modelo, alguna de las dos
 * tarifas o alguno de los dos conteos de tokens.
 *
 * Búsqueda de tarifa: primero el nombre exacto que reportó el proveedor; si no está y es una
 * versión fechada (`<modelo>-AAAA-MM-DD`), la tarifa de `<modelo>`. No hay otra
 * equivalencia: un modelo sin tarifa propia queda sin costo.
 */
class CostCalculator(private val pricing: AiPricingProperties) {

    fun estimate(model: String?, inputTokens: Int?, outputTokens: Int?): BigDecimal? {
        if (model.isNullOrBlank() || inputTokens == null || outputTokens == null) return null
        val rate = rateFor(model) ?: return null
        val inputRate = rate.inputUsdPerMillion?.takeIf { it.signum() >= 0 } ?: return null
        val outputRate = rate.outputUsdPerMillion?.takeIf { it.signum() >= 0 } ?: return null

        val exact = BigDecimal(inputTokens).multiply(inputRate)
            .add(BigDecimal(outputTokens).multiply(outputRate))
            .movePointLeft(6)
        return exact.setScale(COST_SCALE, RoundingMode.HALF_UP)
    }

    private fun rateFor(model: String): AiPricingProperties.ModelRate? =
        pricing.rates[model] ?: pricing.rates[model.replace(DATED_SNAPSHOT_SUFFIX, "")]
}

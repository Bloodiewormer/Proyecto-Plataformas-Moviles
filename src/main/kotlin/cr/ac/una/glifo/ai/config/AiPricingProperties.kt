package cr.ac.una.glifo.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

/**
 * Tarifas para estimar el costo de cada llamada (issue #46), en USD por millón de tokens.
 * Sin tarifa para un modelo su costo queda como desconocido (NULL), nunca en 0. Se
 * configuran en application.properties; la clave va entre corchetes porque los nombres de
 * modelo llevan guiones y puntos:
 *
 *     glifo.ai.pricing.rates[gpt-4o-mini].input-usd-per-million=...
 *     glifo.ai.pricing.rates[gpt-4o-mini].output-usd-per-million=...
 */
@ConfigurationProperties("glifo.ai.pricing")
data class AiPricingProperties(
    val rates: Map<String, ModelRate> = emptyMap()
) {
    data class ModelRate(
        val inputUsdPerMillion: BigDecimal? = null,
        val outputUsdPerMillion: BigDecimal? = null
    )
}

@Configuration
@EnableConfigurationProperties(AiPricingProperties::class)
class AiPricingPropertiesConfig

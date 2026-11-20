package cr.ac.una.glifo.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties("openai")
data class OpenAiProperties(
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val baseUrl: String = "https://api.openai.com/v1",
    val timeoutSeconds: Long = 60
) {
    override fun toString(): String =
        "OpenAiProperties(model='$model', baseUrl='$baseUrl', timeoutSeconds=$timeoutSeconds, apiKeyConfigured=${apiKey.isNotBlank()})"
}

@Configuration
@EnableConfigurationProperties(OpenAiProperties::class)
class OpenAiPropertiesConfig

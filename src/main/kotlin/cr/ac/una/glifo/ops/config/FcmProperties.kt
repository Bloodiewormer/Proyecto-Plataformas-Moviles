package cr.ac.una.glifo.ops.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties("glifo.fcm")
data class FcmProperties(
    val enabled: Boolean = true,
    val projectId: String = "glifoapp",
    val credentialsPath: String = "",
    val credentialsFile: String = "",
    val useApplicationDefault: Boolean = false,
    val maxAttempts: Int = 3,
    val retryBackoffMs: Long = 200,
) {
    override fun toString(): String =
        "FcmProperties(enabled=$enabled, projectId='$projectId', credentialsConfigured=${credentialsPath.isNotBlank() || credentialsFile.isNotBlank() || useApplicationDefault}, maxAttempts=$maxAttempts, retryBackoffMs=$retryBackoffMs)"
}

@Configuration
@EnableConfigurationProperties(FcmProperties::class)
class FcmPropertiesConfig

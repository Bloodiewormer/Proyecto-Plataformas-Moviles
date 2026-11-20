package cr.ac.una.glifo.ai

open class AiProviderException(
    override val message: String,
    val code: String = "AI_PROVIDER_ERROR",
    val isNotConfigured: Boolean = false,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    companion object {
        fun notConfigured(message: String = "La IA no está configurada en el servidor"): AiProviderException =
            AiProviderException(
                message = message,
                code = "AI_NOT_CONFIGURED",
                isNotConfigured = true
            )
    }
}

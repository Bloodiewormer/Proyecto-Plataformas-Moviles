package cr.ac.una.glifo.ops.fcm

/**
 * Mensaje push dirigido exclusivamente a un dispositivo individual mediante su token FCM.
 * No expone envío por topic ni difusión a múltiples tokens.
 */
data class FcmTargetedMessage(
    val fcmToken: String,
    val collapseKey: String,
    val data: Map<String, String>,
) {
    override fun toString(): String =
        "FcmTargetedMessage(token='${TokenSanitizer.mask(fcmToken)}', collapseKey='$collapseKey', dataKeys=${data.keys})"
}

/**
 * Clasificación explícita de fallos de envío en Firebase Cloud Messaging.
 *
 * - [retryable]: indica si vale la pena reintentar de forma acotada (`UNAVAILABLE`, `INTERNAL`,
 *   `QUOTA_EXCEEDED`, `NETWORK_TIMEOUT`).
 * - [invalidatesToken]: indica si el token FCM dejó de ser válido para esa instalación
 *   (`UNREGISTERED`). Un error de argumento (`INVALID_ARGUMENT`) o falta de credenciales
 *   (`NOT_CONFIGURED`) nunca invalida el token del dispositivo.
 */
enum class FcmErrorKind(
    val retryable: Boolean,
    val invalidatesToken: Boolean,
) {
    UNREGISTERED(retryable = false, invalidatesToken = true),
    INVALID_ARGUMENT(retryable = false, invalidatesToken = false),
    SENDER_ID_MISMATCH(retryable = false, invalidatesToken = false),
    THIRD_PARTY_AUTH_ERROR(retryable = false, invalidatesToken = false),
    NOT_CONFIGURED(retryable = false, invalidatesToken = false),
    UNAVAILABLE(retryable = true, invalidatesToken = false),
    INTERNAL(retryable = true, invalidatesToken = false),
    QUOTA_EXCEEDED(retryable = true, invalidatesToken = false),
    NETWORK_TIMEOUT(retryable = true, invalidatesToken = false),
    UNKNOWN(retryable = false, invalidatesToken = false),
}

sealed interface FcmSendOutcome {
    /**
     * FCM aceptó el mensaje para su despacho. No implica que el dispositivo ya lo haya
     * recibido ni mostrado en la barra de estado del sistema operativo.
     */
    data class Accepted(val messageId: String) : FcmSendOutcome

    data class Failed(
        val errorKind: FcmErrorKind,
        val errorCode: String,
        val sanitizedMessage: String,
    ) : FcmSendOutcome
}

/**
 * Puerto de envío de notificaciones push mediante Firebase Cloud Messaging.
 * Permite usar el SDK oficial de Firebase Admin en ejecución real y sustituirlo limpiamente
 * por dobles deterministas en las pruebas automatizadas.
 */
interface FcmClient {
    fun send(message: FcmTargetedMessage): FcmSendOutcome
}

object TokenSanitizer {
    private const val MAX_ERROR_LENGTH = 450

    fun mask(token: String?): String {
        val clean = token?.trim().orEmpty()
        if (clean.isEmpty()) return "<sin-token>"
        if (clean.length <= 10) return "***"
        return "${clean.take(4)}...${clean.takeLast(4)}(len=${clean.length})"
    }

    fun sanitizeError(rawMessage: String?, sensitiveToken: String? = null): String {
        var text = rawMessage?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        if (text.isEmpty()) {
            text = "Error de comunicación con FCM"
        }
        val cleanToken = sensitiveToken?.trim().orEmpty()
        if (cleanToken.length > 4 && text.contains(cleanToken)) {
            text = text.replace(cleanToken, mask(cleanToken))
        }
        return text.take(MAX_ERROR_LENGTH)
    }
}

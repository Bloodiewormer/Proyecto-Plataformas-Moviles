package cr.ac.una.glifo.ops.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import cr.ac.una.glifo.ops.config.FcmProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Implementación real de [FcmClient] sobre Firebase Admin SDK.
 *
 * - Carga credenciales desde `glifo.fcm.credentials-path`, `glifo.fcm.credentials-file`,
 *   `GLIFO_FCM_CREDENTIALS_PATH`, `FCM_CREDENTIALS_PATH`, `GOOGLE_APPLICATION_CREDENTIALS`
 *   o Application Default Credentials si se habilitó explícitamente.
 * - Si no hay credenciales válidas configuradas, devuelve `FcmErrorKind.NOT_CONFIGURED` y
 *   nunca simula un envío exitoso.
 * - Nunca escribe tokens completos ni secretos en los logs.
 */
@Component
class FirebaseAdminFcmClient @Autowired constructor(
    private val properties: FcmProperties,
    private val environment: Environment? = null,
) : FcmClient {

    private val log = LoggerFactory.getLogger(FirebaseAdminFcmClient::class.java)
    private val appLock = Any()

    @PostConstruct
    fun logStartupStatus() {
        if (!resolveEnabled()) {
            log.info("FCM deshabilitado por configuración (glifo.fcm.enabled=false)")
            return
        }
        val explicitPath = resolveExplicitCredentialsPath()
        val projectId = resolveProjectId()
        if (explicitPath.isNotEmpty()) {
            val path = runCatching { Path.of(explicitPath) }.getOrNull()
            if (path != null && Files.isRegularFile(path) && Files.isReadable(path)) {
                log.info(
                    "FCM configurado correctamente (projectId={}, credentialsFile={})",
                    projectId,
                    path.fileName,
                )
            } else {
                log.warn(
                    "FCM: el archivo indicado en la configuración no existe o no es legible ({})",
                    path?.fileName ?: "ruta inválida",
                )
            }
        } else if (properties.useApplicationDefault) {
            log.info("FCM configurado con Application Default Credentials (projectId={})", projectId)
        } else {
            log.warn("FCM sin credenciales configuradas: defina GLIFO_FCM_CREDENTIALS_PATH o FCM_CREDENTIALS_PATH en .env")
        }
    }

    override fun send(message: FcmTargetedMessage): FcmSendOutcome {
        if (!resolveEnabled()) {
            return FcmSendOutcome.Failed(
                errorKind = FcmErrorKind.NOT_CONFIGURED,
                errorCode = "FCM_DISABLED",
                sanitizedMessage = "El envío FCM está deshabilitado por configuración",
            )
        }

        val token = message.fcmToken.trim()
        if (token.isEmpty()) {
            return FcmSendOutcome.Failed(
                errorKind = FcmErrorKind.INVALID_ARGUMENT,
                errorCode = "EMPTY_FCM_TOKEN",
                sanitizedMessage = "El dispositivo no tiene un token FCM activo registrado",
            )
        }

        val firebaseMessaging = try {
            resolveFirebaseMessaging()
        } catch (ex: FcmNotConfiguredException) {
            log.warn("FCM no configurado al intentar enviar notificación (token={}): {}", TokenSanitizer.mask(token), ex.message)
            return FcmSendOutcome.Failed(
                errorKind = FcmErrorKind.NOT_CONFIGURED,
                errorCode = "FCM_NOT_CONFIGURED",
                sanitizedMessage = ex.message ?: "Credenciales de Firebase Admin no configuradas",
            )
        } catch (ex: Exception) {
            val sanitized = TokenSanitizer.sanitizeError(ex.message, token)
            log.error("Error inicializando Firebase Admin SDK: {}", sanitized)
            return FcmSendOutcome.Failed(
                errorKind = FcmErrorKind.NOT_CONFIGURED,
                errorCode = "FCM_INIT_ERROR",
                sanitizedMessage = "No se pudo inicializar Firebase Admin SDK: $sanitized",
            )
        }

        val fcmMessage = Message.builder()
            .setToken(token)
            .putAllData(message.data)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setCollapseKey(message.collapseKey)
                    .build(),
            )
            .build()

        return try {
            val messageId = firebaseMessaging.send(fcmMessage)
            log.info(
                "Mensaje FCM aceptado por Firebase (token={}, collapseKey={}, messageId={})",
                TokenSanitizer.mask(token),
                message.collapseKey,
                messageId,
            )
            FcmSendOutcome.Accepted(messageId = messageId)
        } catch (ex: FirebaseMessagingException) {
            val kind = classifyFirebaseException(ex)
            val code = ex.messagingErrorCode?.name ?: ex.errorCode?.name ?: kind.name
            val sanitized = TokenSanitizer.sanitizeError(ex.message, token)
            log.warn(
                "Fallo de envío FCM (token={}, kind={}, code={}, retryable={}): {}",
                TokenSanitizer.mask(token),
                kind,
                code,
                kind.retryable,
                sanitized,
            )
            FcmSendOutcome.Failed(
                errorKind = kind,
                errorCode = code,
                sanitizedMessage = sanitized,
            )
        } catch (ex: Exception) {
            val isTimeout = hasTimeoutCause(ex)
            val kind = if (isTimeout) FcmErrorKind.NETWORK_TIMEOUT else FcmErrorKind.UNKNOWN
            val code = if (isTimeout) "NETWORK_TIMEOUT" else "FCM_SEND_EXCEPTION"
            val sanitized = TokenSanitizer.sanitizeError(ex.message, token)
            log.warn(
                "Excepción al invocar FCM (token={}, kind={}): {}",
                TokenSanitizer.mask(token),
                kind,
                sanitized,
            )
            FcmSendOutcome.Failed(
                errorKind = kind,
                errorCode = code,
                sanitizedMessage = sanitized,
            )
        }
    }

    private fun resolveFirebaseMessaging(): FirebaseMessaging {
        synchronized(appLock) {
            val projectId = resolveProjectId()
            val appName = "glifo-fcm-${projectId.ifBlank { "default" }}"
            val existing = FirebaseApp.getApps().firstOrNull { it.name == appName }
            if (existing != null) {
                return FirebaseMessaging.getInstance(existing)
            }

            val credentials = loadCredentials()
            val optionsBuilder = FirebaseOptions.builder().setCredentials(credentials)
            if (projectId.isNotBlank()) {
                optionsBuilder.setProjectId(projectId)
            }
            val app = FirebaseApp.initializeApp(optionsBuilder.build(), appName)
            return FirebaseMessaging.getInstance(app)
        }
    }

    private fun resolveEnabled(): Boolean {
        val envOverride = environment?.getProperty("GLIFO_FCM_ENABLED")
            ?: environment?.getProperty("FCM_ENABLED")
        return envOverride?.trim()?.toBooleanStrictOrNull() ?: properties.enabled
    }

    private fun resolveProjectId(): String {
        val candidates = listOfNotNull(
            properties.projectId,
            environment?.getProperty("GLIFO_FCM_PROJECT_ID"),
            environment?.getProperty("FCM_PROJECT_ID"),
            System.getenv("GLIFO_FCM_PROJECT_ID"),
            System.getenv("FCM_PROJECT_ID"),
        )
        return candidates.map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
    }

    private fun resolveExplicitCredentialsPath(): String {
        val candidates = listOfNotNull(
            properties.credentialsPath,
            properties.credentialsFile,
            environment?.getProperty("GLIFO_FCM_CREDENTIALS_PATH"),
            environment?.getProperty("GLIFO_FCM_CREDENTIALS_FILE"),
            environment?.getProperty("FCM_CREDENTIALS_PATH"),
            environment?.getProperty("GOOGLE_APPLICATION_CREDENTIALS"),
            System.getenv("GLIFO_FCM_CREDENTIALS_PATH"),
            System.getenv("FCM_CREDENTIALS_PATH"),
            System.getenv("GOOGLE_APPLICATION_CREDENTIALS"),
        )
        val raw = candidates
            .map { it.trim().removeSurrounding("\"").removeSurrounding("'").trim() }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
        return raw.replace('\\', '/')
    }

    private fun loadCredentials(): GoogleCredentials {
        val explicitPath = resolveExplicitCredentialsPath()

        if (explicitPath.isNotEmpty()) {
            val path = runCatching { Path.of(explicitPath) }.getOrElse {
                throw FcmNotConfiguredException(
                    "La ruta de credenciales de Firebase tiene un formato inválido (use barras '/' en lugar de '\\')",
                )
            }
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw FcmNotConfiguredException(
                    "El archivo de credenciales de Firebase indicado en la configuración no existe o no es legible",
                )
            }
            Files.newInputStream(path).use { input ->
                return GoogleCredentials.fromStream(input)
            }
        }

        if (properties.useApplicationDefault) {
            return try {
                GoogleCredentials.getApplicationDefault()
            } catch (ex: IOException) {
                throw FcmNotConfiguredException(
                    "No se encontraron Application Default Credentials para Firebase Admin",
                )
            }
        }

        throw FcmNotConfiguredException(
            "Credenciales de Firebase Admin no configuradas (defina GLIFO_FCM_CREDENTIALS_PATH o GOOGLE_APPLICATION_CREDENTIALS)",
        )
    }

    internal fun classifyFirebaseException(ex: FirebaseMessagingException): FcmErrorKind {
        if (hasTimeoutCause(ex)) {
            return FcmErrorKind.NETWORK_TIMEOUT
        }
        return when (ex.messagingErrorCode) {
            MessagingErrorCode.UNREGISTERED -> FcmErrorKind.UNREGISTERED
            MessagingErrorCode.INVALID_ARGUMENT -> FcmErrorKind.INVALID_ARGUMENT
            MessagingErrorCode.SENDER_ID_MISMATCH -> FcmErrorKind.SENDER_ID_MISMATCH
            MessagingErrorCode.THIRD_PARTY_AUTH_ERROR -> FcmErrorKind.THIRD_PARTY_AUTH_ERROR
            MessagingErrorCode.UNAVAILABLE -> FcmErrorKind.UNAVAILABLE
            MessagingErrorCode.INTERNAL -> FcmErrorKind.INTERNAL
            MessagingErrorCode.QUOTA_EXCEEDED -> FcmErrorKind.QUOTA_EXCEEDED
            null -> {
                val errName = ex.errorCode?.name.orEmpty()
                when (errName) {
                    "UNAVAILABLE", "INTERNAL", "DEADLINE_EXCEEDED" -> FcmErrorKind.UNAVAILABLE
                    "RESOURCE_EXHAUSTED" -> FcmErrorKind.QUOTA_EXCEEDED
                    "INVALID_ARGUMENT" -> FcmErrorKind.INVALID_ARGUMENT
                    "NOT_FOUND" -> FcmErrorKind.UNREGISTERED
                    "UNAUTHENTICATED", "PERMISSION_DENIED" -> FcmErrorKind.THIRD_PARTY_AUTH_ERROR
                    else -> if (hasTransientIoCause(ex)) FcmErrorKind.UNAVAILABLE else FcmErrorKind.UNKNOWN
                }
            }
        }
    }

    private fun hasTransientIoCause(throwable: Throwable?): Boolean =
        generateSequence(throwable) { it.cause }.any {
            it is IOException ||
                it.message?.contains("Not in GZIP format", ignoreCase = true) == true ||
                it.message?.contains("making a remote service call", ignoreCase = true) == true
        }

    private fun hasTimeoutCause(throwable: Throwable?): Boolean =
        generateSequence(throwable) { it.cause }.any {
            it is SocketTimeoutException ||
                it.message?.contains("timed out", ignoreCase = true) == true ||
                it.message?.contains("timeout", ignoreCase = true) == true
        }

    private class FcmNotConfiguredException(message: String) : RuntimeException(message)
}

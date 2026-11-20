package cr.ac.una.glifo.ops

import cr.ac.una.glifo.ops.config.FcmProperties
import cr.ac.una.glifo.ops.fcm.FcmErrorKind
import cr.ac.una.glifo.ops.fcm.FcmSendOutcome
import cr.ac.una.glifo.ops.fcm.FcmTargetedMessage
import cr.ac.una.glifo.ops.fcm.FirebaseAdminFcmClient
import cr.ac.una.glifo.ops.fcm.TokenSanitizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirebaseAdminFcmClientUnitTest {

    @Test
    fun `una configuracion ausente no simula envio exitoso y devuelve NOT_CONFIGURED`() {
        val client = FirebaseAdminFcmClient(
            FcmProperties(
                enabled = true,
                projectId = "glifoapp",
                credentialsPath = "",
                useApplicationDefault = false,
            ),
        )

        val outcome = client.send(
            FcmTargetedMessage(
                fcmToken = "fcm-token-test-1234567890abcdef",
                collapseKey = "sync_complete_1",
                data = mapOf("kind" to "SYNC_COMPLETE"),
            ),
        )

        assertTrue(outcome is FcmSendOutcome.Failed)
        val failed = outcome as FcmSendOutcome.Failed
        assertEquals(FcmErrorKind.NOT_CONFIGURED, failed.errorKind)
        assertFalse(failed.errorKind.retryable)
        assertFalse(failed.errorKind.invalidatesToken)
    }

    @Test
    fun `una ruta de credenciales inexistente devuelve NOT_CONFIGURED sin exponer tokens`() {
        val sensitiveToken = "secret-fcm-token-value-9999999999"
        val client = FirebaseAdminFcmClient(
            FcmProperties(
                enabled = true,
                projectId = "glifoapp",
                credentialsPath = "ruta/inexistente/firebase-adminsdk.json",
                useApplicationDefault = false,
            ),
        )

        val outcome = client.send(
            FcmTargetedMessage(
                fcmToken = sensitiveToken,
                collapseKey = "sync_complete_2",
                data = mapOf("kind" to "SYNC_COMPLETE"),
            ),
        )

        assertTrue(outcome is FcmSendOutcome.Failed)
        val failed = outcome as FcmSendOutcome.Failed
        assertEquals(FcmErrorKind.NOT_CONFIGURED, failed.errorKind)
        assertFalse(failed.sanitizedMessage.contains(sensitiveToken))
    }

    @Test
    fun `TokenSanitizer enmascara tokens completos en logs y mensajes de error`() {
        val rawToken = "dGhpcy1pcy1hLXZlcnktbG9uZy1mY20tdG9rZW4tMTIzNDU2Nzg5MA=="
        val masked = TokenSanitizer.mask(rawToken)
        assertFalse(masked.contains(rawToken))
        assertTrue(masked.startsWith("dGhp..."))

        val sanitizedError = TokenSanitizer.sanitizeError("Falló envío para token $rawToken en FCM", rawToken)
        assertFalse(sanitizedError.contains(rawToken))
        assertTrue(sanitizedError.contains(masked))
    }
}

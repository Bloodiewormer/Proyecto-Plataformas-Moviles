package cr.ac.una.glifo.core.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.data.datasource.local.dao.SyncOperationDao
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.RegisterDeviceRequest
import cr.ac.una.glifo.data.dto.SyncCompleteRequest
import cr.ac.una.glifo.data.dto.UpdateDeviceTokenRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed interface QueueCompletionReportOutcome {
    data object NoOp : QueueCompletionReportOutcome
    data object Completed : QueueCompletionReportOutcome
    data object RetryLater : QueueCompletionReportOutcome
}

fun interface FcmTokenFetcher {
    suspend fun fetchToken(): String?
}

/**
 * Gestiona la identidad de instalación (`installation_id`), el registro del `device_id`
 * asociado al usuario autenticado, la rotación del token FCM y el aviso de finalización
 * de la cola offline (`POST /api/v1/sync/complete`) (#48).
 */
@Singleton
class DevicePushManager(
    private val api: GlifoApi,
    private val tokenManager: TokenManager,
    private val syncDao: SyncOperationDao,
    private val fcmTokenFetcher: FcmTokenFetcher
) {

    @Inject
    constructor(
        api: GlifoApi,
        tokenManager: TokenManager,
        syncDao: SyncOperationDao
    ) : this(
        api = api,
        tokenManager = tokenManager,
        syncDao = syncDao,
        fcmTokenFetcher = DefaultFirebaseTokenFetcher
    )

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun reconcileDeviceOnSessionStartAsync() {
        managerScope.launch {
            reconcileDeviceOnSessionStart()
        }
    }

    fun unlinkDeviceOnLogoutAsync() {
        managerScope.launch {
            unlinkDeviceOnLogout()
        }
    }

    /**
     * Atiende la llegada o rotación de un token FCM desde `FirebaseMessagingService.onNewToken`.
     * Si todavía no hay sesión iniciada, persiste el token localmente para registrarlo
     * apenas el usuario inicie sesión.
     */
    suspend fun onNewFcmToken(newToken: String) {
        val normalized = newToken.trim()
        if (normalized.isEmpty()) return
        tokenManager.saveFcmToken(normalized)

        val jwt = tokenManager.getToken()
        val userId = tokenManager.getUserId()
        if (jwt.isNullOrBlank() || userId == null) {
            return
        }

        val registeredDeviceId = tokenManager.getRegisteredDeviceId()
        val registeredUserId = tokenManager.getRegisteredDeviceUserId()

        try {
            if (registeredDeviceId != null && registeredUserId == userId) {
                val updated = api.updateDeviceToken(
                    deviceId = registeredDeviceId,
                    request = UpdateDeviceTokenRequest(fcmToken = normalized)
                ).data
                tokenManager.saveRegisteredDevice(updated.id, userId, normalized)
            } else {
                val installationId = tokenManager.getOrCreateInstallationId()
                val registered = api.registerDevice(
                    RegisterDeviceRequest(
                        installationId = installationId,
                        platform = PLATFORM_ANDROID,
                        fcmToken = normalized
                    )
                ).data
                tokenManager.saveRegisteredDevice(registered.id, userId, normalized)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (http: HttpException) {
            if (http.code() == 403 || http.code() == 404) {
                // El device_id guardado ya no pertenece a esta sesión; re-registramos por installation_id
                reconcileDeviceOnSessionStart()
            } else {
                Log.w(TAG, "No se pudo actualizar token FCM en el servidor (HTTP ${http.code()})")
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo actualizar token FCM en el servidor (${e.javaClass.simpleName})")
        }
    }

    /**
     * Registra o reconcilia el dispositivo y su token FCM al iniciar sesión o al arrancar la app
     * con una sesión ya activa.
     */
    suspend fun reconcileDeviceOnSessionStart(): Long? {
        val jwt = tokenManager.getToken()
        val userId = tokenManager.getUserId()
        if (jwt.isNullOrBlank() || userId == null) {
            return null
        }

        val installationId = tokenManager.getOrCreateInstallationId()
        val fcmToken = tokenManager.getFcmToken() ?: run {
            val fetched = try {
                fcmTokenFetcher.fetchToken()?.trim()?.takeIf { it.isNotEmpty() }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            if (fetched != null) {
                tokenManager.saveFcmToken(fetched)
            }
            fetched
        }

        return try {
            val response = api.registerDevice(
                RegisterDeviceRequest(
                    installationId = installationId,
                    platform = PLATFORM_ANDROID,
                    fcmToken = fcmToken
                )
            ).data
            tokenManager.saveRegisteredDevice(response.id, userId, fcmToken)
            response.id
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Log.w(TAG, "Reconciliacion de dispositivo diferida (${e.javaClass.simpleName})")
            tokenManager.getRegisteredDeviceId()?.takeIf { tokenManager.getRegisteredDeviceUserId() == userId }
        }
    }

    /**
     * Garantiza que exista un `device_id` válido del usuario actual antes de informar el cierre
     * de la cola offline.
     */
    suspend fun ensureDeviceRegisteredForSync(): Long? {
        val userId = tokenManager.getUserId() ?: return null
        val existingDeviceId = tokenManager.getRegisteredDeviceId()
        val existingUserId = tokenManager.getRegisteredDeviceUserId()
        val currentToken = tokenManager.getFcmToken()
        val lastSyncedToken = tokenManager.getLastSyncedFcmToken()

        if (existingDeviceId != null && existingUserId == userId && currentToken == lastSyncedToken) {
            return existingDeviceId
        }
        return reconcileDeviceOnSessionStart()
    }

    /**
     * Desvincula el dispositivo en el servidor al cerrar sesión para evitar que la cuenta
     * anterior siga asociada al token FCM de este teléfono, y limpia la sesión local.
     */
    suspend fun unlinkDeviceOnLogout() {
        val jwt = tokenManager.getToken()
        val userId = tokenManager.getUserId()
        val deviceId = tokenManager.getRegisteredDeviceId()
        val deviceUserId = tokenManager.getRegisteredDeviceUserId()

        if (!jwt.isNullOrBlank() && userId != null && deviceId != null && deviceUserId == userId) {
            try {
                api.unlinkDevice(deviceId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo desvincular el dispositivo en logout (${e.javaClass.simpleName})")
            }
        }
        tokenManager.clearAll()
    }

    /**
     * Verifica si la cola local quedó completamente vacía (`pendingCount == 0`) y si existen
     * operaciones `DONE` posteriores a la última marca reportada para el usuario actual.
     *
     * Si corresponde, informa la finalización al backend (`POST /api/v1/sync/complete`) con una
     * clave de idempotencia determinista calculada sobre el tramo de operaciones sincronizadas.
     */
    suspend fun reportQueueCompletionIfNeeded(uploadedInRun: Int): QueueCompletionReportOutcome {
        val jwt = tokenManager.getToken()
        val userId = tokenManager.getUserId()
        if (jwt.isNullOrBlank() || userId == null) {
            return QueueCompletionReportOutcome.NoOp
        }

        if (syncDao.pendingCount() > 0) {
            return QueueCompletionReportOutcome.NoOp
        }

        val maxDoneId = syncDao.maxDoneId() ?: return QueueCompletionReportOutcome.NoOp
        val lastReportedId = tokenManager.getLastReportedSyncOpId(userId)
        if (maxDoneId <= lastReportedId) {
            return QueueCompletionReportOutcome.NoOp
        }

        val doneCount = syncDao.countDoneAfter(lastReportedId).coerceAtLeast(uploadedInRun.coerceAtLeast(1))
        val noteIds = syncDao.syncedServerNoteIdsAfter(lastReportedId)
        val installationId = tokenManager.getOrCreateInstallationId()
        val idempotencyKey = buildDeterministicBatchKey(
            userId = userId,
            installationId = installationId,
            fromOpIdExclusive = lastReportedId,
            toOpIdInclusive = maxDoneId
        )

        val deviceId = ensureDeviceRegisteredForSync() ?: return QueueCompletionReportOutcome.RetryLater

        return try {
            api.completeSync(
                SyncCompleteRequest(
                    deviceId = deviceId,
                    idempotencyKey = idempotencyKey,
                    uploadedCount = doneCount,
                    pendingCount = 0,
                    failedCount = 0,
                    noteIds = noteIds
                )
            )
            tokenManager.setLastReportedSyncOpId(userId, maxDoneId)
            QueueCompletionReportOutcome.Completed
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            QueueCompletionReportOutcome.RetryLater
        } catch (http: HttpException) {
            when (http.code()) {
                409 -> {
                    tokenManager.setLastReportedSyncOpId(userId, maxDoneId)
                    QueueCompletionReportOutcome.Completed
                }
                403, 404 -> {
                    tokenManager.clearRegisteredDevice()
                    QueueCompletionReportOutcome.RetryLater
                }
                401, 408, 429, in 500..599 -> QueueCompletionReportOutcome.RetryLater
                else -> {
                    tokenManager.setLastReportedSyncOpId(userId, maxDoneId)
                    QueueCompletionReportOutcome.Completed
                }
            }
        } catch (_: Exception) {
            QueueCompletionReportOutcome.RetryLater
        }
    }

    companion object {
        private const val TAG = "DevicePushManager"
        const val PLATFORM_ANDROID = "ANDROID"

        fun buildDeterministicBatchKey(
            userId: Long,
            installationId: String,
            fromOpIdExclusive: Long,
            toOpIdInclusive: Long
        ): String {
            val raw = "sync-complete:$userId:$installationId:${fromOpIdExclusive + 1}:$toOpIdInclusive"
            return UUID.nameUUIDFromBytes(raw.toByteArray(Charsets.UTF_8)).toString()
        }
    }
}

private object DefaultFirebaseTokenFetcher : FcmTokenFetcher {
    override suspend fun fetchToken(): String? = suspendCancellableCoroutine { continuation ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resume(null)
                    }
                }
        } catch (_: Exception) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
}

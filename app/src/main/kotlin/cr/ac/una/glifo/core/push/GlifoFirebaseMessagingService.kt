package cr.ac.una.glifo.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio de recepción de Firebase Cloud Messaging (#48).
 *
 * Atiende:
 * - la rotación e inicialización del token FCM ([onNewToken]);
 * - la recepción de mensajes `data-only` dirigidos al `device_id` que completó la subida
 *   de su cola offline ([onMessageReceived]).
 */
@AndroidEntryPoint
class GlifoFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var devicePushManager: DevicePushManager

    @Inject
    lateinit var syncNotificationPresenter: SyncNotificationPresenter

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            devicePushManager.onNewFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val payload = message.data
        if (payload.isEmpty()) return
        serviceScope.launch {
            syncNotificationPresenter.handleIncomingDataMessage(payload)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

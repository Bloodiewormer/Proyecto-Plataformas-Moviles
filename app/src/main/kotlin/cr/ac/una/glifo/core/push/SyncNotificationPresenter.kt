package cr.ac.una.glifo.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cr.ac.una.glifo.MainActivity
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.core.ui.theme.LanguageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ValidatedSyncNotification(
    val eventId: String,
    val deviceId: Long,
    val userId: Long,
    val uploadedCount: Int
)

/**
 * Validador puro del payload `data` de FCM para el evento `SYNC_COMPLETE`.
 * Verifica tipo de evento, preferencia del usuario y pertenencia a la sesión y al dispositivo
 * activos antes de construir una notificación visible.
 */
object SyncNotificationPayloadValidator {
    const val KIND_SYNC_COMPLETE = "SYNC_COMPLETE"

    fun validate(
        data: Map<String, String>,
        notifyEnabled: Boolean,
        currentUserId: Long?,
        registeredDeviceId: Long?
    ): ValidatedSyncNotification? {
        if (!notifyEnabled) return null
        if (currentUserId == null) return null

        val kind = data["kind"]?.trim()
        if (kind != KIND_SYNC_COMPLETE) return null

        val eventId = data["event_id"]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val payloadUserId = data["user_id"]?.trim()?.toLongOrNull() ?: return null
        if (payloadUserId != currentUserId) return null

        val payloadDeviceId = data["device_id"]?.trim()?.toLongOrNull() ?: return null
        if (registeredDeviceId != null && payloadDeviceId != registeredDeviceId) return null

        val uploadedCount = data["uploaded_count"]?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        return ValidatedSyncNotification(
            eventId = eventId,
            deviceId = payloadDeviceId,
            userId = payloadUserId,
            uploadedCount = uploadedCount
        )
    }
}

/**
 * Crea el canal de notificaciones y muestra el aviso de fin de sincronización de la cola offline
 * tanto en foreground como en background (#48).
 *
 * Muestra también la notificación local "Tu apunte está listo" cuando el dispositivo termina
 * de procesar y subir apuntes fuera de primer plano, deduplicando con el aviso de FCM mediante
 * el identificador fijo [NOTIFICATION_ID_SYNC].
 */
@Singleton
class SyncNotificationPresenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val appVisibilityChecker: AppVisibilityChecker? = null
) {

    constructor(
        context: Context,
        tokenManager: TokenManager
    ) : this(context, tokenManager, null)

    fun ensureChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localizedContext = localizedContext()
            val name = localizedContext.getString(R.string.sync_notification_channel_name)
            val descriptionText = localizedContext.getString(R.string.sync_notification_channel_description)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = descriptionText
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun showLocalSyncCompleteNotification(
        uploadedCount: Int,
        noteTitle: String? = null,
        courseName: String? = null
    ): Boolean {
        if (uploadedCount <= 0) return false
        val notifyEnabled = tokenManager.isNotifySyncDoneEnabled()
        if (!notifyEnabled) return false

        if (appVisibilityChecker?.isAppInForeground() == true) {
            return false
        }

        if (!hasNotificationPermission()) {
            return false
        }

        ensureChannelCreated()
        val localized = localizedContext()
        val title = localized.getString(R.string.sync_local_notification_title)
        val body = buildLocalNotificationBody(localized, uploadedCount, noteTitle, courseName)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SYNC,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_glifo_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun handleIncomingDataMessage(data: Map<String, String>): Boolean {
        val notifyEnabled = tokenManager.isNotifySyncDoneEnabled()
        val currentUserId = tokenManager.getUserId()
        val registeredDeviceId = tokenManager.getRegisteredDeviceId()

        val validated = SyncNotificationPayloadValidator.validate(
            data = data,
            notifyEnabled = notifyEnabled,
            currentUserId = currentUserId,
            registeredDeviceId = registeredDeviceId
        ) ?: return false

        val isFirstTime = tokenManager.markNotificationEventSeen(validated.eventId)
        if (!isFirstTime) {
            return false
        }

        if (!hasNotificationPermission()) {
            return false
        }

        ensureChannelCreated()
        val localized = localizedContext()
        val title = localized.getString(R.string.sync_notification_title)
        val body = buildNotificationBody(localized, validated.uploadedCount)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_EVENT_ID, validated.eventId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SYNC,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_glifo_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun localizedContext(): Context {
        return try {
            LanguageManager.wrapContext(context, LanguageManager.language)
        } catch (_: Exception) {
            context
        }
    }

    companion object {
        const val CHANNEL_ID = "glifo_sync_channel"
        const val NOTIFICATION_ID_SYNC = 4801
        const val EXTRA_NOTIFICATION_EVENT_ID = "glifo_sync_event_id"

        fun buildNotificationBody(context: Context, uploadedCount: Int): String {
            return if (uploadedCount == 1) {
                context.getString(R.string.sync_notification_body_single)
            } else {
                context.getString(R.string.sync_notification_body_plural, uploadedCount)
            }
        }

        fun buildLocalNotificationBody(
            context: Context,
            uploadedCount: Int,
            noteTitle: String? = null,
            courseName: String? = null
        ): String {
            return if (uploadedCount == 1) {
                when {
                    !noteTitle.isNullOrBlank() && !courseName.isNullOrBlank() ->
                        context.getString(R.string.sync_local_notification_single_title_and_course, noteTitle, courseName)
                    !noteTitle.isNullOrBlank() ->
                        context.getString(R.string.sync_local_notification_single_title, noteTitle)
                    !courseName.isNullOrBlank() ->
                        context.resources.getQuantityString(R.plurals.sync_local_notification_body_with_course, 1, 1, courseName)
                    else ->
                        context.resources.getQuantityString(R.plurals.sync_local_notification_body, 1, 1)
                }
            } else {
                if (!courseName.isNullOrBlank()) {
                    context.resources.getQuantityString(R.plurals.sync_local_notification_body_with_course, uploadedCount, uploadedCount, courseName)
                } else {
                    context.resources.getQuantityString(R.plurals.sync_local_notification_body, uploadedCount, uploadedCount)
                }
            }
        }
    }
}

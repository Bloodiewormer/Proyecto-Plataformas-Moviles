package cr.ac.una.glifo.core.reminder

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
import cr.ac.una.glifo.core.ui.theme.LanguageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyReminderNotificationPresenter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun ensureChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localizedContext = localizedContext()
            val name = localizedContext.getString(R.string.study_reminder_channel_name)
            val descriptionText = localizedContext.getString(R.string.study_reminder_channel_description)
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

    fun showReminderNotification(
        count: Int,
        courseName: String,
        isQuizOnly: Boolean = false,
        courseId: Long? = null
    ): Boolean {
        if (!hasNotificationPermission()) return false

        ensureChannelCreated()
        val localized = localizedContext()
        val title = localized.getString(R.string.study_reminder_notification_title)
        val body = if (isQuizOnly) {
            localized.resources.getQuantityString(
                R.plurals.study_reminder_notification_body_quiz,
                count,
                count,
                courseName
            )
        } else {
            localized.resources.getQuantityString(
                R.plurals.study_reminder_notification_body_flashcards,
                count,
                count,
                courseName
            )
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_STUDY)
            if (courseId != null) {
                putExtra(MainActivity.EXTRA_COURSE_ID, courseId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
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
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
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
        const val CHANNEL_ID = "glifo_study_reminder_channel"
        const val NOTIFICATION_ID = 4802
    }
}

package cr.ac.una.glifo

import android.app.Application
import android.util.Log
import cr.ac.una.glifo.core.push.DevicePushManager
import cr.ac.una.glifo.core.push.SyncNotificationPresenter
import cr.ac.una.glifo.core.reminder.StudyReminderNotificationPresenter
import cr.ac.una.glifo.core.reminder.StudyReminderScheduler
import cr.ac.una.glifo.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader
import javax.inject.Inject

@HiltAndroidApp
class GlifoApplication : Application() {

    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var devicePushManager: DevicePushManager
    @Inject lateinit var syncNotificationPresenter: SyncNotificationPresenter
    @Inject lateinit var studyReminderPresenter: StudyReminderNotificationPresenter
    @Inject lateinit var studyReminderScheduler: StudyReminderScheduler

    override fun onCreate() {
        super.onCreate()
        // initLocal() carga las .so que vienen dentro del APK (el SDK oficial deprecó initDebug()).
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "Unable to load OpenCV!")
        } else {
            Log.i("OpenCV", "OpenCV loaded successfully!")
        }
        // Canal de notificaciones de sincronización y reconciliación del dispositivo (#48).
        syncNotificationPresenter.ensureChannelCreated()
        studyReminderPresenter.ensureChannelCreated()
        devicePushManager.reconcileDeviceOnSessionStartAsync()

        // Capturas que quedaron en la cola de una sesión anterior (#26): WorkManager espera
        // a que haya red y las sube.
        syncScheduler.schedule()
        studyReminderScheduler.scheduleIfEnabled()
    }
}

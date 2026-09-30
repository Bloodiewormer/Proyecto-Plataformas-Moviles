package cr.ac.una.glifo

import android.app.Application
import android.util.Log
import cr.ac.una.glifo.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader
import javax.inject.Inject

@HiltAndroidApp
class GlifoApplication : Application() {

    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV!")
        } else {
            Log.i("OpenCV", "OpenCV loaded successfully!")
        }
        // Capturas que quedaron en la cola de una sesión anterior (#26): WorkManager espera
        // a que haya red y las sube.
        syncScheduler.schedule()
    }
}

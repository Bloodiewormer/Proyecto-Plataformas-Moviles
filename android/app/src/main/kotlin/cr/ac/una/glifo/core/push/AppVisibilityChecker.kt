package cr.ac.una.glifo.core.push

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AppVisibilityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    open fun isAppInForeground(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            val appProcesses = activityManager.runningAppProcesses ?: return false
            val packageName = context.packageName
            appProcesses.any {
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == packageName
            }
        } catch (_: Exception) {
            false
        }
    }
}

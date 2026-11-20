package cr.ac.una.glifo.core.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cr.ac.una.glifo.core.datastore.AppPreferences
import cr.ac.una.glifo.core.datastore.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class StudyReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val tokenManager: TokenManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    open fun scheduleIfEnabled() {
        scope.launch {
            val enabled = appPreferences.isDailyStudyReminder()
            val token = tokenManager.getToken()
            if (enabled && !token.isNullOrBlank()) {
                enqueueDailyWork()
            } else {
                cancel()
            }
        }
    }

    open fun schedule() {
        scope.launch {
            enqueueDailyWork()
        }
    }

    open fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun enqueueDailyWork() {
        val initialDelayMillis = StudyReminderTimeCalculator.calculateInitialDelayMillis()
        val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        const val WORK_NAME = "glifo-study-reminder"
    }
}

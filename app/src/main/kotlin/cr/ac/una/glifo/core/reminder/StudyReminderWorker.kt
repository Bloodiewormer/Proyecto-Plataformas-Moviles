package cr.ac.una.glifo.core.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.core.datastore.AppPreferences
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.domain.error.Result as DomainResult
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Worker diario de recordatorio de estudio.
 * Si hay sesión activa, el interruptor está habilitado y hay flashcards o quiz pendientes
 * en el curso activo, muestra la notificación para repasar.
 */
class StudyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StudyReminderWorkerEntryPoint {
        fun tokenManager(): TokenManager
        fun appPreferences(): AppPreferences
        fun activeCourseStore(): ActiveCourseStore
        fun studyRepository(): StudyRepository
        fun courseRepository(): CourseRepository
        fun reminderPresenter(): StudyReminderNotificationPresenter
    }

    override suspend fun doWork(): WorkResult {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            StudyReminderWorkerEntryPoint::class.java
        )

        val token = entryPoint.tokenManager().getToken()
        if (token.isNullOrBlank()) {
            return WorkResult.success()
        }

        val isReminderEnabled = entryPoint.appPreferences().isDailyStudyReminder()
        if (!isReminderEnabled) {
            return WorkResult.success()
        }

        val presenter = entryPoint.reminderPresenter()
        if (!presenter.hasNotificationPermission()) {
            return WorkResult.success()
        }

        val activeCourseStore = entryPoint.activeCourseStore()
        var courseId = activeCourseStore.activeCourseId.value
        var courseName = activeCourseStore.activeCourse.value?.name

        if (courseId == null) {
            when (val courses = entryPoint.courseRepository().getStudentCourses()) {
                is DomainResult.Success -> {
                    val first = courses.value.firstOrNull()
                    if (first != null) {
                        courseId = first.id
                        courseName = first.name
                        activeCourseStore.set(first.id, first.name)
                    }
                }
                is DomainResult.Failure -> {}
            }
        }

        if (courseId == null) {
            return WorkResult.success()
        }

        val studyRepository = entryPoint.studyRepository()
        val flashcardCount = (studyRepository.getFlashcards(courseId) as? DomainResult.Success)?.value?.size ?: 0
        val quizCount = if (studyRepository.getQuizQuestion(courseId) is DomainResult.Success) 1 else 0

        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = true,
            isReminderEnabled = isReminderEnabled,
            activeCourseId = courseId,
            courseName = courseName,
            flashcardCount = flashcardCount,
            quizCount = quizCount
        )

        if (decision.shouldNotify) {
            presenter.showReminderNotification(
                count = decision.count,
                courseName = decision.courseName,
                isQuizOnly = decision.isQuizOnly,
                courseId = courseId
            )
        }

        return WorkResult.success()
    }
}

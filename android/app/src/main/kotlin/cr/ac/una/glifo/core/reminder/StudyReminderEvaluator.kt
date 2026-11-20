package cr.ac.una.glifo.core.reminder

data class ReminderDecision(
    val shouldNotify: Boolean,
    val count: Int = 0,
    val isQuizOnly: Boolean = false,
    val courseName: String = ""
)

object StudyReminderEvaluator {

    fun evaluate(
        isLoggedIn: Boolean,
        isReminderEnabled: Boolean,
        activeCourseId: Long?,
        courseName: String?,
        flashcardCount: Int,
        quizCount: Int
    ): ReminderDecision {
        if (!isLoggedIn || !isReminderEnabled || activeCourseId == null) {
            return ReminderDecision(shouldNotify = false)
        }

        val resolvedCourseName = courseName?.trim()?.takeIf { it.isNotEmpty() } ?: "tu curso"

        return when {
            flashcardCount > 0 -> ReminderDecision(
                shouldNotify = true,
                count = flashcardCount,
                isQuizOnly = false,
                courseName = resolvedCourseName
            )
            quizCount > 0 -> ReminderDecision(
                shouldNotify = true,
                count = quizCount,
                isQuizOnly = true,
                courseName = resolvedCourseName
            )
            else -> ReminderDecision(shouldNotify = false)
        }
    }
}

object StudyReminderMessageFormatter {

    fun formatEs(count: Int, courseName: String, isQuizOnly: Boolean = false): String {
        val safeCount = count.coerceAtLeast(1)
        return if (isQuizOnly) {
            if (safeCount == 1) {
                "Tenés 1 quiz para repasar hoy en $courseName"
            } else {
                "Tenés $safeCount quizzes para repasar hoy en $courseName"
            }
        } else {
            if (safeCount == 1) {
                "Tenés 1 flashcard para repasar hoy en $courseName"
            } else {
                "Tenés $safeCount flashcards para repasar hoy en $courseName"
            }
        }
    }

    fun formatEn(count: Int, courseName: String, isQuizOnly: Boolean = false): String {
        val safeCount = count.coerceAtLeast(1)
        return if (isQuizOnly) {
            if (safeCount == 1) {
                "You have 1 quiz to review today in $courseName"
            } else {
                "You have $safeCount quizzes to review today in $courseName"
            }
        } else {
            if (safeCount == 1) {
                "You have 1 flashcard to review today in $courseName"
            } else {
                "You have $safeCount flashcards to review today in $courseName"
            }
        }
    }
}

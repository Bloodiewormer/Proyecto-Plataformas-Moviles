package cr.ac.una.glifo.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class StudyReminderMessageFormatterTest {

    @Test
    fun `formats Spanish single flashcard with voseo and tildes`() {
        val message = StudyReminderMessageFormatter.formatEs(
            count = 1,
            courseName = "Álgebra Lineal",
            isQuizOnly = false
        )
        assertEquals("Tenés 1 flashcard para repasar hoy en Álgebra Lineal", message)
    }

    @Test
    fun `formats Spanish plural flashcards with voseo and tildes`() {
        val message = StudyReminderMessageFormatter.formatEs(
            count = 5,
            courseName = "Álgebra Lineal",
            isQuizOnly = false
        )
        assertEquals("Tenés 5 flashcards para repasar hoy en Álgebra Lineal", message)
    }

    @Test
    fun `formats Spanish single quiz with voseo and tildes`() {
        val message = StudyReminderMessageFormatter.formatEs(
            count = 1,
            courseName = "Química General",
            isQuizOnly = true
        )
        assertEquals("Tenés 1 quiz para repasar hoy en Química General", message)
    }

    @Test
    fun `formats Spanish plural quizzes with voseo and tildes`() {
        val message = StudyReminderMessageFormatter.formatEs(
            count = 3,
            courseName = "Química General",
            isQuizOnly = true
        )
        assertEquals("Tenés 3 quizzes para repasar hoy en Química General", message)
    }

    @Test
    fun `formats English single and plural flashcards`() {
        val single = StudyReminderMessageFormatter.formatEn(1, "Calculus")
        val plural = StudyReminderMessageFormatter.formatEn(4, "Calculus")

        assertEquals("You have 1 flashcard to review today in Calculus", single)
        assertEquals("You have 4 flashcards to review today in Calculus", plural)
    }

    @Test
    fun `formats English single and plural quizzes`() {
        val single = StudyReminderMessageFormatter.formatEn(1, "Calculus", isQuizOnly = true)
        val plural = StudyReminderMessageFormatter.formatEn(2, "Calculus", isQuizOnly = true)

        assertEquals("You have 1 quiz to review today in Calculus", single)
        assertEquals("You have 2 quizzes to review today in Calculus", plural)
    }
}

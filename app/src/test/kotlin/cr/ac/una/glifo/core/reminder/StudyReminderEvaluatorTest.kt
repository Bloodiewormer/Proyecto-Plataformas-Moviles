package cr.ac.una.glifo.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyReminderEvaluatorTest {

    @Test
    fun `does not notify when user is not logged in`() {
        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = false,
            isReminderEnabled = true,
            activeCourseId = 1L,
            courseName = "Cálculo",
            flashcardCount = 5,
            quizCount = 1
        )

        assertFalse(decision.shouldNotify)
    }

    @Test
    fun `does not notify when reminder preference is disabled`() {
        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = true,
            isReminderEnabled = false,
            activeCourseId = 1L,
            courseName = "Cálculo",
            flashcardCount = 5,
            quizCount = 1
        )

        assertFalse(decision.shouldNotify)
    }

    @Test
    fun `does not notify when there is no active course`() {
        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = true,
            isReminderEnabled = true,
            activeCourseId = null,
            courseName = null,
            flashcardCount = 5,
            quizCount = 1
        )

        assertFalse(decision.shouldNotify)
    }

    @Test
    fun `does not notify when there are no flashcards and no quizzes`() {
        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = true,
            isReminderEnabled = true,
            activeCourseId = 1L,
            courseName = "Cálculo",
            flashcardCount = 0,
            quizCount = 0
        )

        assertFalse(decision.shouldNotify)
    }

    @Test
    fun `notifies with flashcard count when flashcards are available`() {
        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = true,
            isReminderEnabled = true,
            activeCourseId = 10L,
            courseName = "Física I",
            flashcardCount = 4,
            quizCount = 0
        )

        assertTrue(decision.shouldNotify)
        assertEquals(4, decision.count)
        assertFalse(decision.isQuizOnly)
        assertEquals("Física I", decision.courseName)
    }

    @Test
    fun `notifies with quiz when only quiz is available`() {
        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = true,
            isReminderEnabled = true,
            activeCourseId = 10L,
            courseName = "Física I",
            flashcardCount = 0,
            quizCount = 1
        )

        assertTrue(decision.shouldNotify)
        assertEquals(1, decision.count)
        assertTrue(decision.isQuizOnly)
        assertEquals("Física I", decision.courseName)
    }

    @Test
    fun `prioritizes flashcards when both flashcards and quiz are available`() {
        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = true,
            isReminderEnabled = true,
            activeCourseId = 10L,
            courseName = "Física I",
            flashcardCount = 8,
            quizCount = 1
        )

        assertTrue(decision.shouldNotify)
        assertEquals(8, decision.count)
        assertFalse(decision.isQuizOnly)
    }

    @Test
    fun `falls back to default course name when course name is blank or null`() {
        val decision = StudyReminderEvaluator.evaluate(
            isLoggedIn = true,
            isReminderEnabled = true,
            activeCourseId = 10L,
            courseName = "   ",
            flashcardCount = 3,
            quizCount = 0
        )

        assertTrue(decision.shouldNotify)
        assertEquals("tu curso", decision.courseName)
    }
}

package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.model.QuizResultRow

object DummyStudyProvider {

    private val flashcards = listOf(
        Flashcard(
            question = "¿Qué condición deben cumplir las series para aplicar el criterio de comparación directa?",
            answer = "Ambas series deben ser de términos positivos y debe cumplirse aₙ ≤ bₙ para todo n a partir de un índice.",
            source = "De tu apunte del 7 ago"
        ),
        Flashcard(
            question = "¿Cuándo converge una serie geométrica ∑ arⁿ?",
            answer = "Cuando |r| < 1. La suma es a/(1−r).",
            source = "De tu apunte del 3 ago"
        ),
        Flashcard(
            question = "Enunciá el criterio de la integral para series de términos positivos.",
            answer = "La serie ∑f(n) converge si y solo si la integral ∫f(x)dx de 1 a ∞ converge.",
            source = "De tu apunte del 5 ago"
        )
    )

    fun getFlashcards(courseId: Long): List<Flashcard> = flashcards

    fun getQuizQuestion(courseId: Long, index: Int = 2, total: Int = 5): QuizQuestion = QuizQuestion(
        topicTag = "TEMA AUSENTE",
        topicLabel = "3.4 Series de potencias",
        prompt = "¿Cuál es el radio de convergencia de la serie ∑ xⁿ / n!?",
        options = listOf("R = 1", "R = ∞", "R = 0", "R = e"),
        correctIndex = 1,
        explanation = "La serie exponencial ∑ xⁿ/n! converge para todo x ∈ ℝ, por lo que su radio de convergencia es infinito."
    )

    fun getQuizResult(courseId: Long): QuizResultInfo = QuizResultInfo(
        score = 7,
        total = 10,
        headline = "Series de potencias sigue floja",
        subtext = "Los 3 fallos son del mismo tema.",
        review = listOf(
            QuizResultRow(true, "1 · Definición de sucesión"),
            QuizResultRow(true, "2 · Serie geométrica"),
            QuizResultRow(false, "3 · Radio de convergencia"),
            QuizResultRow(false, "4 · Serie exponencial")
        ),
        rescheduledCount = 3,
        daysUntilReturn = 6
    )
}

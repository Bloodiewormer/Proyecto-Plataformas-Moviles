package cr.ac.una.glifo.domain.model

data class Flashcard(val question: String, val answer: String, val source: String)

data class QuizQuestion(
    val topicTag: String,
    val topicLabel: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class QuizResultRow(val correct: Boolean, val label: String)

data class QuizResultInfo(
    val score: Int,
    val total: Int,
    val headline: String,
    val subtext: String,
    val review: List<QuizResultRow>,
    val rescheduledCount: Int? = null,
    val daysUntilReturn: Int? = null
)

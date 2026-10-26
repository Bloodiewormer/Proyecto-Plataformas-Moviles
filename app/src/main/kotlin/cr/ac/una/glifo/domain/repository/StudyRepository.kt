package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.model.QuizResultInfo

interface StudyRepository {
    suspend fun generateStudyItems(
        courseId: Long,
        topicIds: List<Long>,
        itemsPerTopic: Int = 3
    ): Result<Unit> = Result.Success(Unit)

    suspend fun getFlashcards(courseId: Long): Result<List<Flashcard>>

    suspend fun getQuizQuestions(courseId: Long): Result<List<QuizQuestion>> =
        when (val single = getQuizQuestion(courseId)) {
            is Result.Success -> Result.Success(listOf(single.value))
            is Result.Failure -> single
        }

    suspend fun getQuizQuestion(courseId: Long): Result<QuizQuestion>

    suspend fun saveQuizResult(courseId: Long, result: QuizResultInfo) {}

    suspend fun getQuizResult(courseId: Long): Result<QuizResultInfo?>
}

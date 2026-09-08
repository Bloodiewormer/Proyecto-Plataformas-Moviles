package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.model.QuizResultInfo

interface StudyRepository {
    suspend fun getFlashcards(courseId: Long): Result<List<Flashcard>>
    suspend fun getQuizQuestion(courseId: Long): Result<QuizQuestion>
    suspend fun getQuizResult(courseId: Long): Result<QuizResultInfo>
}

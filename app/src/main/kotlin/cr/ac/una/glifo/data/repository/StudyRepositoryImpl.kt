package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.dummy.DummyStudyProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.repository.StudyRepository
import javax.inject.Inject

class StudyRepositoryImpl @Inject constructor() : StudyRepository {
    override suspend fun getFlashcards(courseId: Long): Result<List<Flashcard>> =
        Result.Success(DummyStudyProvider.getFlashcards(courseId))

    override suspend fun getQuizQuestion(courseId: Long): Result<QuizQuestion> =
        Result.Success(DummyStudyProvider.getQuizQuestion(courseId))

    override suspend fun getQuizResult(courseId: Long): Result<QuizResultInfo> =
        Result.Success(DummyStudyProvider.getQuizResult(courseId))
}

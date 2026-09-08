package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.dummy.DummyTeacherProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.repository.TeacherRepository
import javax.inject.Inject

class TeacherRepositoryImpl @Inject constructor() : TeacherRepository {
    override suspend fun getGlossaryEntries(): Result<List<GlossaryEntry>> =
        Result.Success(DummyTeacherProvider.getGlossaryEntries())

    override suspend fun addGlossaryEntry(term: String, notation: String, isSymbol: Boolean): Result<Unit> {
        DummyTeacherProvider.addGlossaryEntry(term, notation, isSymbol)
        return Result.Success(Unit)
    }

    override suspend fun getGlossarySuggestions(): Result<List<GlossarySuggestion>> =
        Result.Success(DummyTeacherProvider.getGlossarySuggestions())

    override suspend fun getSyllabusFileName(): Result<String> =
        Result.Success(DummyTeacherProvider.getSyllabusFileName())

    override suspend fun getSyllabusUnits(): Result<List<SyllabusUnitRow>> =
        Result.Success(DummyTeacherProvider.getSyllabusUnits())

    override suspend fun addSyllabusTopic(label: String): Result<Unit> {
        DummyTeacherProvider.addSyllabusTopic(label)
        return Result.Success(Unit)
    }

    override suspend fun getGaps(): Result<List<TeacherGapTopic>> =
        Result.Success(DummyTeacherProvider.getGaps())
}

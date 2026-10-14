package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.model.TeacherGapTopic

interface TeacherRepository {
    suspend fun getGlossaryEntries(): Result<List<GlossaryEntry>>
    suspend fun addGlossaryEntry(term: String, notation: String, isSymbol: Boolean): Result<Unit>
    suspend fun getGlossarySuggestions(): Result<List<GlossarySuggestion>>
    /** [approve] true la agrega al glosario; false la descarta. */
    suspend fun resolveGlossarySuggestion(suggestionId: Long, approve: Boolean): Result<Unit>
    suspend fun getSyllabusFileName(): Result<String>
    suspend fun isSyllabusPublished(): Result<Boolean> = Result.Success(false)
    suspend fun getSyllabusUnits(): Result<List<SyllabusUnitRow>>
    suspend fun addSyllabusTopic(label: String): Result<Unit>
    suspend fun publishSyllabus(topics: List<String>): Result<Unit> = Result.Success(Unit)
    suspend fun getGaps(): Result<List<TeacherGapTopic>>
}


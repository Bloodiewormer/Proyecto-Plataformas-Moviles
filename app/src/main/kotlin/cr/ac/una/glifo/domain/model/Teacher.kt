package cr.ac.una.glifo.domain.model

data class GlossaryEntry(val term: String, val notation: String, val tag: String, val isSymbol: Boolean)
/** [term] es lo que el estudiante corrigió y [meta] la corrección y quién la propuso. */
data class GlossarySuggestion(val id: Long, val term: String, val meta: String)
data class SyllabusTopicRow(val label: String, val hasIssue: Boolean)
data class SyllabusUnitRow(val label: String, val topicCount: Int, val topics: List<SyllabusTopicRow>)

data class SyllabusDocument(
    val courseId: Long,
    val fileName: String?,
    val sizeBytes: Long?,
    val formattedSize: String?,
    val parsedAtDisplay: String?,
    val syllabusVersion: Int,
    val units: List<SyllabusUnitRow>
) {
    val hasSyllabus: Boolean
        get() = units.isNotEmpty() || !fileName.isNullOrBlank()
}
/** [percent] = % de estudiantes con brecha en el tema; [fraction] = "con brecha/inscritos". */
data class TeacherGapTopic(val state: ConfidenceState, val label: String, val percent: Float, val fraction: String)

/**
 * Cobertura del grupo para el docente (GET coverage/gaps). Las partes (0..100) alimentan la barra
 * CovBar: cuánto de todas las celdas estudiante × tema está sólido, parcial, ausente o dudoso.
 */
data class TeacherGroupCoverage(
    val enrolledStudents: Int,
    val coveragePercent: Int,
    val topics: List<TeacherGapTopic>,
    val solidShare: Float,
    val partialShare: Float,
    val absentShare: Float,
    val uncertainShare: Float
)

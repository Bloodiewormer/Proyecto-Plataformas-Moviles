package cr.ac.una.glifo.domain.model

data class GlossaryEntry(val term: String, val notation: String, val tag: String, val isSymbol: Boolean)
/** [term] es lo que el estudiante corrigió y [meta] la corrección y quién la propuso. */
data class GlossarySuggestion(val id: Long, val term: String, val meta: String)
data class SyllabusTopicRow(val label: String, val hasIssue: Boolean)
data class SyllabusUnitRow(val label: String, val topicCount: Int, val topics: List<SyllabusTopicRow>)
data class TeacherGapTopic(val state: ConfidenceState, val label: String, val percent: Float, val fraction: String)

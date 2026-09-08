package cr.ac.una.glifo.domain.model

data class GlossaryEntry(val term: String, val notation: String, val tag: String, val isSymbol: Boolean)
data class GlossarySuggestion(val term: String, val meta: String)
data class SyllabusTopicRow(val label: String, val hasIssue: Boolean)
data class SyllabusUnitRow(val label: String, val topicCount: Int, val topics: List<SyllabusTopicRow>)
data class TeacherGapTopic(val state: ConfidenceState, val label: String, val percent: Float, val fraction: String)

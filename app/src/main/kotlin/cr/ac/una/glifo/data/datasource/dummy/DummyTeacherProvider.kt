package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.model.SyllabusTopicRow
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.model.ConfidenceState

object DummyTeacherProvider {

    private val glossaryEntries = mutableListOf(
        GlossaryEntry("Sumatoria", "\\sum_{n=1}^{\\infty}", "SÍMBOLO", isSymbol = true),
        GlossaryEntry("Converge", "conv. · cnv · converg.", "TÉRMINO", isSymbol = false),
        GlossaryEntry("Radio de convergencia", "R", "TÉRMINO", isSymbol = false),
        GlossaryEntry("Integral impropia", "\\int_a^{\\infty}", "SÍMBOLO", isSymbol = true),
        GlossaryEntry("Límite n→∞", "\\lim_{n \\to \\infty}", "SÍMBOLO", isSymbol = true)
    )

    fun getGlossaryEntries(): List<GlossaryEntry> = glossaryEntries

    fun addGlossaryEntry(term: String, notation: String, isSymbol: Boolean) {
        glossaryEntries.add(0, GlossaryEntry(term.trim(), notation.trim(), if (isSymbol) "SÍMBOLO" else "TÉRMINO", isSymbol))
    }

    fun getGlossarySuggestions(): List<GlossarySuggestion> = listOf(
        GlossarySuggestion("diverge", "corregido 7 veces esta semana")
    )

    fun getSyllabusFileName(): String = "programa-EIF411.pdf"

    private val syllabusUnits = mutableListOf(
        SyllabusUnitRow(
            label = "3 · Series",
            topicCount = 5,
            topics = mutableListOf(
                SyllabusTopicRow("3.1 Sucesiones", hasIssue = false),
                SyllabusTopicRow("3.2 Series geométricas", hasIssue = false),
                SyllabusTopicRow("3.3 Criterio de comparación", hasIssue = true)
            )
        ),
        SyllabusUnitRow(label = "4 · Cálculo vectorial", topicCount = 7, topics = emptyList())
    )

    fun getSyllabusUnits(): List<SyllabusUnitRow> = syllabusUnits

    /** Appends a manually-entered topic to the first unit (the one already expanded in the UI). */
    fun addSyllabusTopic(label: String) {
        val first = syllabusUnits.firstOrNull() ?: return
        val updatedTopics = first.topics + SyllabusTopicRow(label.trim(), hasIssue = false)
        syllabusUnits[0] = first.copy(topicCount = first.topicCount + 1, topics = updatedTopics)
    }

    fun getGaps(): List<TeacherGapTopic> = listOf(
        TeacherGapTopic(ConfidenceState.Escalated, "4.3 Sockets e hilos", 70f, "19/27"),
        TeacherGapTopic(ConfidenceState.Uncertain, "6.1 Seguridad y permisos", 52f, "14/27"),
        TeacherGapTopic(ConfidenceState.Repaired, "3.2 Jetpack Compose", 30f, "8/27")
    )
}

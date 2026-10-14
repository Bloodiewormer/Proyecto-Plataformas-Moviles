package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.GlossaryEntryResponse
import cr.ac.una.glifo.data.dto.GlossarySuggestionResponse
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion

/** SYMBOL y NOTATION se muestran como símbolo; TERM como término (CHECK de course_glossary). */
fun GlossaryEntryResponse.toDomain(): GlossaryEntry {
    val isSymbol = kind != "TERM"
    val tag = when (kind) {
        "SYMBOL" -> "SÍMBOLO"
        "NOTATION" -> "NOTACIÓN"
        else -> "TÉRMINO"
    }
    return GlossaryEntry(term = term, notation = canonicalForm, tag = tag, isSymbol = isSymbol)
}

fun GlossarySuggestionResponse.toDomain(): GlossarySuggestion = GlossarySuggestion(
    id = id,
    term = originalText,
    meta = "→ $suggestedCorrection · $suggestedBy"
)

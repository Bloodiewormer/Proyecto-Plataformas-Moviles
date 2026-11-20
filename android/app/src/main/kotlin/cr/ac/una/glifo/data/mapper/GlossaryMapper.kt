package cr.ac.una.glifo.data.mapper

import android.content.Context
import cr.ac.una.glifo.R
import cr.ac.una.glifo.data.dto.GlossaryEntryResponse
import cr.ac.una.glifo.data.dto.GlossarySuggestionResponse
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion

/** SYMBOL y NOTATION se muestran como símbolo; TERM como término (CHECK de course_glossary). */
fun GlossaryEntryResponse.toDomain(context: Context? = null): GlossaryEntry {
    val isSymbol = kind != "TERM"
    val tag = when (kind) {
        "SYMBOL" -> context?.getString(R.string.glossary_tag_symbol) ?: "SÍMBOLO"
        "NOTATION" -> context?.getString(R.string.glossary_tag_notation) ?: "NOTACIÓN"
        else -> context?.getString(R.string.glossary_tag_term) ?: "TÉRMINO"
    }
    return GlossaryEntry(term = term, notation = canonicalForm, tag = tag, isSymbol = isSymbol)
}

fun GlossarySuggestionResponse.toDomain(): GlossarySuggestion = GlossarySuggestion(
    id = id,
    term = originalText,
    meta = "→ $suggestedCorrection · $suggestedBy"
)

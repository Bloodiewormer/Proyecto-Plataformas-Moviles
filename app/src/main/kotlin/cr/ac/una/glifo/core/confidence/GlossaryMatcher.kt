package cr.ac.una.glifo.core.confidence

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/**
 * Sugerencias de corrección a partir del glosario canónico del curso (D-15, #20): para cada
 * palabra del fragmento dudoso se busca el término del glosario que más se le parece. Es
 * local y determinista, sin IA: distancia de edición sobre texto sin tildes ni mayúsculas.
 */
object GlossaryMatcher {

    /** Similitud mínima (0..1) para ofrecer un término. */
    const val MIN_SIMILARITY = 0.6f

    /**
     * Los términos del glosario más parecidos a alguna palabra de [text], de mejor a peor.
     * Un término que ya aparece tal cual en el texto no se sugiere: no corrige nada.
     */
    fun suggestions(text: String, glossaryTerms: List<String>, limit: Int = 3): List<String> {
        val words = text.split(Regex("\\s+")).map { normalize(it) }.filter { it.length >= 3 }
        if (words.isEmpty()) return emptyList()

        return glossaryTerms
            .asSequence()
            .filter { it.isNotBlank() }
            .map { term ->
                val normalizedTerm = normalize(term)
                val candidates = words + words.windowed(2) { it.joinToString(" ") }
                term to candidates.maxOf { similarity(it, normalizedTerm) }
            }
            .filter { (term, score) -> score >= MIN_SIMILARITY && !normalize(text).contains(normalize(term)) }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinct()
            .take(limit)
            .toList()
    }

    /** 1 = iguales, 0 = nada en común (Levenshtein normalizado por el más largo). */
    fun similarity(a: String, b: String): Float {
        if (a.isEmpty() && b.isEmpty()) return 1f
        val longest = max(a.length, b.length)
        return 1f - levenshtein(a, b).toFloat() / longest
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = min(min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^\\p{L}\\p{Nd} ]"), "")
            .trim()
}

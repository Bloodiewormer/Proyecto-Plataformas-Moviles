package cr.ac.una.glifo.coverage.service

import org.springframework.stereotype.Component
import java.text.Normalizer
import java.util.Locale

/** Un tema hoja del temario y sus ancestros (unidades) tal como los compara el pre-filtro. */
data class PrefilterTopic(val topicId: Long, val code: String, val title: String, val ancestorIds: Set<Long> = emptySet())

/** Texto reconstruido de un apunte (sin título ni fragmentos inciertos) y el tema al que está asignado. */
data class PrefilterNote(val linkedTopicId: Long?, val text: String)

/**
 * Resultado del pre-filtro para un tema.
 * - [state]: SOLID o ABSENT si la comparación léxica es concluyente; null si es ambigua y va a IA-04.
 * - [score]: proporción de palabras clave del título que aparecen en los apuntes (0..1).
 * - [excerpts]: fragmentos de apuntes que mencionan alguna palabra clave, para IA-04.
 */
data class PrefilterDecision(
    val topic: PrefilterTopic,
    val state: String?,
    val score: Float,
    val linkedNotes: Int,
    val excerpts: List<String>
)

private val STOPWORDS = setOf(
    "para", "como", "entre", "sobre", "desde", "hasta", "segun", "unos", "unas", "este", "esta",
    "estos", "estas", "cada", "todo", "toda", "todos", "todas", "otro", "otra", "tema", "unidad",
    "introduccion"
)
private val SHORT_STOPWORDS = setOf(
    "al", "con", "de", "del", "dos", "el", "en", "es", "fue", "ha", "la", "las", "le", "lo", "los", "mas", "me", "mi",
    "ni", "no", "por", "que", "se", "sea", "si", "sin", "son", "su", "sus", "te", "tu", "un", "una", "uno", "ya"
)
private val MARKS = Regex("\\p{M}+")
private val NON_ALNUM = Regex("[^a-z0-9]+")
private val SENTENCE_BREAK = Regex("\\n|(?<![0-9])[.;](?=\\s|$)")

/**
 * Pre-filtro determinista de cobertura (D-08), sin IA: resuelve los temas claros y deja como
 * ambiguos solo los que necesitan juicio semántico.
 * - Palabras clave del título: minúsculas, sin tildes ni palabras vacías (también las cortas), con
 *   raíz española simple (plural, z/c y vocal final) y recortadas a 6 caracteres. Las siglas de 2 o
 *   3 letras (SQL, JWT) cuentan como palabra clave.
 * - Sin apuntes con contenido: todo ABSENT.
 * - SOLID, solo con evidencia directa: al menos [MIN_SOLID_KEYWORDS] palabras clave y al menos
 *   [SOLID_RATIO] de ellas en los apuntes asignados al propio tema (no en otros apuntes).
 * - ABSENT: ninguna palabra clave aparece y ningún apunte está asignado al propio tema. Un apunte
 *   asignado solo a la unidad no lo impide: el subtema puede no haberse visto.
 * - Ambiguo: el resto, o títulos sin palabras clave. Los fragmentos para IA-04 priorizan las
 *   oraciones con palabras clave, luego las de apuntes del tema y por último las de su unidad.
 */
@Component
class CoveragePrefilter {

    fun decide(topics: List<PrefilterTopic>, notes: List<PrefilterNote>): List<PrefilterDecision> {
        if (notes.isEmpty()) return topics.map { PrefilterDecision(it, "ABSENT", 0f, 0, emptyList()) }
        val allStems = notes.flatMap { stems(it.text, short = true) }.toSet()
        return topics.map { topic ->
            val keywords = keywords(topic.title)
            val hits = keywords.count { it in allStems }
            val ratio = if (keywords.isEmpty()) 0f else hits.toFloat() / keywords.size
            val direct = notes.filter { it.linkedTopicId == topic.topicId }
            val linked = direct.size
            val directStems = direct.flatMap { stems(it.text, short = true) }.toSet()
            val directRatio = if (keywords.isEmpty()) 0f else keywords.count { it in directStems }.toFloat() / keywords.size
            val state = when {
                keywords.isEmpty() -> null
                directRatio >= SOLID_RATIO && keywords.size >= MIN_SOLID_KEYWORDS -> "SOLID"
                hits == 0 && linked == 0 -> "ABSENT"
                else -> null
            }
            PrefilterDecision(
                topic = topic,
                state = state,
                score = if (state == "ABSENT") 0f else ratio,
                linkedNotes = linked,
                excerpts = if (state == null) excerpts(notes, topic, keywords) else emptyList()
            )
        }
    }

    private fun excerpts(notes: List<PrefilterNote>, topic: PrefilterTopic, keywords: Set<String>): List<String> {
        val sentences = notes.flatMap { note ->
            note.text.split(SENTENCE_BREAK).map { it.trim() }.filter { it.isNotEmpty() }.map { note to it }
        }
        fun rank(note: PrefilterNote, sentence: String) = when {
            keywords(sentence).any { it in keywords } -> 0
            note.linkedTopicId == topic.topicId -> 1
            note.linkedTopicId != null && note.linkedTopicId in topic.ancestorIds -> 2
            else -> null
        }
        return sentences.mapNotNull { (note, sentence) -> rank(note, sentence)?.let { it to sentence } }
            .sortedBy { it.first }
            .map { it.second }
            .distinct()
            .take(MAX_EXCERPTS)
            .map { it.take(MAX_EXCERPT_CHARS) }
    }

    companion object {
        const val SOLID_RATIO = 0.8f
        const val MIN_SOLID_KEYWORDS = 2
        const val MAX_EXCERPTS = 4
        const val MAX_EXCERPT_CHARS = 300

        fun keywords(title: String): Set<String> = stems(title, short = true)

        fun stems(text: String, short: Boolean = false): Set<String> =
            Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace(MARKS, "")
                .split(NON_ALNUM)
                .filter { word ->
                    if (word.length >= 4) word !in STOPWORDS else short && word.length >= 2 && word !in SHORT_STOPWORDS
                }
                .map { stem(it) }
                .toSet()

        /**
         * Raíz simple: plural -es tras consonante o -s tras vocal, z como c (matriz/matrices), una
         * vocal final y recorte a 6. Es léxica: puede unir palabras distintas con la misma raíz.
         */
        private fun stem(word: String): String {
            var w = word
            if (w.length >= 5 && w.endsWith("es") && w[w.length - 3] !in VOWELS) {
                w = w.dropLast(2)
            } else if (w.length >= 4 && w.endsWith("s") && w[w.length - 2] in VOWELS) {
                w = w.dropLast(1)
            }
            w = w.replace('z', 'c')
            if (w.length >= 4 && w.last() in VOWELS) w = w.dropLast(1)
            return w.take(6)
        }

        private const val VOWELS = "aeiou"
    }
}

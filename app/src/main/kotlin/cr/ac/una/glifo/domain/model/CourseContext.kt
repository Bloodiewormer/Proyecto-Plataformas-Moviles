package cr.ac.una.glifo.domain.model

/**
 * Lo mínimo del curso que el pipeline necesita para correr sin conexión (D-17, issue #27):
 * el glosario canónico del docente, que alimenta al ConfidenceScorer, y las versiones que
 * permiten saber si la copia local quedó vieja al reconectar.
 */
data class LocalCourseContext(
    val courseId: Long,
    val courseName: String,
    val syllabusVersion: Int,
    val glossaryVersion: Int,
    val entries: List<GlossaryTerm>,
    /** Momento (epoch ms) en que se guardó o se confirmó vigente contra el servidor. */
    val cachedAt: Long,
    val source: ContextSource
) {
    /** Términos que el scorer busca dentro del texto reconocido por N1. */
    val glossaryTerms: List<String> get() = entries.map { it.term }.filter { it.isNotBlank() }
}

data class GlossaryTerm(
    val term: String,
    val canonicalForm: String,
    /** TERM · SYMBOL · NOTATION (Glifo_UML_Modeling.md §5.7). */
    val kind: String
)

enum class ContextSource {
    /** Recién descargado del servidor. */
    Network,
    /** Copia local de Room; el servidor no respondió o no hacía falta preguntarle. */
    Cache
}

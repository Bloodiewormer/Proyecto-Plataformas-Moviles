package cr.ac.una.glifo.core.data.dummy

import cr.ac.una.glifo.core.network.dto.NoteResponse
import cr.ac.una.glifo.core.ui.component.ConfidenceState
import cr.ac.una.glifo.core.ui.component.GlifoRole

// ───────────────────────── Note content (keyed by noteId) ─────────────────────────
// Backs NoteDetail / CorrectFragment / FormulaViewer / RegionProvenance — the "reading
// a captured note" cluster. All four screens key off the same noteId nav argument.

data class NoteFormulaInfo(
    val regionLabel: String,
    val display: String,
    val latex: String,
    val confidence: Float,
    val tier: String,
    val engine: String
)

data class NoteCorrectionInfo(
    val regionIndex: Int,
    val regionTotal: Int,
    val confidence: Float,
    val threshold: Float,
    val recognizedPrefix: String,
    val recognizedWord: String,
    val suggestions: List<String>
)

data class NoteProvenanceFactor(val label: String, val value: Float, val isStrong: Boolean)
data class NoteProvenanceStep(val state: String, val label: String, val duration: String)

data class NoteProvenanceInfo(
    val confidence: Float,
    val threshold: Float,
    val factors: List<NoteProvenanceFactor>,
    val pipeline: List<NoteProvenanceStep>
)

data class NoteContent(
    val note: NoteResponse,
    val pageLabel: String,
    val heading: String,
    val bodyBeforeHighlight: String,
    val highlighted: String,
    val bodyAfterHighlight: String,
    val verifiedCount: Int,
    val repairedCount: Int,
    val escalatedCount: Int,
    val uncertainCount: Int,
    val formula: NoteFormulaInfo,
    val secondParagraphBefore: String,
    val secondParagraphHighlighted: String,
    val uncertainFragmentLabel: String,
    val correction: NoteCorrectionInfo,
    val provenance: NoteProvenanceInfo
)

object DummyNoteContentProvider {

    private val defaultCorrection = NoteCorrectionInfo(
        regionIndex = 12,
        regionTotal = 18,
        confidence = 0.41f,
        threshold = 0.70f,
        recognizedPrefix = "converge; en caso contrario ",
        recognizedWord = "dvierge",
        suggestions = listOf("diverge", "divergente", "deriva")
    )

    private val defaultProvenance = NoteProvenanceInfo(
        confidence = 0.41f,
        threshold = 0.70f,
        factors = listOf(
            NoteProvenanceFactor("Confianza del OCR", 0.38f, isStrong = false),
            NoteProvenanceFactor("Coincide con el glosario", 0f, isStrong = false),
            NoteProvenanceFactor("Palabra del diccionario", 0f, isStrong = false),
            NoteProvenanceFactor("Coherencia con región vecina", 0.72f, isStrong = true)
        ),
        pipeline = listOf(
            NoteProvenanceStep("done", "N0 · preprocesada", "0.02 s"),
            NoteProvenanceStep("done", "N1 · ML Kit", "0.06 s"),
            NoteProvenanceStep("skipped", "N1.5 · no es fórmula", "omitido"),
            NoteProvenanceStep("escalated", "N2 · visión, en lote", "1.4 s")
        )
    )

    private val contents = listOf(
        NoteContent(
            note = DummyNoteProvider.findNote(1L),
            pageLabel = "Clase del 12 de agosto · página 1 de 3",
            heading = "Integrales impropias de primera especie",
            bodyBeforeHighlight = "Una integral es impropia cuando el intervalo de integración no está acotado, o cuando la función presenta una ",
            highlighted = "discontinuidad infinita",
            bodyAfterHighlight = " dentro del intervalo.",
            verifiedCount = 14,
            repairedCount = 2,
            escalatedCount = 1,
            uncertainCount = 1,
            formula = NoteFormulaInfo(
                regionLabel = "región 7",
                display = "∫_a^∞ f(x) dx = lim_{b→∞} ∫_a^b f(x) dx",
                latex = "\\int_a^{\\infty} f(x)\\,dx = \\lim_{b \\to \\infty} \\int_a^b f(x)\\,dx",
                confidence = 0.86f,
                tier = "N1.5",
                engine = "SimpleTex · gratuito"
            ),
            secondParagraphBefore = "Si el límite existe y es finito, la integral ",
            secondParagraphHighlighted = "converge; en caso contrario diverge",
            uncertainFragmentLabel = "1 fragmento sin leer con certeza",
            correction = defaultCorrection,
            provenance = defaultProvenance
        ),
        NoteContent(
            note = DummyNoteProvider.findNote(2L),
            pageLabel = "Clase del 7 de agosto · página 2 de 2",
            heading = "Criterio de comparación para series de términos positivos",
            bodyBeforeHighlight = "Si 0 ≤ aₙ ≤ bₙ para todo n a partir de cierto índice y ∑bₙ converge, entonces ",
            highlighted = "∑aₙ también converge",
            bodyAfterHighlight = ". Si ∑aₙ diverge, entonces ∑bₙ también diverge.",
            verifiedCount = 9,
            repairedCount = 1,
            escalatedCount = 0,
            uncertainCount = 2,
            formula = NoteFormulaInfo(
                regionLabel = "región 3",
                display = "0 ≤ aₙ ≤ bₙ ⟹ ∑aₙ ≤ ∑bₙ",
                latex = "0 \\le a_n \\le b_n \\implies \\sum a_n \\le \\sum b_n",
                confidence = 0.91f,
                tier = "N1",
                engine = "ML Kit · en el dispositivo"
            ),
            secondParagraphBefore = "El criterio no dice nada cuando ",
            secondParagraphHighlighted = "la desigualdad no se cumple desde el inicio",
            uncertainFragmentLabel = "2 fragmentos sin leer con certeza",
            correction = defaultCorrection.copy(
                regionIndex = 5,
                regionTotal = 9,
                recognizedPrefix = "para todo n a partir de cierto ",
                recognizedWord = "indce",
                suggestions = listOf("índice", "indicio", "índices")
            ),
            provenance = defaultProvenance.copy(confidence = 0.52f)
        ),
        NoteContent(
            note = DummyNoteProvider.findNote(3L),
            pageLabel = "Clase del 2 de agosto · página 1 de 4",
            heading = "Series de potencias y radio de convergencia",
            bodyBeforeHighlight = "Una serie de potencias centrada en a converge en un intervalo determinado por su ",
            highlighted = "radio de convergencia R",
            bodyAfterHighlight = ", que puede calcularse con el criterio del cociente.",
            verifiedCount = 11,
            repairedCount = 3,
            escalatedCount = 2,
            uncertainCount = 0,
            formula = NoteFormulaInfo(
                regionLabel = "región 9",
                display = "R = 1 / lim sup |aₙ|^(1/n)",
                latex = "R = \\dfrac{1}{\\limsup_{n\\to\\infty} |a_n|^{1/n}}",
                confidence = 0.79f,
                tier = "N1.5",
                engine = "SimpleTex · gratuito"
            ),
            secondParagraphBefore = "En los extremos del intervalo, ",
            secondParagraphHighlighted = "la convergencia debe revisarse aparte",
            uncertainFragmentLabel = "Sin fragmentos pendientes",
            correction = defaultCorrection.copy(
                regionIndex = 9,
                regionTotal = 14,
                confidence = 0.79f,
                recognizedPrefix = "el radio de ",
                recognizedWord = "convergncia",
                suggestions = listOf("convergencia", "convergente", "convergen")
            ),
            provenance = defaultProvenance.copy(confidence = 0.79f)
        ),
        NoteContent(
            note = DummyNoteProvider.findNote(4L),
            pageLabel = "Clase del 28 de julio · página 1 de 2",
            heading = "Criterio de la integral",
            bodyBeforeHighlight = "Si f es una función positiva, continua y decreciente en [1, ∞), entonces ∑f(n) converge si y solo si ",
            highlighted = "la integral impropia converge",
            bodyAfterHighlight = ".",
            verifiedCount = 13,
            repairedCount = 0,
            escalatedCount = 0,
            uncertainCount = 0,
            formula = NoteFormulaInfo(
                regionLabel = "región 2",
                display = "∑ f(n) converge ⟺ ∫₁^∞ f(x) dx converge",
                latex = "\\sum f(n) \\text{ converge} \\iff \\int_1^{\\infty} f(x)\\,dx \\text{ converge}",
                confidence = 0.95f,
                tier = "N1",
                engine = "ML Kit · en el dispositivo"
            ),
            secondParagraphBefore = "Este criterio es útil cuando ",
            secondParagraphHighlighted = "la serie no tiene una forma cerrada evidente",
            uncertainFragmentLabel = "Sin fragmentos pendientes",
            correction = defaultCorrection.copy(regionIndex = 2, regionTotal = 6, confidence = 0.95f),
            provenance = defaultProvenance.copy(confidence = 0.95f)
        )
    )

    fun get(noteId: Long): NoteContent = contents.find { it.note.id == noteId } ?: contents.first()
}

// ───────────────────────────────── Coverage ─────────────────────────────────

data class CoverageTopic(val state: ConfidenceState, val name: String, val note: String?, val score: String)

data class CoverageSummary(
    val topicCount: Int,
    val coveragePercent: Int,
    val v: Float, val r: Float, val e: Float, val u: Float,
    val solidCount: Int,
    val partialCount: Int,
    val absentCount: Int,
    val doubtfulCount: Int,
    val unitLabel: String,
    val deltaLabel: String
)

data class TopicChecklistItem(val ok: Boolean, val label: String, val meta: String)
data class TopicNoteRef(val noteId: Long, val title: String, val meta: String)
data class TopicDetail(
    val name: String,
    val state: ConfidenceState,
    val stateLabel: String,
    val confidence: String,
    val checklist: List<TopicChecklistItem>,
    val noteRefs: List<TopicNoteRef>
)

data class CovBarValues(val v: Float, val r: Float, val e: Float, val u: Float)
data class CoverageImprovement(val label: String, val from: ConfidenceState, val to: ConfidenceState)
data class CoverageMissing(val label: String, val state: ConfidenceState, val meta: String, val needsReview: Boolean)
data class CoverageDeltaInfo(
    val dateRange: String,
    val fromPercent: Int,
    val toPercent: Int,
    val fromBar: CovBarValues,
    val toBar: CovBarValues,
    val deltaPoints: Int,
    val newNotesCount: Int,
    val newPagesCount: Int,
    val improved: List<CoverageImprovement>,
    val stillMissing: List<CoverageMissing>
)

object DummyCoverageProvider {

    fun getSummary(courseId: Long): CoverageSummary {
        val stats = DummyCourseProvider.getStats(courseId)
        return CoverageSummary(
            topicCount = if (stats.topicCount > 0) stats.topicCount else 33,
            coveragePercent = stats.coveragePercent,
            v = stats.verified, r = stats.repaired, e = stats.escalated, u = stats.uncertain,
            solidCount = 18, partialCount = 7, absentCount = 5, doubtfulCount = 3,
            unitLabel = "UNIDAD 3 · SERIES",
            deltaLabel = "+2 temas desde el 8 de agosto"
        )
    }

    private val topics = listOf(
        CoverageTopic(ConfidenceState.Verified, "3.1 Sucesiones", null, "0.92"),
        CoverageTopic(ConfidenceState.Verified, "3.2 Series geométricas", null, "0.88"),
        CoverageTopic(ConfidenceState.Repaired, "3.3 Comparación", "Falta el caso del límite", "0.54"),
        CoverageTopic(ConfidenceState.Escalated, "3.4 Series de potencias", "Sin evidencia en tus apuntes", "0.11"),
        CoverageTopic(ConfidenceState.Uncertain, "3.5 Radio de convergencia", "Apunte ilegible en esa sección", "—")
    )

    fun getTopics(courseId: Long): List<CoverageTopic> = topics

    private val topicDetails = listOf(
        TopicDetail(
            name = "3.3 Comparación",
            state = ConfidenceState.Repaired,
            stateLabel = "PARCIAL",
            confidence = "0.54",
            checklist = listOf(
                TopicChecklistItem(true, "Enunciado del criterio", "2 apuntes"),
                TopicChecklistItem(true, "Ejemplo resuelto", "1 apunte"),
                TopicChecklistItem(false, "Criterio de comparación en el límite", "sin evidencia"),
                TopicChecklistItem(false, "Condición de convergencia", "sin evidencia")
            ),
            noteRefs = listOf(
                TopicNoteRef(2L, "Criterio de comparación", "7 ago · página 2"),
                TopicNoteRef(3L, "Series de potencias", "5 ago · página 1")
            )
        ),
        TopicDetail(
            name = "3.4 Series de potencias",
            state = ConfidenceState.Escalated,
            stateLabel = "AUSENTE",
            confidence = "0.11",
            checklist = listOf(
                TopicChecklistItem(false, "Enunciado general", "sin evidencia"),
                TopicChecklistItem(false, "Radio de convergencia", "sin evidencia"),
                TopicChecklistItem(false, "Ejemplo resuelto", "sin evidencia")
            ),
            noteRefs = emptyList()
        ),
        TopicDetail(
            name = "3.5 Radio de convergencia",
            state = ConfidenceState.Uncertain,
            stateLabel = "DUDOSO",
            confidence = "—",
            checklist = listOf(
                TopicChecklistItem(true, "Hay apunte del tema", "1 apunte"),
                TopicChecklistItem(false, "Apunte legible", "ilegible")
            ),
            noteRefs = listOf(TopicNoteRef(3L, "Series de potencias", "2 ago · página 3"))
        )
    )

    fun getTopicDetail(name: String): TopicDetail = topicDetails.find { it.name == name } ?: topicDetails.first()

    fun getDelta(): CoverageDeltaInfo = CoverageDeltaInfo(
        dateRange = "Del 8 al 15 de agosto",
        fromPercent = 62,
        toPercent = 68,
        fromBar = CovBarValues(48f, 18f, 20f, 14f),
        toBar = CovBarValues(54f, 20f, 14f, 12f),
        deltaPoints = 6,
        newNotesCount = 4,
        newPagesCount = 11,
        improved = listOf(
            CoverageImprovement("3.2 Series geométricas", ConfidenceState.Escalated, ConfidenceState.Verified),
            CoverageImprovement("3.3 Comparación", ConfidenceState.Escalated, ConfidenceState.Repaired),
            CoverageImprovement("2.4 Integrales impropias", ConfidenceState.Repaired, ConfidenceState.Verified)
        ),
        stillMissing = listOf(
            CoverageMissing("3.4 Series de potencias", ConfidenceState.Escalated, "Sin cambios desde el 29 de julio", needsReview = false),
            CoverageMissing("3.5 Radio de convergencia", ConfidenceState.Uncertain, "Hay apunte, pero ilegible", needsReview = true)
        )
    )
}

// ───────────────────────────────── Study ─────────────────────────────────

data class Flashcard(val question: String, val answer: String, val source: String)
data class QuizQuestion(
    val topicTag: String,
    val topicLabel: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)
data class QuizResultRow(val correct: Boolean, val label: String)
data class QuizResultInfo(
    val score: Int,
    val total: Int,
    val headline: String,
    val subtext: String,
    val review: List<QuizResultRow>,
    val rescheduledCount: Int,
    val daysUntilReturn: Int
)

object DummyStudyProvider {

    private val flashcards = listOf(
        Flashcard(
            question = "¿Qué condición deben cumplir las series para aplicar el criterio de comparación directa?",
            answer = "Ambas series deben ser de términos positivos y debe cumplirse aₙ ≤ bₙ para todo n a partir de un índice.",
            source = "De tu apunte del 7 ago"
        ),
        Flashcard(
            question = "¿Cuándo converge una serie geométrica ∑ arⁿ?",
            answer = "Cuando |r| < 1. La suma es a/(1−r).",
            source = "De tu apunte del 3 ago"
        ),
        Flashcard(
            question = "Enunciá el criterio de la integral para series de términos positivos.",
            answer = "La serie ∑f(n) converge si y solo si la integral ∫f(x)dx de 1 a ∞ converge.",
            source = "De tu apunte del 5 ago"
        )
    )

    fun getFlashcards(courseId: Long): List<Flashcard> = flashcards

    fun getQuizQuestion(courseId: Long, index: Int = 2, total: Int = 5): QuizQuestion = QuizQuestion(
        topicTag = "TEMA AUSENTE",
        topicLabel = "3.4 Series de potencias",
        prompt = "¿Cuál es el radio de convergencia de la serie ∑ xⁿ / n!?",
        options = listOf("R = 1", "R = ∞", "R = 0", "R = e"),
        correctIndex = 1,
        explanation = "La serie exponencial ∑ xⁿ/n! converge para todo x ∈ ℝ, por lo que su radio de convergencia es infinito."
    )

    fun getQuizResult(courseId: Long): QuizResultInfo = QuizResultInfo(
        score = 7,
        total = 10,
        headline = "Series de potencias sigue floja",
        subtext = "Los 3 fallos son del mismo tema.",
        review = listOf(
            QuizResultRow(true, "1 · Definición de sucesión"),
            QuizResultRow(true, "2 · Serie geométrica"),
            QuizResultRow(false, "3 · Radio de convergencia"),
            QuizResultRow(false, "4 · Serie exponencial")
        ),
        rescheduledCount = 3,
        daysUntilReturn = 6
    )
}

// ───────────────────────────────── Sync queue ─────────────────────────────────

enum class SyncStatus { Syncing, Queued, Done }
data class SyncQueueItem(
    val noteId: Long,
    val title: String,
    val timestamp: String,
    val status: SyncStatus,
    val progress: Float = 0f
)

object DummySyncProvider {
    fun getQueueItems(): List<SyncQueueItem> = listOf(
        SyncQueueItem(1L, "Integrales impropias · p.1", "12 ago · 9:41 AM", SyncStatus.Syncing, progress = 0.72f),
        SyncQueueItem(2L, "Criterio de comparación · p.1", "7 ago · 8:20 AM", SyncStatus.Queued),
        SyncQueueItem(3L, "Series de potencias · p.2", "2 ago · 7:55 AM", SyncStatus.Done),
        SyncQueueItem(4L, "Prueba de la integral · p.1", "28 jul · 10:12 AM", SyncStatus.Done)
    )

    fun getPendingCount(): Int = getQueueItems().count { it.status != SyncStatus.Done }
}

// ───────────────────────────────── AI cost ─────────────────────────────────

data class AiCostBreakdownRow(val state: ConfidenceState, val label: String, val percent: Float, val count: String)
data class AiCostPaidCall(val label: String, val count: String)
data class AiCostStats(
    val periodLabel: String,
    val headlinePercent: Int,
    val isSystemWide: Boolean,
    val quotaUsed: Int? = null,
    val quotaTotal: Int? = null,
    val breakdown: List<AiCostBreakdownRow>,
    val paidCalls: List<AiCostPaidCall>,
    val footerNote: String
) {
    val quotaPercent: Int?
        get() = if (quotaUsed != null && quotaTotal != null && quotaTotal > 0) (quotaUsed * 100 / quotaTotal) else null
    val totalPaidCalls: Int
        get() = paidCalls.sumOf { it.count.toIntOrNull() ?: 0 }
}

object DummyAiCostProvider {
    private val breakdown = listOf(
        AiCostBreakdownRow(ConfidenceState.Verified, "N1 · ML Kit local", 68f, "612"),
        AiCostBreakdownRow(ConfidenceState.Repaired, "N1.5 · SimpleTex gratis", 13f, "117"),
        AiCostBreakdownRow(ConfidenceState.Escalated, "N2 · visión, en lote", 16f, "144"),
        AiCostBreakdownRow(ConfidenceState.Uncertain, "Sin resolver", 3f, "27")
    )

    private val paidCalls = listOf(
        AiCostPaidCall("IA-00 · reparación por visión", "31"),
        AiCostPaidCall("IA-01 · reconstrucción", "47"),
        AiCostPaidCall("IA-02 · flashcards y quizzes", "9"),
        AiCostPaidCall("IA-05 · adjudicación semántica", "6")
    )

    fun getStats(role: GlifoRole): AiCostStats {
        val isAdmin = role == GlifoRole.Admin
        val isTeacher = role == GlifoRole.Teacher
        return AiCostStats(
            periodLabel = when {
                isAdmin -> "Agosto · Sistema completo · 142 usuarios"
                isTeacher -> "Agosto · EIF411 · 27 estudiantes"
                else -> "Agosto · Cálculo II"
            },
            headlinePercent = if (isAdmin) 64 else 81,
            isSystemWide = isAdmin,
            quotaUsed = if (isAdmin) 1842 else null,
            quotaTotal = if (isAdmin) 2900 else null,
            breakdown = breakdown,
            paidCalls = paidCalls,
            footerNote = "Sin la escalera, 47 páginas habrían requerido 47 llamadas de visión de página completa."
        )
    }
}

// ───────────────────────────────── Teacher ─────────────────────────────────

data class GlossaryEntry(val term: String, val notation: String, val tag: String, val isSymbol: Boolean)
data class GlossarySuggestion(val term: String, val meta: String)
data class SyllabusTopicRow(val label: String, val hasIssue: Boolean)
data class SyllabusUnitRow(val label: String, val topicCount: Int, val topics: List<SyllabusTopicRow>)
data class TeacherGapTopic(val state: ConfidenceState, val label: String, val percent: Float, val fraction: String)

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

// ───────────────────────────────── Admin ─────────────────────────────────

data class AdminUserRow(
    val initials: String,
    val name: String,
    val email: String,
    val role: String, // "DOCENTE" | "ESTUDIANTE" | "ADMIN" — the real role, kept even while inactive
    val isActive: Boolean
) {
    /** What the badge/chip should show — "INACTIVO" overrides the real role visually. */
    val displayLabel: String get() = if (!isActive) "INACTIVO" else role
}

data class AdminRoleInfo(
    val name: String,
    val userCount: Int,
    val description: String,
    val privileges: List<String>,
    val extraPrivilegeCount: Int,
    val isSensitive: Boolean = false
)

object DummyAdminProvider {

    private val users = mutableListOf(
        AdminUserRow("MG", "Maikol Guzmán", "docente@una.ac.cr", "DOCENTE", isActive = true),
        AdminUserRow("BB", "Brandon Brenes", "estudiante@est.una.ac.cr", "ESTUDIANTE", isActive = true),
        AdminUserRow("DG", "David González", "dgonzalez@est.una.ac.cr", "ESTUDIANTE", isActive = true),
        AdminUserRow("FU", "Felipe Ugalde", "fugalde@est.una.ac.cr", "ESTUDIANTE", isActive = true),
        AdminUserRow("KJ", "Keneth Jara", "kjara@est.una.ac.cr", "ESTUDIANTE", isActive = false)
    )

    fun getUsers(): List<AdminUserRow> = users

    fun findUser(name: String): AdminUserRow = users.find { it.name == name } ?: users[2]

    fun getRecentSignups(): List<AdminUserRow> = users.takeLast(2)

    fun addUser(name: String, email: String, role: String) {
        val initials = name.trim().split(" ").filter { it.isNotBlank() }
            .take(2).map { it.first().uppercaseChar() }.joinToString("").ifEmpty { "??" }
        users.add(AdminUserRow(initials, name.trim(), email.trim(), role, isActive = true))
    }

    fun setActive(name: String, active: Boolean) {
        val index = users.indexOfFirst { it.name == name }
        if (index >= 0) users[index] = users[index].copy(isActive = active)
    }

    fun getRoles(): List<AdminRoleInfo> = listOf(
        AdminRoleInfo(
            "Estudiante", 131, "Captura, revisa y estudia sobre sus propios apuntes.",
            listOf("note:create", "note:read:own", "coverage:read:own"), extraPrivilegeCount = 3
        ),
        AdminRoleInfo(
            "Docente", 9, "Publica temario y glosario; ve brechas agregadas del grupo.",
            listOf("course:create", "syllabus:publish", "glossary:write"), extraPrivilegeCount = 6
        ),
        AdminRoleInfo(
            "Administrador", 2, "Usuarios, roles y estado del sistema.",
            listOf("user:manage", "role:assign", "system:read"), extraPrivilegeCount = 11, isSensitive = true
        )
    )

    fun getPrivilegesFor(roleLabel: String): List<String> = when (roleLabel.lowercase()) {
        "estudiante" -> listOf("note:create", "note:read:own", "coverage:read:own", "study:attempt", "course:join", "glossary:suggest")
        "docente" -> listOf("course:create", "syllabus:publish", "glossary:write", "coverage:read:group", "gaps:read", "ai_cost:read:course")
        else -> listOf("user:manage", "role:assign", "system:read", "ai_cost:read:system", "course:manage:any", "sync:read:any")
    }
}

// ───────────────────────────────── Capture pipeline ─────────────────────────────────

data class CaptureQualityCheck(val label: String, val scoreLabel: String, val percent: Float, val isWarning: Boolean)
data class CaptureStep(val state: String, val name: String, val description: String)

object DummyCaptureProvider {
    fun getQualityChecks(): List<CaptureQualityCheck> = listOf(
        CaptureQualityCheck("Nitidez", "12 / 35", 24f, isWarning = true),
        CaptureQualityCheck("Iluminación", "78", 78f, isWarning = false),
        CaptureQualityCheck("Sin reflejos", "91", 91f, isWarning = false),
        CaptureQualityCheck("Perspectiva", "corregida −4.2°", 100f, isWarning = false)
    )

    fun hasQualityWarning(): Boolean = getQualityChecks().any { it.isWarning }

    fun getProcessingSteps(): List<CaptureStep> = listOf(
        CaptureStep("done", "N0 · Preprocesamiento", "Perspectiva −4.2° · contraste normalizado · 18 regiones segmentadas"),
        CaptureStep("done", "N1 · OCR de texto", "18 de 18 regiones leídas · ML Kit, en el dispositivo · 1.1 s"),
        CaptureStep("done", "Clasificador de región", "14 texto · 3 fórmulas · 1 dibujo"),
        CaptureStep("now", "N1.5 · OCR matemático", "2 de 3 fórmulas convertidas a LaTeX · SimpleTex · capa gratuita"),
        CaptureStep("pending", "N2 · Reparación por visión", "Se activa solo si algo no supera las compuertas.")
    )
}

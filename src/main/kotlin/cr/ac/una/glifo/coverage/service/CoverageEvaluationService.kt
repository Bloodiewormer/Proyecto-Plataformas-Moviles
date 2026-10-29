package cr.ac.una.glifo.coverage.service

import cr.ac.una.glifo.ai.service.AmbiguousTopic
import cr.ac.una.glifo.ai.service.SemanticJudgeService
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.coverage.dto.CoverageResponse
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.study.entity.TopicCoverage
import cr.ac.una.glifo.study.repository.TopicCoverageRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class CoverageEvaluation(
    val coverage: CoverageResponse,
    val prefilterResolved: Int,
    val adjudicated: Int,
    val uncertain: Int,
    val aiCalled: Boolean
)

private enum class Source { PREFILTER, AI, OMITTED, NONE }

private data class Outcome(val state: String, val score: Float, val source: Source)

private data class Snapshot(val courseName: String, val leaves: List<PrefilterTopic>, val notes: List<PrefilterNote>)

/**
 * `POST /courses/{id}/coverage/evaluate`: recalcula la cobertura de los temas hoja del estudiante.
 * - Lee curso, temario y apuntes en una transacción corta; el pre-filtro determinista resuelve lo
 *   claro y solo los temas ambiguos van a IA-04 en una llamada, fuera de cualquier transacción.
 * - Lo que IA-04 recibió y no adjudicó conserva su evaluación previa (o queda UNCERTAIN si no
 *   tenía); lo ambiguo que no se pudo enviar (sin api key o fuera del lote) pasa a UNCERTAIN con
 *   score 0. Un ABSENT nuevo no se inserta: GET coverage ya lo muestra así.
 * - updated_at solo cambia si cambia el estado (o el score del pre-filtro), para que el delta
 *   refleje cambios reales y no la variación entre llamadas a la IA.
 * - Una sola evaluación a la vez por usuario y curso, y un minuto entre evaluaciones con IA
 *   exitosas. Ambos controles viven en memoria de esta instancia.
 */
@Service
class CoverageEvaluationService(
    private val coverageService: CoverageService,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val noteRepository: NoteRepository,
    private val topicCoverageRepository: TopicCoverageRepository,
    private val userRepository: UserRepository,
    private val prefilter: CoveragePrefilter,
    private val judge: SemanticJudgeService,
    private val transactions: TransactionOperations,
    private val jsonMapper: JsonMapper
) {

    private val inFlight = ConcurrentHashMap.newKeySet<Pair<Long, Long>>()
    private val lastAiCall = ConcurrentHashMap<Pair<Long, Long>, Instant>()

    fun evaluate(courseId: Long, userId: Long): CoverageEvaluation {
        val key = userId to courseId
        if (!inFlight.add(key)) {
            throw ConflictException("Ya hay una evaluación de cobertura en curso para este curso.", "COVERAGE_EVALUATION_IN_PROGRESS")
        }
        try {
            return evaluateExclusive(courseId, userId, key)
        } finally {
            inFlight.remove(key)
        }
    }

    private fun evaluateExclusive(courseId: Long, userId: Long, key: Pair<Long, Long>): CoverageEvaluation {
        val snapshot = transactions.execute { snapshot(courseId, userId) }
        val decisions = prefilter.decide(snapshot.leaves, snapshot.notes)
        val toJudge = decisions.filter { it.state == null }.take(MAX_TOPICS_PER_BATCH)
        val aiCalled = toJudge.isNotEmpty() && judge.isAvailable()
        val verdicts = if (aiCalled) {
            requireCooldownElapsed(key)
            judge.judge(snapshot.courseName, toJudge.map { AmbiguousTopic(it.topic.topicId, it.topic.code, it.topic.title, it.excerpts) })
                .also { recordAiCall(key) }
        } else {
            emptyMap()
        }

        val outcomes = decisions.associate { decision ->
            val verdict = verdicts[decision.topic.topicId]
            decision.topic.topicId to when {
                decision.state != null -> Outcome(decision.state, decision.score, Source.PREFILTER)
                verdict != null -> Outcome(verdict.state, if (verdict.state == "ABSENT") 0f else verdict.score, Source.AI)
                aiCalled && decision in toJudge -> Outcome("UNCERTAIN", 0f, Source.OMITTED)
                else -> Outcome("UNCERTAIN", 0f, Source.NONE)
            }
        }

        transactions.executeWithoutResult {
            val leafIds = snapshot.leaves.map { it.topicId }
            val existing = if (leafIds.isEmpty()) emptyMap() else
                topicCoverageRepository.findByUserIdAndSyllabusTopicIdIn(userId, leafIds).associateBy { it.syllabusTopic?.id }
            val now = Instant.now()
            for (topicId in leafIds) {
                val outcome = outcomes.getValue(topicId)
                val row = existing[topicId]
                if (!shouldWrite(row, outcome)) continue
                val target = row ?: TopicCoverage().apply {
                    user = userRepository.getReferenceById(userId)
                    syllabusTopic = syllabusTopicRepository.getReferenceById(topicId)
                }
                target.state = outcome.state
                target.score = outcome.score
                target.updatedAt = now
                topicCoverageRepository.save(target)
            }
        }

        val coverage = coverageService.getCoverage(courseId, userId)
        return CoverageEvaluation(
            coverage = coverage,
            prefilterResolved = decisions.count { it.state != null },
            adjudicated = verdicts.size,
            uncertain = coverage.stateCounts["UNCERTAIN"] ?: 0,
            aiCalled = aiCalled
        )
    }

    private fun snapshot(courseId: Long, userId: Long): Snapshot {
        val course = coverageService.findAccessibleCourse(courseId, userId)
        val topics = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
        val parentById = topics.associate { it.id to it.parent?.id }
        val leaves = topics.filter { it.id !in parentById.values }.map { topic ->
            val seen = mutableSetOf(topic.id)
            val ancestors = generateSequence(parentById[topic.id]?.takeIf(seen::add)) { parentById[it]?.takeIf(seen::add) }.toSet()
            PrefilterTopic(topic.id, topic.code, topic.title, ancestors)
        }
        val notes = noteRepository.findByUserIdAndCourseId(userId, courseId)
            .filter { it.status == NoteStatus.READY.name }
            .mapNotNull { note -> note.content?.takeIf { it.isNotBlank() }?.let { PrefilterNote(note.syllabusTopic?.id, noteText(it)) } }
            .filter { it.text.isNotBlank() }
        return Snapshot(course.name, leaves, notes)
    }

    private fun shouldWrite(row: TopicCoverage?, outcome: Outcome): Boolean = when {
        row == null -> outcome.state != "ABSENT"
        outcome.source == Source.OMITTED -> false
        outcome.source == Source.AI || outcome.source == Source.NONE -> row.state != outcome.state
        else -> row.state != outcome.state || abs(row.score - outcome.score) >= SCORE_TOLERANCE
    }

    private fun requireCooldownElapsed(key: Pair<Long, Long>) {
        val previous = lastAiCall[key]
        if (previous != null && Duration.between(previous, Instant.now()) < AI_COOLDOWN) {
            throw ConflictException("La cobertura se evaluó hace poco. Esperá un minuto antes de volver a evaluarla.", "COVERAGE_EVALUATION_COOLDOWN")
        }
    }

    private fun recordAiCall(key: Pair<Long, Long>) {
        val now = Instant.now()
        lastAiCall.entries.removeIf { Duration.between(it.value, now) >= AI_COOLDOWN }
        lastAiCall[key] = now
    }

    /** Texto de los bloques reconstruidos, sin título, encabezados sintetizados por IA-01 ni fragmentos inciertos. */
    private fun noteText(content: String): String {
        val blocks = runCatching { jsonMapper.readTree(content).path("blocks") }.getOrNull()
        if (blocks == null || !blocks.isArray) return ""
        val parts = mutableListOf<String>()
        for (block in blocks) {
            val synthesizedHeading = block.path("type").asString("") == "HEADING" && block.stringOrNull("regionId").isNullOrBlank()
            if (block.isUncertain() || synthesizedHeading) continue
            listOf("text", "latex", "caption").mapNotNullTo(parts) { block.stringOrNull(it) }
            block.path("spans").forEach { span -> if (!span.isUncertain()) span.stringOrNull("text")?.let(parts::add) }
        }
        return parts.joinToString("\n")
    }

    private fun JsonNode.isUncertain() =
        path("isUncertain").asBoolean(false) || path("resolvedAt").asString("").equals("UNRESOLVED", ignoreCase = true)

    private fun JsonNode.stringOrNull(field: String): String? = get(field)?.takeIf { it.isString }?.asString()

    companion object {
        const val MAX_TOPICS_PER_BATCH = 25
        val AI_COOLDOWN: Duration = Duration.ofMinutes(1)
        private const val SCORE_TOLERANCE = 0.005f
    }
}

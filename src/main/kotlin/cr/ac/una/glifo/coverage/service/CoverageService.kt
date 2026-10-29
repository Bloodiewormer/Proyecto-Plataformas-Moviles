package cr.ac.una.glifo.coverage.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.coverage.dto.CoverageDeltaResponse
import cr.ac.una.glifo.coverage.dto.CoverageResponse
import cr.ac.una.glifo.coverage.dto.TopicCoverageResponse
import cr.ac.una.glifo.study.entity.TopicCoverage
import cr.ac.una.glifo.study.repository.TopicCoverageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.math.roundToInt

/** CoverageState (Glifo_UML_Modeling.md §5.7); coincide con el CHECK de topic_coverage. */
private val COVERAGE_STATES = listOf("SOLID", "PARTIAL", "ABSENT", "UNCERTAIN")

/**
 * Cobertura del temario de un estudiante en un curso. Solo lee: la escribe CoverageEvaluationService.
 *
 * Reglas:
 * - Un tema sin fila en topic_coverage cuenta como ABSENT con score 0 (todavía no hay apuntes).
 * - El porcentaje del curso es el promedio del score de los temas hoja. Un tema padre
 *   (por ejemplo "U1") no tiene fila propia: su score es el promedio de sus subtemas, así
 *   no cuenta doble.
 * - Solo ve la cobertura quien está matriculado o es el docente dueño; si no, 404, igual
 *   que en los apuntes, para no revelar que el curso existe.
 */
@Service
class CoverageService(
    private val courseRepository: CourseRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val topicCoverageRepository: TopicCoverageRepository
) {

    @Transactional(readOnly = true)
    fun getCoverage(courseId: Long, userId: Long): CoverageResponse {
        findAccessibleCourse(courseId, userId)
        val topics = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
        val coverageByTopic = coverageByTopic(userId, topics)

        val parentIds = topics.mapNotNull { it.parent?.id }.toSet()
        val leaves = topics.filter { it.id !in parentIds }
        val leafRows = leaves.map { it.toLeafResponse(coverageByTopic[it.id]) }
        val leafById = leafRows.associateBy { it.topicId }

        val rows = topics.map { topic ->
            if (topic.id in parentIds) {
                val children = leafRows.filter { it.parentId == topic.id }
                TopicCoverageResponse(
                    topicId = topic.id,
                    parentId = topic.parent?.id,
                    code = topic.code,
                    title = topic.title,
                    state = null,
                    score = children.map { it.score }.averageOrZero(),
                    updatedAt = children.mapNotNull { it.updatedAt }.maxOrNull()
                )
            } else {
                leafById.getValue(topic.id)
            }
        }

        return CoverageResponse(
            courseId = courseId,
            coveragePct = toPercent(leafRows.map { it.score }.averageOrZero()),
            stateCounts = countByState(leafRows),
            topics = rows
        )
    }

    /**
     * Qué temas cambiaron desde [since]. Se calcula solo con topic_coverage.updated_at y no
     * con coverage_snapshots, que es Could-have y puede desaparecer (Glifo_UML_Modeling.md §4.5).
     */
    @Transactional(readOnly = true)
    fun getCoverageDelta(courseId: Long, userId: Long, since: Instant): CoverageDeltaResponse {
        if (since.isAfter(Instant.now())) {
            throw BadRequestException("La fecha 'since' no puede estar en el futuro")
        }
        findAccessibleCourse(courseId, userId)
        val topics = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
        val topicById = topics.associateBy { it.id }

        val changed = if (topics.isEmpty()) emptyList() else
            topicCoverageRepository.findByUserIdAndSyllabusTopicIdInAndUpdatedAtAfter(userId, topics.map { it.id }, since)
        val changedRows = changed.mapNotNull { coverage ->
            topicById[coverage.syllabusTopic?.id]?.toLeafResponse(coverage)
        }

        return CoverageDeltaResponse(
            courseId = courseId,
            since = since,
            changedCounts = countByState(changedRows),
            changedTopics = changedRows
        )
    }

    @Transactional(readOnly = true)
    fun findAccessibleCourse(courseId: Long, userId: Long): Course {
        val course = courseRepository.findById(courseId).orElseThrow { ResourceNotFoundException("Curso no encontrado") }
        val isOwner = course.owner?.id == userId
        if (!isOwner && !enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw ResourceNotFoundException("Curso no encontrado")
        }
        return course
    }

    private fun coverageByTopic(userId: Long, topics: List<SyllabusTopic>): Map<Long, TopicCoverage> {
        if (topics.isEmpty()) return emptyMap()
        return topicCoverageRepository.findByUserIdAndSyllabusTopicIdIn(userId, topics.map { it.id })
            .filter { it.syllabusTopic != null }
            .associateBy { it.syllabusTopic!!.id }
    }

    private fun SyllabusTopic.toLeafResponse(coverage: TopicCoverage?) = TopicCoverageResponse(
        topicId = id,
        parentId = parent?.id,
        code = code,
        title = title,
        state = coverage?.state ?: "ABSENT",
        score = coverage?.score ?: 0f,
        updatedAt = coverage?.updatedAt
    )

    /** Siempre trae los cuatro estados, aunque alguno quede en 0, para que la app no tenga que adivinar. */
    private fun countByState(rows: List<TopicCoverageResponse>): Map<String, Int> =
        COVERAGE_STATES.associateWith { state -> rows.count { it.state == state } }

    private fun List<Float>.averageOrZero(): Float = if (isEmpty()) 0f else average().toFloat()

    /** 0.4366 → 43.7 */
    private fun toPercent(score: Float): Float = (score * 1000).roundToInt() / 10f
}

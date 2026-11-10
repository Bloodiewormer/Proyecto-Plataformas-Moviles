package cr.ac.una.glifo.data.mapper

import cr.ac.una.glifo.data.dto.CourseGapsResponseDto
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.model.TeacherGroupCoverage
import kotlin.math.roundToInt

/**
 * GET coverage/gaps → lo que muestran Inicio docente y Brechas del grupo.
 * El color de cada tema sale de qué parte del grupo no lo tiene cubierto:
 * 60 % o más = ámbar (Escalated), 30 % o más = dudoso, menos = reparado.
 */
object TeacherGapsMapper {

    fun toGroupCoverage(dto: CourseGapsResponseDto): TeacherGroupCoverage {
        val enrolled = dto.enrolledStudents
        val topics = if (enrolled <= 0) emptyList() else dto.topics
            .filter { it.studentsWithGap > 0 }
            .map { topic ->
                val share = topic.studentsWithGap * 100f / enrolled
                TeacherGapTopic(
                    state = stateFor(share),
                    label = listOf(topic.code.trim(), topic.title.trim()).filter { it.isNotEmpty() }.joinToString(" "),
                    percent = share,
                    fraction = "${topic.studentsWithGap}/$enrolled"
                )
            }

        // Celdas estudiante × tema: de ahí salen las partes de la barra del grupo.
        val cells = (enrolled * dto.topics.size).toFloat()
        fun share(count: Int) = if (cells <= 0f) 0f else count * 100f / cells

        return TeacherGroupCoverage(
            enrolledStudents = enrolled,
            coveragePercent = dto.coveragePct.roundToInt().coerceIn(0, 100),
            topics = topics,
            solidShare = share(dto.topics.sumOf { it.solidCount }),
            partialShare = share(dto.topics.sumOf { it.partialCount }),
            absentShare = share(dto.topics.sumOf { it.absentCount }),
            uncertainShare = share(dto.topics.sumOf { it.uncertainCount })
        )
    }

    private fun stateFor(gapShare: Float): ConfidenceState = when {
        gapShare >= 60f -> ConfidenceState.Escalated
        gapShare >= 30f -> ConfidenceState.Uncertain
        else -> ConfidenceState.Repaired
    }
}

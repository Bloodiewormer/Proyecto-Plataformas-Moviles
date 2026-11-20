package cr.ac.una.glifo.coverage.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

// Solo DTOs de salida: la cobertura la calcula el backend (IA-04 en el Lab 6), el cliente
// nunca la envía. Los nombres del JSON van en snake_case, como el resto de la API.

/** `GET /courses/{id}/coverage`: cobertura del temario para el usuario autenticado. */
data class CoverageResponse(
    @JsonProperty("course_id") val courseId: Long,
    /** Promedio del score de los temas hoja, de 0 a 100. */
    @JsonProperty("coverage_pct") val coveragePct: Float,
    /** Cuántos temas hoja hay en cada estado (SOLID, PARTIAL, ABSENT, UNCERTAIN). */
    @JsonProperty("state_counts") val stateCounts: Map<String, Int>,
    val topics: List<TopicCoverageResponse>
)

data class TopicCoverageResponse(
    @JsonProperty("topic_id") val topicId: Long,
    @JsonProperty("parent_id") val parentId: Long?,
    val code: String,
    val title: String,
    /** Null en un tema padre: su estado es el de sus subtemas. */
    val state: String?,
    /** En un tema padre es el promedio de sus subtemas. */
    val score: Float,
    @JsonProperty("updated_at") val updatedAt: Instant?
)

/**
 * `GET /courses/{id}/coverage/gaps`: brechas del grupo para el docente dueño del curso.
 * Un estudiante tiene brecha en un tema si no está SOLID (incluye no tener fila: ABSENT).
 */
data class CourseGapsResponse(
    @JsonProperty("course_id") val courseId: Long,
    /** Estudiantes con matrícula ACTIVE. */
    @JsonProperty("enrolled_students") val enrolledStudents: Int,
    /** Promedio del grupo, de 0 a 100 (promedio de los scores de todos los temas hoja y estudiantes). */
    @JsonProperty("coverage_pct") val coveragePct: Float,
    /** Temas hoja ordenados de más a menos estudiantes con brecha. */
    val topics: List<TopicGapResponse>
)

data class TopicGapResponse(
    @JsonProperty("topic_id") val topicId: Long,
    val code: String,
    val title: String,
    @JsonProperty("students_with_gap") val studentsWithGap: Int,
    @JsonProperty("solid_count") val solidCount: Int,
    @JsonProperty("partial_count") val partialCount: Int,
    @JsonProperty("absent_count") val absentCount: Int,
    @JsonProperty("uncertain_count") val uncertainCount: Int,
    /** Promedio del score del tema en el grupo, de 0 a 100. */
    @JsonProperty("average_pct") val averagePct: Float
)

/** `GET /courses/{id}/coverage/delta?since=`: qué cambió desde una fecha (§4.5). */
data class CoverageDeltaResponse(
    @JsonProperty("course_id") val courseId: Long,
    val since: Instant,
    /** Cuántos temas quedaron en cada estado entre los que cambiaron. */
    @JsonProperty("changed_counts") val changedCounts: Map<String, Int>,
    @JsonProperty("changed_topics") val changedTopics: List<TopicCoverageResponse>
)

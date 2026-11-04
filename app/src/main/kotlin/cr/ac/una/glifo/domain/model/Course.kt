package cr.ac.una.glifo.domain.model

data class Course(
    val id: Long,
    val name: String,
    val code: String,
    val joinCode: String
)

data class CourseDetail(
    val id: Long,
    val name: String,
    val code: String,
    val joinCode: String,
    val syllabusStatus: String?
)

/** Tema del temario de un curso (#32, #44). */
data class SyllabusTopic(
    val id: Long,
    val courseId: Long,
    val parentId: Long? = null,
    val code: String,
    val title: String,
    val orderIndex: Int = 1
)

/**
 * Métricas de cobertura de un curso obtenidas del backend (`GET /courses/{id}/coverage`, #43).
 */
data class CourseStats(
    val courseId: Long,
    val instructorName: String,
    val coveragePercent: Int,
    val verified: Float,
    val repaired: Float,
    val escalated: Float,
    val uncertain: Float,
    val studentCount: Int,
    val noteCount: Int,
    val topicCount: Int,
    val pendingCount: Int,
    val weeklyDeltaPoints: Int,
    val hasSyllabus: Boolean,
    val solidCount: Int = 0,
    val partialCount: Int = 0,
    val absentCount: Int = 0,
    val uncertainCount: Int = 0
)

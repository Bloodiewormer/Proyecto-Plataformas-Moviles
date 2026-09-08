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

/**
 * Extra, UI-facing stats for a course (coverage breakdown, roster size, etc.) that a real
 * backend would likely compute server-side. Kept as its own model — rather than folded
 * into [CourseDetail] — so the DTO-backed course shape stays a clean mirror of the future
 * API contract while this stays free to evolve with the dummy data source.
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
    val hasSyllabus: Boolean
)

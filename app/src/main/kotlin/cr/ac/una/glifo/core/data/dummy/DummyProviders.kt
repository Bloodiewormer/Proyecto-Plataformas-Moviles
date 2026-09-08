package cr.ac.una.glifo.core.data.dummy

import cr.ac.una.glifo.core.network.dto.CourseDetailResponse
import cr.ac.una.glifo.core.network.dto.CourseResponse
import cr.ac.una.glifo.core.network.dto.NoteResponse
import cr.ac.una.glifo.domain.model.ConfidenceState

/**
 * Extra, UI-only stats for a course (coverage breakdown, roster size, etc.). Kept
 * separate from [CourseResponse]/[CourseDetailResponse] so those stay a clean mirror
 * of the real backend contract — this is the part a real API would likely compute
 * server-side, and is the seam to delete once it does.
 */
data class DummyCourseStats(
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

object DummyCourseProvider {
    /** Courses the dummy student (see [DummyAuthProvider]) is enrolled in. */
    val getCourses = listOf(
        CourseResponse(id = 1L, name = "Cálculo II", code = "MAT-202", joinCode = "CALC-202"),
        CourseResponse(id = 2L, name = "Física General II", code = "FIS-204", joinCode = "FIS-204"),
        CourseResponse(id = 3L, name = "Plataformas Móviles", code = "EIF411", joinCode = "GLF-4K2P")
    )

    /** Courses the dummy teacher (see [DummyAuthProvider]) teaches. */
    val getTeacherCourses = listOf(
        CourseResponse(id = 3L, name = "Plataformas Móviles", code = "EIF411", joinCode = "GLF-4K2P"),
        CourseResponse(id = 4L, name = "Álgebra Lineal", code = "MAT-101", joinCode = "ALG-101X")
    )

    private val allCourses = (getCourses + getTeacherCourses).distinctBy { it.id }

    private val stats = listOf(
        DummyCourseStats(1L, "Prof. Rodríguez", 68, 54f, 20f, 14f, 12f, 34, 14, 33, 13, 6, hasSyllabus = true),
        DummyCourseStats(2L, "Prof. Mora", 45, 31f, 14f, 25f, 30f, 29, 8, 21, 9, 0, hasSyllabus = true),
        DummyCourseStats(3L, "Prof. Maikol Guzmán", 54, 40f, 24f, 26f, 10f, 27, 11, 12, 5, 8, hasSyllabus = true),
        DummyCourseStats(4L, "Prof. Maikol Guzmán", 0, 0f, 0f, 0f, 0f, 18, 0, 0, 0, 0, hasSyllabus = false)
    )

    fun findCourse(courseId: Long): CourseResponse? = allCourses.find { it.id == courseId }

    fun getCourseByJoinCode(joinCode: String): CourseResponse? =
        allCourses.find { it.joinCode.equals(joinCode.trim(), ignoreCase = true) }

    fun getStats(courseId: Long): DummyCourseStats =
        stats.find { it.courseId == courseId } ?: stats.first()

    fun getCourseDetail(courseId: Long): CourseDetailResponse? {
        val course = findCourse(courseId) ?: return null
        val courseStats = getStats(courseId)
        return CourseDetailResponse(
            id = course.id,
            name = course.name,
            code = course.code,
            joinCode = course.joinCode,
            syllabusStatus = if (courseStats.hasSyllabus) "UPLOADED" else "MISSING"
        )
    }
}

data class DummyNoteMeta(
    val note: NoteResponse,
    val chips: List<ConfidenceState>,
    val pageCount: Int,
    val displayDate: String
)

object DummyNoteProvider {
    private val notes = listOf(
        DummyNoteMeta(
            NoteResponse(id = 1L, courseId = 1L, title = "Integrales impropias", classDate = "2026-08-12"),
            listOf(ConfidenceState.Verified, ConfidenceState.Repaired, ConfidenceState.Escalated),
            pageCount = 3, displayDate = "12 ago"
        ),
        DummyNoteMeta(
            NoteResponse(id = 2L, courseId = 1L, title = "Criterio de comparación", classDate = "2026-08-07"),
            listOf(ConfidenceState.Verified, ConfidenceState.Uncertain),
            pageCount = 2, displayDate = "7 ago"
        ),
        DummyNoteMeta(
            NoteResponse(id = 3L, courseId = 1L, title = "Series de potencias", classDate = "2026-08-02"),
            listOf(ConfidenceState.Verified, ConfidenceState.Repaired),
            pageCount = 4, displayDate = "2 ago"
        ),
        DummyNoteMeta(
            NoteResponse(id = 4L, courseId = 1L, title = "Prueba de la integral", classDate = "2026-07-28"),
            listOf(ConfidenceState.Verified),
            pageCount = 2, displayDate = "28 jul"
        )
    )

    fun getNotesForCourse(courseId: Long): List<NoteResponse> = notes.map { it.note }

    fun getNoteMetaList(): List<DummyNoteMeta> = notes

    fun findNote(noteId: Long): NoteResponse = notes.find { it.note.id == noteId }?.note ?: notes.first().note

    fun findMeta(noteId: Long): DummyNoteMeta = notes.find { it.note.id == noteId } ?: notes.first()

    fun getChips(noteId: Long): List<ConfidenceState> =
        notes.find { it.note.id == noteId }?.chips ?: notes.first().chips
}

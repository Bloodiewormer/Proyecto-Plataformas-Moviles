package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.data.dto.CourseDetailResponse
import cr.ac.una.glifo.data.dto.CourseResponse
import cr.ac.una.glifo.domain.model.CourseStats

/**
 * Temporary local data source for courses (see [DummyAuthProvider] doc for why this stays
 * until there's a real backend). Emits the same DTOs [cr.ac.una.glifo.data.datasource.remote.GlifoApi]
 * will eventually return, so [cr.ac.una.glifo.data.repository.CourseRepositoryImpl] maps
 * them through [cr.ac.una.glifo.data.mapper.toDomain] exactly like it would for real
 * network responses. [CourseStats] has no DTO counterpart yet (it's UI-facing test data a
 * real backend would compute server-side), so it's produced directly as a domain model.
 */
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
        CourseStats(1L, "Prof. Rodríguez", 68, 54f, 20f, 14f, 12f, 34, 14, 33, 13, 6, hasSyllabus = true),
        CourseStats(2L, "Prof. Mora", 45, 31f, 14f, 25f, 30f, 29, 8, 21, 9, 0, hasSyllabus = true),
        CourseStats(3L, "Prof. Maikol Guzmán", 54, 40f, 24f, 26f, 10f, 27, 11, 12, 5, 8, hasSyllabus = true),
        CourseStats(4L, "Prof. Maikol Guzmán", 0, 0f, 0f, 0f, 0f, 18, 0, 0, 0, 0, hasSyllabus = false)
    )

    fun findCourse(courseId: Long): CourseResponse? = allCourses.find { it.id == courseId }

    fun getCourseByJoinCode(joinCode: String): CourseResponse? =
        allCourses.find { it.joinCode.equals(joinCode.trim(), ignoreCase = true) }

    fun getStats(courseId: Long): CourseStats =
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

    private var nextId = 100L

    /** Appends a teacher-created course (TeacherNewCourseScreen). Session-only, like registration. */
    fun createCourse(name: String, code: String, term: String): CourseResponse {
        val course = CourseResponse(id = nextId++, name = name.trim(), code = code.trim(), joinCode = code.trim().uppercase())
        return course
    }
}

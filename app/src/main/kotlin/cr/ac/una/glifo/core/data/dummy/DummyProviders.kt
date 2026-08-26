package cr.ac.una.glifo.core.data.dummy

import cr.ac.una.glifo.core.network.dto.CourseDetailResponse
import cr.ac.una.glifo.core.network.dto.CourseResponse
import cr.ac.una.glifo.core.network.dto.NoteResponse

object DummyCourseProvider {
    val getCourses = listOf(
        CourseResponse(
            id = 1L,
            name = "Cálculo II",
            code = "MAT-202",
            joinCode = "CALC-202"
        ),
        CourseResponse(
            id = 2L,
            name = "Física General II",
            code = "FIS-204",
            joinCode = "FIS-204"
        ),
        CourseResponse(
            id = 3L,
            name = "Plataformas Móviles",
            code = "EIF411",
            joinCode = "GLF-4K2P"
        )
    )

    fun getCourseDetail(courseId: Long): CourseDetailResponse? {
        val course = getCourses.find { it.id == courseId } ?: return null
        return CourseDetailResponse(
            id = course.id,
            name = course.name,
            code = course.code,
            joinCode = course.joinCode,
            syllabusStatus = if (courseId == 3L) "UPLOADED" else "MISSING"
        )
    }
}

object DummyNoteProvider {
    fun getNotesForCourse(courseId: Long): List<NoteResponse> {
        return listOf(
            NoteResponse(
                id = 101L,
                courseId = courseId,
                title = "Integrales impropias",
                classDate = "2026-08-12"
            ),
            NoteResponse(
                id = 102L,
                courseId = courseId,
                title = "Criterio de comparación",
                classDate = "2026-08-07"
            ),
            NoteResponse(
                id = 103L,
                courseId = courseId,
                title = "Series de potencias",
                classDate = "2026-08-02"
            )
        )
    }
}

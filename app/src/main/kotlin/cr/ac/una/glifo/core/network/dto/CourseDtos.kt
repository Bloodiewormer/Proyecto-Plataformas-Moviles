package cr.ac.una.glifo.core.network.dto

data class ApiResponse<T>(val data: T, val meta: Map<String, Any>? = null)
data class CourseResponse(val id: Long, val name: String, val code: String, val joinCode: String, val emoji: String?)
data class CourseDetailResponse(val id: Long, val name: String, val code: String, val joinCode: String, val emoji: String?, val syllabusStatus: String?)
data class CreateCourseRequest(val name: String, val code: String, val term: String)
data class JoinCourseRequest(val joinCode: String)
data class EnrollmentResponse(val id: Long, val courseId: Long, val status: String)
data class NoteResponse(val id: Long, val courseId: Long, val title: String, val classDate: String)
data class NoteDetailResponse(val id: Long, val courseId: Long, val title: String, val classDate: String, val content: Any?)
data class CreateNoteRequest(val courseId: Long, val title: String, val classDate: String)

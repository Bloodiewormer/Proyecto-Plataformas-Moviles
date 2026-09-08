package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("data") val data: T,
    @SerializedName("meta") val meta: Map<String, Any>? = null
)

data class CourseResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("join_code") val joinCode: String
)

data class CourseDetailResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("join_code") val joinCode: String,
    @SerializedName("syllabus_status") val syllabusStatus: String?
)

data class CreateCourseRequest(
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("term") val term: String
)

data class JoinCourseRequest(
    @SerializedName("join_code") val joinCode: String
)

data class EnrollmentResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("status") val status: String
)

data class NoteResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("class_date") val classDate: String
)

data class NoteDetailResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("class_date") val classDate: String,
    @SerializedName("content") val content: Any?
)

data class CreateNoteRequest(
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("class_date") val classDate: String
)

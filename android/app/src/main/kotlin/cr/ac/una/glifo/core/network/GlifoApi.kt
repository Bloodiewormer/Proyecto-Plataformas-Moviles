package cr.ac.una.glifo.core.network

import cr.ac.una.glifo.core.network.dto.ApiResponse
import cr.ac.una.glifo.core.network.dto.AuthResponse
import cr.ac.una.glifo.core.network.dto.CourseDetailResponse
import cr.ac.una.glifo.core.network.dto.CourseResponse
import cr.ac.una.glifo.core.network.dto.CreateCourseRequest
import cr.ac.una.glifo.core.network.dto.CreateNoteRequest
import cr.ac.una.glifo.core.network.dto.EnrollmentResponse
import cr.ac.una.glifo.core.network.dto.JoinCourseRequest
import cr.ac.una.glifo.core.network.dto.LoginRequest
import cr.ac.una.glifo.core.network.dto.NoteDetailResponse
import cr.ac.una.glifo.core.network.dto.NoteResponse
import cr.ac.una.glifo.core.network.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GlifoApi {
    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    // Courses
    @GET("courses")
    suspend fun getCourses(): ApiResponse<List<CourseResponse>>

    @POST("courses")
    suspend fun createCourse(@Body request: CreateCourseRequest): ApiResponse<CourseResponse>

    @GET("courses/{id}")
    suspend fun getCourse(@Path("id") id: Long): ApiResponse<CourseDetailResponse>

    @POST("enrollments")
    suspend fun joinCourse(@Body request: JoinCourseRequest): ApiResponse<EnrollmentResponse>

    // Notes
    @GET("notes")
    suspend fun getNotes(@Query("courseId") courseId: Long): ApiResponse<List<NoteResponse>>

    @POST("notes")
    suspend fun createNote(@Body request: CreateNoteRequest): ApiResponse<NoteResponse>

    @GET("notes/{id}")
    suspend fun getNote(@Path("id") id: Long): ApiResponse<NoteDetailResponse>

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long): Response<Unit>
}

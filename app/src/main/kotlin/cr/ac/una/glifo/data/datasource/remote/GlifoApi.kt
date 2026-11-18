package cr.ac.una.glifo.data.datasource.remote

import cr.ac.una.glifo.data.dto.AdminChangeRoleRequest
import cr.ac.una.glifo.data.dto.AdminResetPasswordRequest
import cr.ac.una.glifo.data.dto.AdminRoleDto
import cr.ac.una.glifo.data.dto.UserDto
import cr.ac.una.glifo.data.dto.AdminResetPasswordResponse
import cr.ac.una.glifo.data.dto.AiUsageResponseDto
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.AuthResponse
import cr.ac.una.glifo.data.dto.ChangePasswordRequest
import cr.ac.una.glifo.data.dto.ForgotPasswordRequest
import cr.ac.una.glifo.data.dto.CourseDetailResponse
import cr.ac.una.glifo.data.dto.CourseGlossaryResponse
import cr.ac.una.glifo.data.dto.CourseResponse
import cr.ac.una.glifo.data.dto.CourseGapsResponseDto
import cr.ac.una.glifo.data.dto.CoverageDeltaResponseDto
import cr.ac.una.glifo.data.dto.CoverageResponseDto
import cr.ac.una.glifo.data.dto.CreateCourseRequest
import cr.ac.una.glifo.data.dto.CreateGlossaryEntryRequest
import cr.ac.una.glifo.data.dto.CreateGlossarySuggestionRequest
import cr.ac.una.glifo.data.dto.CreateNotePageRequest
import cr.ac.una.glifo.data.dto.CreateNoteRequest
import cr.ac.una.glifo.data.dto.CreateSyllabusTopicRequest
import cr.ac.una.glifo.data.dto.DeviceResponse
import cr.ac.una.glifo.data.dto.EnrollmentResponse
import cr.ac.una.glifo.data.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.data.dto.GlossaryEntryResponse
import cr.ac.una.glifo.data.dto.GlossarySuggestionResponse
import cr.ac.una.glifo.data.dto.HealthResponseDto
import cr.ac.una.glifo.data.dto.JoinCourseRequest
import cr.ac.una.glifo.data.dto.LoginRequest
import cr.ac.una.glifo.data.dto.MathOcrResponseDto
import cr.ac.una.glifo.data.dto.NoteDetailResponse
import cr.ac.una.glifo.data.dto.NotePageResponse
import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.data.dto.PageRepairResponseDto
import cr.ac.una.glifo.data.dto.PublishSyllabusRequest
import cr.ac.una.glifo.data.dto.RegisterDeviceRequest
import cr.ac.una.glifo.data.dto.RegisterRequest
import cr.ac.una.glifo.data.dto.ResolveGlossarySuggestionRequest
import cr.ac.una.glifo.data.dto.StudyItemResponse
import cr.ac.una.glifo.data.dto.SyllabusDocumentResponse
import cr.ac.una.glifo.data.dto.SyllabusTopicResponse
import cr.ac.una.glifo.data.dto.SyncCompleteRequest
import cr.ac.una.glifo.data.dto.SyncCompleteResponse
import cr.ac.una.glifo.data.dto.UpdateDeviceTokenRequest
import cr.ac.una.glifo.data.dto.UpdateNoteRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface GlifoApi {
    @GET("health")
    suspend fun getHealth(): ApiResponse<HealthResponseDto>

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    /** Pide token. 204 si cambió; 400 con mensaje si la actual no coincide o la nueva no sirve. */
    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    /** Sin token. 204 si cambió; 404 si el correo no existe; 400 si la nueva no sirve. */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Unit>

    /** Foto de perfil del usuario con sesión: 204 al subir; JPEG/PNG de hasta 1 MB. */
    @Multipart
    @PUT("users/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<Unit>

    /** 200 con la imagen, o 404 si el usuario no tiene foto en el servidor. */
    @GET("users/me/avatar")
    suspend fun getAvatar(): Response<ResponseBody>

    /** Solo admin (USER_MANAGE). 404 si el correo no existe; 400 si es la propia cuenta o el correo no es válido. */
    /** Solo admin (USER_MANAGE): todas las cuentas con su rol. */
    @GET("admin/users")
    suspend fun getAdminUsers(): ApiResponse<List<UserDto>>

    /** Solo admin (USER_MANAGE): los roles con sus privilegios y cuántas cuentas tiene cada uno. */
    @GET("admin/users/roles")
    suspend fun getAdminRoles(): ApiResponse<List<AdminRoleDto>>

    /** Solo admin (ROLE_MANAGE). 400 si el rol no existe o es la propia cuenta. */
    @PUT("admin/users/{id}/role")
    suspend fun changeUserRole(@Path("id") userId: Long, @Body request: AdminChangeRoleRequest): ApiResponse<UserDto>

    @POST("admin/users/reset-password")
    suspend fun adminResetPassword(@Body request: AdminResetPasswordRequest): ApiResponse<AdminResetPasswordResponse>

    // Courses
    @GET("courses")
    suspend fun getCourses(): ApiResponse<List<CourseResponse>>

    @POST("courses")
    suspend fun createCourse(@Body request: CreateCourseRequest): ApiResponse<CourseResponse>

    @GET("courses/{id}")
    suspend fun getCourse(@Path("id") id: Long): ApiResponse<CourseDetailResponse>

    @POST("enrollments")
    suspend fun joinCourse(@Body request: JoinCourseRequest): ApiResponse<EnrollmentResponse>

    // Temario / Topics del curso (#32)
    @GET("courses/{id}/syllabus")
    suspend fun getCourseSyllabus(@Path("id") courseId: Long): ApiResponse<SyllabusDocumentResponse>

    @Multipart
    @POST("courses/{id}/syllabus/pdf")
    suspend fun uploadSyllabusPdf(
        @Path("id") courseId: Long,
        @Part file: MultipartBody.Part
    ): ApiResponse<SyllabusDocumentResponse>

    @POST("courses/{id}/syllabus")
    suspend fun publishSyllabus(
        @Path("id") courseId: Long,
        @Body request: PublishSyllabusRequest
    ): ApiResponse<List<SyllabusTopicResponse>>

    @GET("courses/{id}/topics")
    suspend fun getCourseTopics(@Path("id") courseId: Long): ApiResponse<List<SyllabusTopicResponse>>

    @POST("courses/{id}/topics")
    suspend fun addCourseTopic(
        @Path("id") courseId: Long,
        @Body request: CreateSyllabusTopicRequest
    ): ApiResponse<SyllabusTopicResponse>

    // Glosario + versiones para el contexto offline del pipeline (D-17, issue #27)
    @GET("courses/{id}/glossary")
    suspend fun getCourseGlossary(@Path("id") courseId: Long): ApiResponse<CourseGlossaryResponse>

    // Gestión del glosario por el docente y moderación de sugerencias (issue #33, D-15)
    @POST("courses/{id}/glossary")
    suspend fun addGlossaryEntry(
        @Path("id") courseId: Long,
        @Body request: CreateGlossaryEntryRequest
    ): ApiResponse<GlossaryEntryResponse>

    @GET("courses/{id}/glossary/suggestions")
    suspend fun getGlossarySuggestions(@Path("id") courseId: Long): ApiResponse<List<GlossarySuggestionResponse>>

    @POST("courses/{id}/glossary/suggestions")
    suspend fun suggestGlossaryEntry(
        @Path("id") courseId: Long,
        @Body request: CreateGlossarySuggestionRequest
    ): ApiResponse<GlossarySuggestionResponse>

    @PATCH("courses/{id}/glossary/suggestions/{suggestionId}")
    suspend fun resolveGlossarySuggestion(
        @Path("id") courseId: Long,
        @Path("suggestionId") suggestionId: Long,
        @Body request: ResolveGlossarySuggestionRequest
    ): ApiResponse<GlossarySuggestionResponse>

    // Notes
    @GET("notes")
    suspend fun getNotes(@Query("courseId") courseId: Long): ApiResponse<List<NoteResponse>>

    @POST("notes")
    suspend fun createNote(@Body request: CreateNoteRequest): ApiResponse<NoteResponse>

    @GET("notes/{id}")
    suspend fun getNote(@Path("id") id: Long): ApiResponse<NoteDetailResponse>

    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") id: Long,
        @Body request: UpdateNoteRequest
    ): ApiResponse<NoteResponse>

    // Reconstrucción IA-01 (#42)
    @POST("notes/{id}/reconstruct")
    suspend fun reconstructNote(@Path("id") id: Long): ApiResponse<NoteDetailResponse>

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long): Response<Unit>

    // Página ya procesada en el teléfono; la sube la cola offline (issue #26)
    @POST("notes/{id}/pages")
    suspend fun uploadPage(@Path("id") noteId: Long, @Body request: CreateNotePageRequest): ApiResponse<NotePageResponse>

    // OCR matemático N1.5 (#38 / #39)
    @Multipart
    @POST("notes/{id}/pages/{pageIndex}/regions/{regionId}/math")
    suspend fun transcribeMathRegion(
        @Path("id") noteId: Long,
        @Path("pageIndex") pageIndex: Int,
        @Path("regionId") regionId: String,
        @Part crop: MultipartBody.Part
    ): ApiResponse<MathOcrResponseDto>

    // Reparación por visión N2 en lote por página (#41)
    @Multipart
    @POST("notes/{id}/pages/{pageIndex}/repair")
    suspend fun repairPage(
        @Path("id") noteId: Long,
        @Path("pageIndex") pageIndex: Int,
        @Part crops: List<MultipartBody.Part>
    ): ApiResponse<PageRepairResponseDto>

    // Material de estudio IA-02 (#44)
    @POST("courses/{courseId}/study-items/generate")
    suspend fun generateStudyItems(
        @Path("courseId") courseId: Long,
        @Body request: GenerateStudyItemsRequest
    ): ApiResponse<List<StudyItemResponse>>

    @GET("courses/{courseId}/study-items")
    suspend fun getStudyItems(
        @Path("courseId") courseId: Long,
        @Query("topicId") topicId: Long? = null
    ): ApiResponse<List<StudyItemResponse>>

    // Cobertura del temario e IA-04 (#43)
    @GET("courses/{courseId}/coverage")
    suspend fun getCoverage(
        @Path("courseId") courseId: Long
    ): ApiResponse<CoverageResponseDto>

    /** Brechas del grupo (docente dueño, COVERAGE_READ_COURSE). */
    @GET("courses/{courseId}/coverage/gaps")
    suspend fun getCourseGaps(
        @Path("courseId") courseId: Long
    ): ApiResponse<CourseGapsResponseDto>

    @GET("courses/{courseId}/coverage/delta")
    suspend fun getCoverageDelta(
        @Path("courseId") courseId: Long,
        @Query("since") since: String
    ): ApiResponse<CoverageDeltaResponseDto>

    @POST("courses/{courseId}/coverage/evaluate")
    suspend fun evaluateCoverage(
        @Path("courseId") courseId: Long
    ): ApiResponse<CoverageResponseDto>

    // Registro de dispositivos y notificación de fin de cola offline (#48)
    @GET("devices")
    suspend fun listDevices(): ApiResponse<List<DeviceResponse>>

    @POST("devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): ApiResponse<DeviceResponse>

    @PUT("devices/{id}/token")
    suspend fun updateDeviceToken(
        @Path("id") deviceId: Long,
        @Body request: UpdateDeviceTokenRequest
    ): ApiResponse<DeviceResponse>

    @DELETE("devices/{id}")
    suspend fun unlinkDevice(@Path("id") deviceId: Long): Response<Unit>

    @POST("sync/complete")
    suspend fun completeSync(@Body request: SyncCompleteRequest): ApiResponse<SyncCompleteResponse>

    // Telemetría y costos de IA (#46 / #47)
    @GET("ai-usage/me")
    suspend fun getMyAiUsage(
        @Query("courseId") courseId: Long? = null
    ): ApiResponse<AiUsageResponseDto>

    @GET("courses/{courseId}/ai-usage")
    suspend fun getCourseAiUsage(
        @Path("courseId") courseId: Long
    ): ApiResponse<AiUsageResponseDto>
}

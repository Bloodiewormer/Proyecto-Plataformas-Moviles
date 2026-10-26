package cr.ac.una.glifo.data.datasource.remote

import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.AuthResponse
import cr.ac.una.glifo.data.dto.CourseDetailResponse
import cr.ac.una.glifo.data.dto.CourseGlossaryResponse
import cr.ac.una.glifo.data.dto.CourseResponse
import cr.ac.una.glifo.data.dto.CreateCourseRequest
import cr.ac.una.glifo.data.dto.CreateGlossaryEntryRequest
import cr.ac.una.glifo.data.dto.CreateNotePageRequest
import cr.ac.una.glifo.data.dto.CreateNoteRequest
import cr.ac.una.glifo.data.dto.CreateSyllabusTopicRequest
import cr.ac.una.glifo.data.dto.EnrollmentResponse
import cr.ac.una.glifo.data.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.data.dto.GlossaryEntryResponse
import cr.ac.una.glifo.data.dto.GlossarySuggestionResponse
import cr.ac.una.glifo.data.dto.JoinCourseRequest
import cr.ac.una.glifo.data.dto.LoginRequest
import cr.ac.una.glifo.data.dto.NoteDetailResponse
import cr.ac.una.glifo.data.dto.NotePageResponse
import cr.ac.una.glifo.data.dto.NoteResponse
import cr.ac.una.glifo.data.dto.PublishSyllabusRequest
import cr.ac.una.glifo.data.dto.RegisterRequest
import cr.ac.una.glifo.data.dto.ResolveGlossarySuggestionRequest
import cr.ac.una.glifo.data.dto.StudyItemResponse
import cr.ac.una.glifo.data.dto.SyllabusTopicResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Remote data source (Retrofit). Consumed today only by
 * [cr.ac.una.glifo.data.repository.AuthRepositoryImpl] (`login`/`register`) — the Lab 3
 * requirement (Hilt + Retrofit + Gson + error handling + mock API consumption). It's called
 * against a **Mockoon** mock (see Glifo_Bitacora_Decisiones.md for the tool choice; the
 * professor's own "API de Prueba (Mock API)" lesson lists Mockoon/Beeceptor/Mocky/JSON
 * Server/Postman Mock Server — Mockoon was picked because it runs locally, matching Glifo's
 * offline-first principle, and it can simulate arbitrary status codes/delays for the
 * error-handling requirement without a network dependency during the defense).
 *
 * Every other `*RepositoryImpl` in `data/repository/` still talks to the dummy providers in
 * `data/datasource/dummy/` (see D-24 / Glifo_Bitacora_Decisiones.md and the "temporary until
 * there's a real backend" note on those providers) — this interface and
 * [cr.ac.una.glifo.data.di.NetworkModule] stay ready so swapping another repository impl
 * from dummy to remote is a constructor change, not a redesign.
 */
interface GlifoApi {
    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

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

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long): Response<Unit>

    // Página ya procesada en el teléfono; la sube la cola offline (issue #26)
    @POST("notes/{id}/pages")
    suspend fun uploadPage(@Path("id") noteId: Long, @Body request: CreateNotePageRequest): ApiResponse<NotePageResponse>

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
}

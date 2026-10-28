package cr.ac.una.glifo.security

import cr.ac.una.glifo.common.exception.GlobalExceptionHandler
import cr.ac.una.glifo.course.controller.CourseController
import cr.ac.una.glifo.course.dto.CourseResponse
import cr.ac.una.glifo.course.dto.CreateCourseRequest
import cr.ac.una.glifo.course.service.CourseService
import cr.ac.una.glifo.user.controller.AuthController
import cr.ac.una.glifo.user.entity.Privilege
import cr.ac.una.glifo.user.entity.Role
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import cr.ac.una.glifo.user.service.AuthService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [SecurityHttpTest.TestConfig::class])
@WebAppConfiguration
@TestPropertySource(properties = [
    "url.auth=/api/v1/auth",
    "url.health=/api/v1/health",
    "url.courses=/api/v1/courses",
    "glifo.jwt.secret=http-tests-only-signing-key-not-for-production-0123456789abcdef",
    "glifo.jwt.expiration-ms=60000"
])
class SecurityHttpTest {

    @Autowired private lateinit var context: WebApplicationContext
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var courseService: CourseService
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var jwtProvider: JwtProvider
    private lateinit var mvc: MockMvc
    private lateinit var account: User
    private val email = "student@example.test"
    private val password = "test-password"
    private val mapper = JsonMapper.builder().findAndAddModules().build()
    private val courseRequest = CreateCourseRequest("CS101", "Test Course", "II-2026")

    @BeforeEach
    fun setUp() {
        reset(userRepository, roleRepository, courseService)
        account = User().apply {
            id = 1L
            email = this@SecurityHttpTest.email
            passwordHash = passwordEncoder.encode(password)!!
            roles.add(Role().apply { id = 1L; name = "ROLE_STUDENT" })
        }
        `when`(userRepository.findByEmail(email)).thenReturn(account)
        `when`(courseService.getUserCourses(1L)).thenReturn(emptyList())
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @Test
    fun `protected endpoint without a token returns JSON 401`() {
        courses().andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        verifyNoInteractions(courseService)
    }

    @Test
    fun `malformed token returns 401 without calling a controller service`() {
        courses("Bearer invalid-token").andExpect(status().isUnauthorized)
        verifyNoInteractions(courseService)
    }

    @Test
    fun `expired token returns 401`() {
        val token = JwtProvider(TEST_SIGNING_KEY, -60_000).generateToken(email, 1L, emptyList())
        courses("Bearer $token").andExpect(status().isUnauthorized)
        verifyNoInteractions(courseService)
    }

    @Test
    fun `disabled account with an existing token returns 401`() {
        val token = bearer()
        account.isActive = false

        courses(token).andExpect(status().isUnauthorized)
        verifyNoInteractions(courseService)
    }

    @Test
    fun `deleted account with an existing token returns 401`() {
        `when`(userRepository.findByEmail(email)).thenReturn(null)

        courses(bearer()).andExpect(status().isUnauthorized)
        verifyNoInteractions(courseService)
    }

    @Test
    fun `active account reaches the real course controller`() {
        courses(bearer()).andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
        verify(courseService).getUserCourses(1L)
    }

    @Test
    fun `privilege in token cannot grant course creation`() {
        createCourse(bearer()).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        verifyNoInteractions(courseService)
    }

    @Test
    fun `teacher role alone cannot replace the course write privilege`() {
        account.roles.first().name = "ROLE_TEACHER"

        createCourse(bearer()).andExpect(status().isForbidden)
        verifyNoInteractions(courseService)
    }

    @Test
    fun `current course write privilege allows creation without token claim`() {
        account.roles.first().privileges.add(Privilege().apply { id = 1L; name = "COURSE_WRITE" })
        val created = CourseResponse(10L, courseRequest.name, courseRequest.code, courseRequest.term, "TEST01", "PENDING")
        `when`(courseService.createCourse(courseRequest, 1L)).thenReturn(created)
        val token = jwtProvider.generateToken(email, 1L, emptyList())

        createCourse("Bearer $token").andExpect(status().isCreated)
            .andExpect(header().string("Location", "http://localhost/api/v1/courses/10"))
            .andExpect(jsonPath("$.data.id").value(10))
        verify(courseService).createCourse(courseRequest, 1L)
    }

    @Test
    fun `login is public and issues a token usable on protected endpoints`() {
        val result = login(password).andExpect(status().isOk)
            .andExpect(jsonPath("$.user.role").value("Student")).andReturn()
        val token = mapper.readTree(result.response.contentAsString)["token"].asString()

        assertTrue(jwtProvider.validateToken(token))
        courses("Bearer $token").andExpect(status().isOk)
    }

    @Test
    fun `incorrect password returns JSON 401`() {
        login("wrong-password").andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
    }

    @Test
    fun `disabled account login returns JSON 401 instead of server error`() {
        account.isActive = false

        login(password).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
    }

    @Test
    fun `registration is public and hashes the password`() {
        `when`(roleRepository.findByName("ROLE_STUDENT")).thenReturn(account.roles.first())
        `when`(userRepository.save(any(User::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as User).apply { id = 2L }
        }

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("""{"email":"new@example.test","password":"test-password","firstName":"Test","lastName":"User"}"""))
            .andExpect(status().isCreated).andExpect(jsonPath("$.user.id").value(2))
            .andExpect(jsonPath("$.token").isNotEmpty)
        val saved = ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(saved.capture())
        assertTrue(passwordEncoder.matches(password, saved.value.passwordHash))
    }

    @Test
    fun `authentication is not persisted to a session or next request`() {
        val result = courses(bearer()).andExpect(status().isOk).andReturn()

        assertNull(result.request.getSession(false))
        courses().andExpect(status().isUnauthorized)
    }

    private fun bearer() = "Bearer ${jwtProvider.generateToken(email, 1L, listOf("COURSE_WRITE"))}"

    private fun courses(authorization: String? = null): ResultActions {
        val request = get("/api/v1/courses")
        if (authorization != null) request.header("Authorization", authorization)
        return mvc.perform(request)
    }

    private fun createCourse(authorization: String) = mvc.perform(
        post("/api/v1/courses").header("Authorization", authorization)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(courseRequest))
    )

    private fun login(submittedPassword: String) = mvc.perform(
        post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(mapOf("email" to email, "password" to submittedPassword)))
    )

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig::class, JwtFilter::class, GlifoUserDetailsService::class,
        AuthService::class, AuthController::class, CourseController::class, GlobalExceptionHandler::class)
    class TestConfig : WebMvcConfigurer {
        @Bean fun userRepository(): UserRepository = mock(UserRepository::class.java)
        @Bean fun roleRepository(): RoleRepository = mock(RoleRepository::class.java)
        @Bean fun courseService(): CourseService = mock(CourseService::class.java)
        @Bean fun jwtProvider() = JwtProvider(TEST_SIGNING_KEY, 60_000)
        @Bean fun corsConfigurationSource(): CorsConfigurationSource = UrlBasedCorsConfigurationSource()

        override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
            builder.withJsonConverter(JacksonJsonHttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
        }
    }

    companion object {
        private const val TEST_SIGNING_KEY = "http-tests-only-signing-key-not-for-production-0123456789abcdef"
    }
}

package cr.ac.una.glifo.course

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.Enrollment
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.glossary.entity.CourseGlossary
import cr.ac.una.glifo.glossary.repository.CourseGlossaryRepository
import cr.ac.una.glifo.study.entity.StudyItem
import cr.ac.una.glifo.study.repository.StudyItemRepository
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID

/**
 * Pruebas HTTP de control de acceso en lecturas de curso sobre PostgreSQL ("glifo_test"):
 * dueño (200), estudiante matriculado ACTIVE (200), admin (200) y no miembro / matrícula
 * inactiva (404 para no revelar que el curso existe).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseAccessControlHttpTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var enrollmentRepository: EnrollmentRepository
    @Autowired lateinit var syllabusTopicRepository: SyllabusTopicRepository
    @Autowired lateinit var glossaryRepository: CourseGlossaryRepository
    @Autowired lateinit var studyItemRepository: StudyItemRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var roleRepository: RoleRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    private fun bearer(email: String, password: String): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun teacherBearer() = bearer("docente@una.ac.cr", "Docente123")
    private fun studentBearer() = bearer("estudiante@est.una.ac.cr", "Estudiante123")
    private fun adminBearer() = bearer("admin@una.ac.cr", "Admin1234")
    private fun demoCourseId() = courseRepository.findByJoinCode("GLF-CALC1")!!.id

    private fun createFreshCourse(ownerEmail: String = "docente@una.ac.cr"): Course {
        val ownerUser = userRepository.findByEmail(ownerEmail)!!
        val course = courseRepository.save(
            Course().apply {
                owner = ownerUser
                code = "EIF411"
                name = "Plataformas Móviles"
                term = "II-2026"
                joinCode = "GLF-${UUID.randomUUID().toString().take(6).uppercase()}"
                syllabusSourceUri = "programa-eif411.pdf"
                syllabusParsedAt = Instant.now()
            }
        )
        val topic = syllabusTopicRepository.save(
            SyllabusTopic().apply {
                this.course = course
                code = "U1"
                title = "Arquitectura"
                orderIndex = 1
            }
        )
        glossaryRepository.save(
            CourseGlossary().apply {
                this.course = course
                term = "MVVM"
                canonicalForm = "Model-View-ViewModel"
                kind = "TERM"
            }
        )
        studyItemRepository.save(
            StudyItem().apply {
                this.course = course
                this.syllabusTopic = topic
                kind = "FLASHCARD"
                payload = """{"schemaVersion":1,"kind":"FLASHCARD","front":"F","back":"B","difficulty":"EASY"}"""
            }
        )
        return course
    }

    private fun createSecondTeacher(): User {
        val teacherRole = roleRepository.findByName("ROLE_TEACHER")!!
        return userRepository.save(
            User().apply {
                email = "otro-docente@una.ac.cr"
                passwordHash = passwordEncoder.encode("Docente123")!!
                firstName = "Otro"
                lastName = "Docente"
                roles.add(teacherRole)
            }
        )
    }

    private fun readEndpoints(courseId: Long) = listOf(
        "/api/v1/courses/$courseId",
        "/api/v1/courses/$courseId/topics",
        "/api/v1/courses/$courseId/syllabus",
        "/api/v1/courses/$courseId/glossary",
        "/api/v1/courses/$courseId/study-items"
    )

    @Test
    fun `dueno, estudiante matriculado ACTIVE y admin leen el curso demo con 200`() {
        val courseId = demoCourseId()
        val tokens = listOf(teacherBearer(), studentBearer(), adminBearer())

        tokens.forEach { token ->
            readEndpoints(courseId).forEach { url ->
                mvc.get(url) { header("Authorization", token) }.andExpect { status { isOk() } }
            }
        }
    }

    @Test
    fun `estudiante no matriculado recibe 404 en curso ajeno, mientras dueno y admin reciben 200`() {
        val freshCourse = createFreshCourse()
        val teacherAuth = teacherBearer()
        val studentAuth = studentBearer()
        val adminAuth = adminBearer()

        readEndpoints(freshCourse.id).forEach { url ->
            mvc.get(url) { header("Authorization", teacherAuth) }.andExpect { status { isOk() } }
            mvc.get(url) { header("Authorization", adminAuth) }.andExpect { status { isOk() } }
            mvc.get(url) { header("Authorization", studentAuth) }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOT_FOUND") }
            }
        }
    }

    @Test
    fun `docente que no es dueno del curso recibe 404`() {
        createSecondTeacher()
        val freshCourse = createFreshCourse(ownerEmail = "otro-docente@una.ac.cr")
        val demoTeacherAuth = teacherBearer()
        val otherTeacherAuth = bearer("otro-docente@una.ac.cr", "Docente123")

        readEndpoints(freshCourse.id).forEach { url ->
            mvc.get(url) { header("Authorization", otherTeacherAuth) }.andExpect { status { isOk() } }
            mvc.get(url) { header("Authorization", demoTeacherAuth) }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOT_FOUND") }
            }
        }
    }

    @Test
    fun `matricula REMOVED o PENDING recibe 404 en las lecturas del curso`() {
        val student = userRepository.findByEmail("estudiante@est.una.ac.cr")!!
        val studentAuth = studentBearer()

        listOf("REMOVED", "PENDING").forEach { nonActiveStatus ->
            val freshCourse = createFreshCourse()
            enrollmentRepository.save(
                Enrollment().apply {
                    user = student
                    course = freshCourse
                    status = nonActiveStatus
                }
            )
            readEndpoints(freshCourse.id).forEach { url ->
                mvc.get(url) { header("Authorization", studentAuth) }.andExpect { status { isNotFound() } }
            }
        }
    }

    @Test
    fun `unirse por codigo sigue funcionando para no miembros y luego habilita las lecturas del curso`() {
        val freshCourse = createFreshCourse()
        val studentAuth = studentBearer()

        readEndpoints(freshCourse.id).forEach { url ->
            mvc.get(url) { header("Authorization", studentAuth) }.andExpect { status { isNotFound() } }
        }

        mvc.post("/api/v1/enrollments") {
            header("Authorization", studentAuth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"join_code":"${freshCourse.joinCode}"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.course_id") { value(freshCourse.id) }
            jsonPath("$.data.status") { value("ACTIVE") }
        }

        readEndpoints(freshCourse.id).forEach { url ->
            mvc.get(url) { header("Authorization", studentAuth) }.andExpect { status { isOk() } }
        }
    }

    @Test
    fun `sin token responde 401 e id no positivo responde 400`() {
        val courseId = demoCourseId()
        val studentAuth = studentBearer()

        readEndpoints(courseId).forEach { url ->
            mvc.get(url).andExpect { status { isUnauthorized() } }
        }
        readEndpoints(0L).forEach { url ->
            mvc.get(url) { header("Authorization", studentAuth) }.andExpect { status { isBadRequest() } }
        }
    }
}

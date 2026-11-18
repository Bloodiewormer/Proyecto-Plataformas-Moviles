package cr.ac.una.glifo.user

import cr.ac.una.glifo.user.repository.UserRepository
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper

/**
 * GET /api/v1/admin/users y PUT /api/v1/admin/users/{id}/role. Corre contra "glifo_test" y
 * cada test se revierte, así las cuentas demo conservan su rol.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserRoleHttpTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var userRepository: UserRepository

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    private val studentEmail = "estudiante@est.una.ac.cr"

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

    private fun admin() = bearer("admin@una.ac.cr", "Admin1234")
    private fun idOf(email: String) = userRepository.findByEmail(email)!!.id

    private fun changeRole(token: String, userId: Long, role: String) = mvc.put("/api/v1/admin/users/$userId/role") {
        header("Authorization", token)
        contentType = MediaType.APPLICATION_JSON
        content = """{"role":"$role"}"""
    }

    @Test
    fun `el admin lista las cuentas con su rol`() {
        mvc.get("/api/v1/admin/users") { header("Authorization", admin()) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].email") { value(hasItem(studentEmail)) }
            jsonPath("$.data[*].role") { value(hasItem("Teacher")) }
        }
    }

    @Test
    fun `el admin pasa un estudiante a docente y el login ya lo trae como docente`() {
        changeRole(admin(), idOf(studentEmail), "Teacher").andExpect {
            status { isOk() }
            jsonPath("$.data.role") { value("Teacher") }
            jsonPath("$.data.roles.length()") { value(1) }
        }

        mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$studentEmail","password":"Estudiante123"}"""
        }.andExpect { jsonPath("$.user.role") { value("Teacher") } }
    }

    @Test
    fun `el admin ve los tres roles con sus privilegios`() {
        mvc.get("/api/v1/admin/users/roles") { header("Authorization", admin()) }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(3) }
            jsonPath("$.data[*].name") { value(hasItem("ROLE_TEACHER")) }
            jsonPath("$.data[?(@.name == 'ROLE_ADMIN')].privileges[*]") { value(hasItem("ROLE_MANAGE")) }
        }
    }

    @Test
    fun `el admin no puede cambiar su propio rol`() {
        changeRole(admin(), idOf("admin@una.ac.cr"), "Student").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `un rol desconocido da 400`() {
        changeRole(admin(), idOf(studentEmail), "Root").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `un estudiante no puede listar ni cambiar roles`() {
        val student = bearer(studentEmail, "Estudiante123")
        mvc.get("/api/v1/admin/users") { header("Authorization", student) }.andExpect { status { isForbidden() } }
        changeRole(student, idOf(studentEmail), "Admin").andExpect { status { isForbidden() } }
    }
}

package cr.ac.una.glifo.user

import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper

/**
 * POST /api/v1/admin/users/reset-password: el admin le da una contraseña temporal a otra cuenta.
 * Corre contra "glifo_test" y cada test se revierte, así las cuentas demo no cambian.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminResetPasswordHttpTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var roleRepository: RoleRepository

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    private val resetUrl = "/api/v1/admin/users/reset-password"
    private val studentEmail = "estudiante@est.una.ac.cr"
    private val studentPassword = "Estudiante123"

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    // Hace login y devuelve el encabezado Authorization
    private fun bearer(email: String, password: String): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun admin() = bearer("admin@una.ac.cr", "Admin1234")

    @Test
    fun `el admin restablece la contraseña y solo entra la temporal`() {
        val body = mvc.post(resetUrl) {
            header("Authorization", admin())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$studentEmail"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.email") { value(studentEmail) }
        }.andReturn().response.contentAsString

        val temporary = json.readTree(body)["data"]["temporaryPassword"].asString()
        assertEquals(10, temporary.length)
        assertNotEquals(studentPassword, temporary)

        // La temporal sirve para entrar
        bearer(studentEmail, temporary)

        // La vieja ya no
        mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$studentEmail","password":"$studentPassword"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `un estudiante no puede restablecer contraseñas`() {
        mvc.post(resetUrl) {
            header("Authorization", bearer(studentEmail, studentPassword))
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"docente@una.ac.cr"}"""
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `un correo que no existe da 404`() {
        mvc.post(resetUrl) {
            header("Authorization", admin())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"nadie.existe@est.una.ac.cr"}"""
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `el admin no puede usarlo sobre su propia cuenta`() {
        mvc.post(resetUrl) {
            header("Authorization", admin())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"admin@una.ac.cr"}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `el admin no puede restablecer la contraseña de otro administrador`() {
        val adminRole = roleRepository.findByName("ROLE_ADMIN")!!
        val otherAdminEmail = "otro.admin@una.ac.cr"
        userRepository.saveAndFlush(User().apply {
            email = otherAdminEmail
            passwordHash = "hash-de-prueba"
            firstName = "Otro"
            lastName = "Admin"
            roles.add(adminRole)
        })

        mvc.post(resetUrl) {
            header("Authorization", admin())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$otherAdminEmail"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("No se puede restablecer la contraseña de otra cuenta de administrador") }
        }
    }

    @Test
    fun `un correo inválido da VALIDATION_FAILED`() {
        mvc.post(resetUrl) {
            header("Authorization", admin())
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"no-es-correo"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
    }

    @Test
    fun `sin token da 401`() {
        mvc.post(resetUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$studentEmail"}"""
        }.andExpect { status { isUnauthorized() } }
    }
}

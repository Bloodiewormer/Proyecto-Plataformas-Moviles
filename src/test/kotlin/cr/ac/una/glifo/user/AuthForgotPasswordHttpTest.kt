package cr.ac.una.glifo.user

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

/**
 * POST /api/v1/auth/forgot-password: sin sesión, con el correo y la contraseña nueva.
 * Corre contra "glifo_test" y cada test se revierte al terminar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthForgotPasswordHttpTest {

    @Autowired lateinit var context: WebApplicationContext

    private lateinit var mvc: MockMvc

    private val email = "estudiante@est.una.ac.cr"
    private val oldPassword = "Estudiante123"
    private val forgotUrl = "/api/v1/auth/forgot-password"

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    private fun forgot(email: String, newPassword: String) = mvc.post(forgotUrl) {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email":"$email","newPassword":"$newPassword"}"""
    }

    private fun login(password: String) = mvc.post("/api/v1/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email":"$email","password":"$password"}"""
    }

    @Test
    fun `sin token cambia la contraseña con 204 y luego solo entra la nueva`() {
        forgot(email, "NuevaClave456").andExpect { status { isNoContent() } }

        login("NuevaClave456").andExpect { status { isOk() } }
        login(oldPassword).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `correo no registrado responde 404`() {
        forgot("nadie@est.una.ac.cr", "NuevaClave456").andExpect { status { isNotFound() } }
    }

    @Test
    fun `nueva demasiado corta responde 400 y no cambia nada`() {
        forgot(email, "123").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
        login(oldPassword).andExpect { status { isOk() } }
    }
}

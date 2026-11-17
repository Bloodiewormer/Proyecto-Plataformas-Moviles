package cr.ac.una.glifo.user

import org.hamcrest.Matchers.containsString
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
 * POST /api/v1/auth/change-password: el usuario con sesión cambia su contraseña
 * dando la actual. Corre contra "glifo_test" y cada test se revierte al terminar,
 * así la cuenta demo conserva su contraseña original.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthChangePasswordHttpTest {

    @Autowired lateinit var context: WebApplicationContext

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    private val email = "estudiante@est.una.ac.cr"
    private val currentPassword = "Estudiante123"
    private val changeUrl = "/api/v1/auth/change-password"

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    // Hace login y devuelve el encabezado Authorization
    private fun bearer(password: String = currentPassword): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun changeBody(current: String, new: String) =
        """{"currentPassword":"$current","newPassword":"$new"}"""

    @Test
    fun `cambia la contraseña con 204 y luego solo entra la nueva`() {
        mvc.post(changeUrl) {
            header("Authorization", bearer())
            contentType = MediaType.APPLICATION_JSON
            content = changeBody(currentPassword, "NuevaClave456")
        }.andExpect { status { isNoContent() } }

        // La nueva sirve para entrar
        bearer("NuevaClave456")

        // La vieja ya no
        mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$currentPassword"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `contraseña actual incorrecta responde 400 y no 401`() {
        mvc.post(changeUrl) {
            header("Authorization", bearer())
            contentType = MediaType.APPLICATION_JSON
            content = changeBody("NoEsLaMia", "NuevaClave456")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(containsString("actual no es correcta")) }
        }
    }

    @Test
    fun `la nueva igual a la actual responde 400`() {
        mvc.post(changeUrl) {
            header("Authorization", bearer())
            contentType = MediaType.APPLICATION_JSON
            content = changeBody(currentPassword, currentPassword)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(containsString("distinta de la actual")) }
        }
    }

    @Test
    fun `nueva demasiado corta responde 400 de validacion`() {
        mvc.post(changeUrl) {
            header("Authorization", bearer())
            contentType = MediaType.APPLICATION_JSON
            content = changeBody(currentPassword, "123")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
    }

    @Test
    fun `sin token responde 401`() {
        mvc.post(changeUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = changeBody(currentPassword, "NuevaClave456")
        }.andExpect { status { isUnauthorized() } }
    }
}

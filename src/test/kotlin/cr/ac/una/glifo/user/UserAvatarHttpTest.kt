package cr.ac.una.glifo.user

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper

/** PUT y GET /api/v1/users/me/avatar contra "glifo_test"; cada test se revierte al terminar. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAvatarHttpTest {

    @Autowired lateinit var context: WebApplicationContext

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()
    private val avatarUrl = "/api/v1/users/me/avatar"
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)

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

    private fun student() = bearer("estudiante@est.una.ac.cr", "Estudiante123")

    private fun upload(auth: String?, bytes: ByteArray) = mvc.multipart(HttpMethod.PUT, avatarUrl) {
        file(MockMultipartFile("file", "avatar.jpg", "image/jpeg", bytes))
        auth?.let { header("Authorization", it) }
    }

    @Test
    fun `sube la foto y la devuelve con su tipo`() {
        val auth = student()
        upload(auth, jpeg).andExpect { status { isNoContent() } }

        mvc.get(avatarUrl) { header("Authorization", auth) }.andExpect {
            status { isOk() }
            content { contentType("image/jpeg") }
            content { bytes(jpeg) }
        }
    }

    @Test
    fun `reemplaza la foto anterior`() {
        val auth = student()
        upload(auth, jpeg).andExpect { status { isNoContent() } }
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 9)
        upload(auth, png).andExpect { status { isNoContent() } }

        mvc.get(avatarUrl) { header("Authorization", auth) }.andExpect {
            content { contentType("image/png") }
            content { bytes(png) }
        }
    }

    @Test
    fun `cada usuario ve solo su propia foto`() {
        upload(student(), jpeg).andExpect { status { isNoContent() } }

        mvc.get(avatarUrl) { header("Authorization", bearer("docente@una.ac.cr", "Docente123")) }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `rechaza lo que no es JPEG ni PNG`() {
        upload(student(), "<svg/>".toByteArray()).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `rechaza una imagen de mas de 1 MB`() {
        val big = jpeg + ByteArray(1024 * 1024)
        upload(student(), big).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `sin token responde 401`() {
        upload(null, jpeg).andExpect { status { isUnauthorized() } }
        mvc.get(avatarUrl).andExpect { status { isUnauthorized() } }
    }
}

package cr.ac.una.glifo.web

import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.security.JwtProvider
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper

/**
 * Issue #30: la Web API responde con el código HTTP semántico de cada caso y con un
 * ApiError cuando algo falla.
 *
 * Usa MockMvc sobre la aplicación completa (controllers, @Valid, seguridad JWT, servicios y
 * PostgreSQL "glifo_test"), así que prueba las peticiones tal como llegan desde la app o
 * desde Postman. Cada test corre en una transacción que se revierte al terminar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WebApiStatusCodesTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var jwtProvider: JwtProvider

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    private val notes = "/api/v1/notes"

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    /** Hace login con una cuenta demo de import.sql y devuelve el encabezado Authorization. */
    private fun bearer(email: String, password: String): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun student() = bearer("estudiante@est.una.ac.cr", "Estudiante123")
    private fun teacher() = bearer("docente@una.ac.cr", "Docente123")
    private fun admin() = bearer("admin@una.ac.cr", "Admin1234")
    private fun calculusId() = courseRepository.findByJoinCode("GLF-CALC1")!!.id

    /** Crea un apunte válido y devuelve su id. */
    private fun createNote(token: String): Long {
        val body = mvc.post(notes) {
            header("Authorization", token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"course_id":${calculusId()},"title":"Derivadas","class_date":"2026-10-01"}"""
        }.andReturn().response.contentAsString
        return json.readTree(body)["data"]["id"].asLong()
    }

    // ---------------- Éxitos: 200, 201, 204 ----------------

    @Test
    fun `POST crea el apunte y responde 201 con Location y el DTO de salida`() {
        mvc.post(notes) {
            header("Authorization", student())
            contentType = MediaType.APPLICATION_JSON
            content = """{"course_id":${calculusId()},"title":"Límites","class_date":"2026-10-01"}"""
        }.andExpect {
            status { isCreated() }
            header { string("Location", containsString("/api/v1/notes/")) }
            jsonPath("$.data.title") { value("Límites") }
            jsonPath("$.data.status") { value("DRAFT") }
            jsonPath("$.data.course_id") { value(calculusId()) }
        }
    }

    @Test
    fun `GET lista los apuntes con la cantidad de paginas guardadas`() {
        val token = student()
        val id = createNote(token)
        mvc.get("$notes?courseId=${calculusId()}") { header("Authorization", token) }.andExpect {
            status { isOk() }
            jsonPath("$.data[?(@.id == $id)].page_count") { value(org.hamcrest.Matchers.contains(0)) }
        }
    }

    @Test
    fun `PUT actualiza el apunte y responde 200`() {
        val token = student()
        val id = createNote(token)

        mvc.put("$notes/$id") {
            header("Authorization", token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"Derivadas parciales","class_date":"2026-10-02"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.title") { value("Derivadas parciales") }
            jsonPath("$.data.class_date") { value("2026-10-02") }
        }
    }

    @Test
    fun `DELETE responde 204 y despues el apunte ya no existe`() {
        val token = student()
        val id = createNote(token)

        mvc.delete("$notes/$id") { header("Authorization", token) }
            .andExpect { status { isNoContent() } }
        mvc.get("$notes/$id") { header("Authorization", token) }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `matricularse en un curso nuevo responde 201`() {
        val created = mvc.post("/api/v1/courses") {
            header("Authorization", teacher())
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"EIF411","name":"Plataformas Móviles","term":"II-2026"}"""
        }.andExpect {
            status { isCreated() }
            header { string("Location", containsString("/api/v1/courses/")) }
        }.andReturn().response.contentAsString
        val joinCode = json.readTree(created)["data"]["join_code"].asString()

        mvc.post("/api/v1/enrollments") {
            header("Authorization", student())
            contentType = MediaType.APPLICATION_JSON
            content = """{"join_code":"$joinCode"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.status") { value("ACTIVE") }
        }
    }

    // ---------------- 400: validación del DTO de entrada ----------------

    @Test
    fun `un DTO invalido responde 400 con un subError por campo`() {
        mvc.post(notes) {
            header("Authorization", student())
            contentType = MediaType.APPLICATION_JSON
            content = """{"course_id":0,"title":"","class_date":"01/10/2026"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.path") { value(notes) }
            jsonPath("$.subErrors[*].field") { value(hasItems("courseId", "title", "classDate")) }
            jsonPath("$.message") { value(containsString("El título es obligatorio")) }
        }
    }

    @Test
    fun `un texto mas largo que la columna es 400 y no un error de base de datos`() {
        mvc.post("/api/v1/courses") {
            header("Authorization", teacher())
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"${"X".repeat(25)}","name":"Curso","term":"II-2026"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.subErrors[0].field") { value("code") }
        }
    }

    @Test
    fun `un JSON sin un campo obligatorio responde 400 nombrando el campo`() {
        mvc.post(notes) {
            header("Authorization", student())
            contentType = MediaType.APPLICATION_JSON
            content = """{"course_id":${calculusId()},"class_date":"2026-10-01"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_REQUEST") }
            jsonPath("$.message") { value(containsString("title")) }
        }
    }

    @Test
    fun `un id que no es numero responde 400`() {
        mvc.get("$notes/abc") { header("Authorization", student()) }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("TYPE_MISMATCH") }
            }
    }

    @Test
    fun `un id no positivo responde 400 por la validacion del PathVariable`() {
        mvc.get("$notes/0") { header("Authorization", student()) }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.subErrors[0].field") { value("id") }
            }
    }

    @Test
    fun `un estado desconocido responde 400`() {
        val token = student()
        val id = createNote(token)

        mvc.patch("$notes/$id/status") {
            header("Authorization", token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"DELETED"}"""
        }.andExpect { status { isBadRequest() } }
    }

    // ---------------- 401, 403, 404, 405, 409, 415 ----------------

    @Test
    fun `sin token responde 401 con ApiError`() {
        mvc.get(notes).andExpect {
            status { isUnauthorized() }
            jsonPath("$.status") { value(401) }
            jsonPath("$.error") { value("Unauthorized") }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.message") { value("Tu sesión expiró. Iniciá sesión de nuevo.") }
            jsonPath("$.timestamp") { isString() }
            jsonPath("$.path") { value(notes) }
            jsonPath("$.subErrors") { doesNotExist() }
        }
    }

    @Test
    fun `un token expirado responde 401 UNAUTHORIZED con ApiError`() {
        val expiredToken = "Bearer " + jwtProvider.generateToken(
            email = "estudiante@est.una.ac.cr",
            userId = 1L,
            authorities = listOf("NOTE_READ_OWN"),
            expirationMsOverride = -60000L
        )

        mvc.get(notes) {
            header("Authorization", expiredToken)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.status") { value(401) }
            jsonPath("$.error") { value("Unauthorized") }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.message") { value("Tu sesión expiró. Iniciá sesión de nuevo.") }
            jsonPath("$.timestamp") { isString() }
            jsonPath("$.path") { value(notes) }
            jsonPath("$.subErrors") { doesNotExist() }
        }
    }

    @Test
    fun `un token con firma invalida responde 401 UNAUTHORIZED`() {
        val validToken = jwtProvider.generateToken(
            email = "estudiante@est.una.ac.cr",
            userId = 1L,
            authorities = listOf("NOTE_READ_OWN")
        )
        val tamperedToken = "Bearer " + validToken.substring(0, validToken.length - 6) + "xxxxxx"

        mvc.get(notes) {
            header("Authorization", tamperedToken)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.status") { value(401) }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `un estudiante consultando cursos responde 200 con COURSE_READ`() {
        mvc.get("/api/v1/courses") {
            header("Authorization", student())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
        }
    }

    @Test
    fun `un admin intentando borrar un apunte responde 403 FORBIDDEN por falta de NOTE_DELETE_OWN`() {
        val noteId = createNote(student())

        mvc.delete("$notes/$noteId") {
            header("Authorization", admin())
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.status") { value(403) }
            jsonPath("$.error") { value("Forbidden") }
            jsonPath("$.code") { value("FORBIDDEN") }
            jsonPath("$.message") { value("No tenés permiso para realizar esta acción") }
            jsonPath("$.path") { value("$notes/$noteId") }
        }
    }

    @Test
    fun `un admin intentando crear un apunte responde 403 FORBIDDEN por falta de NOTE_WRITE_OWN`() {
        mvc.post(notes) {
            header("Authorization", admin())
            contentType = MediaType.APPLICATION_JSON
            content = """{"course_id":${calculusId()},"title":"Admin Note","class_date":"2026-10-01"}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.status") { value(403) }
            jsonPath("$.code") { value("FORBIDDEN") }
            jsonPath("$.message") { value("No tenés permiso para realizar esta acción") }
        }
    }

    @Test
    fun `un estudiante creando un curso responde 403`() {
        mvc.post("/api/v1/courses") {
            header("Authorization", student())
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"X1","name":"Curso","term":"II-2026"}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
    }

    @Test
    fun `un apunte inexistente responde 404`() {
        mvc.get("$notes/999999") { header("Authorization", student()) }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Apunte no encontrado") }
            }
    }

    @Test
    fun `una ruta inexistente responde 404`() {
        mvc.get("/api/v1/no-existe") { header("Authorization", student()) }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.path") { value(endsWith("/no-existe")) }
            }
    }

    @Test
    fun `un metodo no soportado responde 405`() {
        mvc.delete("/api/v1/courses") { header("Authorization", teacher()) }
            .andExpect {
                status { isMethodNotAllowed() }
                jsonPath("$.code") { value("METHOD_NOT_ALLOWED") }
            }
    }

    @Test
    fun `una transicion de estado no permitida responde 409`() {
        val token = student()
        val id = createNote(token)

        // DRAFT → READY no está permitido: primero tiene que pasar por PROCESSING.
        mvc.patch("$notes/$id/status") {
            header("Authorization", token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"READY"}"""
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("CONFLICT") }
        }

        mvc.patch("$notes/$id/status") {
            header("Authorization", token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"PROCESSING"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.status") { value("PROCESSING") }
        }
    }

    @Test
    fun `matricularse dos veces en el mismo curso responde 409`() {
        mvc.post("/api/v1/enrollments") {
            header("Authorization", student())
            contentType = MediaType.APPLICATION_JSON
            content = """{"join_code":"GLF-CALC1"}"""
        }.andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("Ya estás matriculado en este curso") }
        }
    }

    @Test
    fun `un Content-Type que no es JSON responde 415`() {
        mvc.post(notes) {
            header("Authorization", student())
            contentType = MediaType.TEXT_PLAIN
            content = "titulo=Derivadas"
        }.andExpect {
            status { isUnsupportedMediaType() }
            jsonPath("$.code") { value("UNSUPPORTED_MEDIA_TYPE") }
        }
    }
}

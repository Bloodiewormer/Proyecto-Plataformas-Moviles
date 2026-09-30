package cr.ac.una.glifo

import cr.ac.una.glifo.course.dto.CourseResponse
import cr.ac.una.glifo.course.dto.JoinCourseRequest
import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.user.dto.AuthResponse
import cr.ac.una.glifo.user.dto.LoginRequest
import cr.ac.una.glifo.user.dto.UserResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

/**
 * El JSON de la API tiene que coincidir con lo que lee la app Android (y con el mock de
 * Mockoon que usaba antes, mockoon/Glifo.json en el repo del frontend). Estos tests no
 * necesitan base de datos: solo convierten DTOs a JSON y viceversa, con los mismos
 * módulos de Jackson que registra Spring Boot.
 */
class ApiContractJsonTest {

    private val mapper = JsonMapper.builder().findAndAddModules().build()

    @Test
    fun `login se lee desde el JSON que manda la app`() {
        val request = mapper.readValue("""{"email":"ana@una.ac.cr","password":"1234"}""", LoginRequest::class.java)
        assertEquals("ana@una.ac.cr", request.email)
    }

    @Test
    fun `respuesta de auth sin envoltorio data y con role en texto`() {
        val user = UserResponse(1, "ana@una.ac.cr", "Ana", "Mora", role = "Student", roles = listOf("ROLE_STUDENT"))
        val json = mapper.readTree(mapper.writeValueAsString(AuthResponse("jwt", user)))

        assertTrue(json.has("token"))
        assertFalse(json.has("data"))
        assertEquals("Student", json["user"]["role"].asString())
        assertTrue(json["user"].has("firstName"))
    }

    @Test
    fun `curso usa snake_case`() {
        val course = CourseResponse(1, "Cálculo I", "MAT-135", "II-2026", joinCode = "GLF-CALC1", syllabusStatus = "PENDING")
        val json = mapper.readTree(mapper.writeValueAsString(course))

        assertEquals("GLF-CALC1", json["join_code"].asString())
        assertEquals("PENDING", json["syllabus_status"].asString())
        assertFalse(json.has("joinCode"))
    }

    @Test
    fun `unirse a un curso lee join_code`() {
        val request = mapper.readValue("""{"join_code":"GLF-CALC1"}""", JoinCourseRequest::class.java)
        assertEquals("GLF-CALC1", request.joinCode)
    }

    @Test
    fun `crear apunte lee el JSON de la app`() {
        val request = mapper.readValue(
            """{"course_id":3,"title":"Derivadas","class_date":"2026-08-20"}""",
            CreateNoteRequest::class.java
        )
        assertEquals(3L, request.courseId)
        assertEquals("2026-08-20", request.classDate)
        assertEquals(null, request.syllabusTopicId)
    }

    @Test
    fun `apunte usa snake_case`() {
        val note = NoteResponse(1, 3, null, "2026-08-20", "Derivadas", "DRAFT", "2026-08-20T10:00:00Z")
        val json = mapper.readTree(mapper.writeValueAsString(note))

        assertEquals(3, json["course_id"].asInt())
        assertEquals("2026-08-20", json["class_date"].asString())
    }
}

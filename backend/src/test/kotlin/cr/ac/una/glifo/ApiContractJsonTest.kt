package cr.ac.una.glifo

import cr.ac.una.glifo.course.dto.CourseResponse
import cr.ac.una.glifo.course.dto.JoinCourseRequest
import cr.ac.una.glifo.course.dto.SyllabusDocumentResponse
import cr.ac.una.glifo.course.dto.SyllabusTopicResponse
import cr.ac.una.glifo.glossary.dto.CourseGlossaryResponse
import cr.ac.una.glifo.glossary.dto.CreateGlossarySuggestionRequest
import cr.ac.una.glifo.glossary.dto.GlossaryEntryResponse
import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.ops.dto.DeviceResponse
import cr.ac.una.glifo.ops.dto.RegisterDeviceRequest
import cr.ac.una.glifo.ops.dto.SyncCompleteRequest
import cr.ac.una.glifo.ops.dto.SyncCompleteResponse
import cr.ac.una.glifo.ops.dto.UpdateDeviceTokenRequest
import cr.ac.una.glifo.study.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.study.dto.StudyItemResponse
import cr.ac.una.glifo.user.dto.AuthResponse
import cr.ac.una.glifo.user.dto.LoginRequest
import cr.ac.una.glifo.user.dto.UserResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

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

    @Test
    fun `glosario usa el JSON que guarda la app en su copia offline`() {
        val glossary = CourseGlossaryResponse(
            courseId = 1, courseName = "Cálculo I", syllabusVersion = 2, glossaryVersion = 3,
            entries = listOf(GlossaryEntryResponse(7, "ε", "\\varepsilon", "SYMBOL"))
        )
        val json = mapper.readTree(mapper.writeValueAsString(glossary))

        assertEquals(3, json["glossary_version"].asInt())
        assertEquals(2, json["syllabus_version"].asInt())
        assertEquals("\\varepsilon", json["entries"][0]["canonical_form"].asString())
    }

    @Test
    fun `sugerencia de glosario lee el JSON de la app`() {
        val request = mapper.readValue(
            """{"original_text":"cnv","suggested_correction":"converge","note_id":4}""",
            CreateGlossarySuggestionRequest::class.java
        )
        assertEquals("converge", request.suggestedCorrection)
        assertEquals(4L, request.noteId)
    }

    @Test
    fun `generar items de estudio lee topic_ids e items_per_topic en snake_case`() {
        val request = mapper.readValue(
            """{"topic_ids":[10,11],"items_per_topic":3}""",
            GenerateStudyItemsRequest::class.java
        )
        assertEquals(listOf(10L, 11L), request.topicIds)
        assertEquals(3, request.itemsPerTopic)
    }

    @Test
    fun `StudyItemResponse serializa campos en snake_case y payload como objeto JSON en camelCase`() {
        val payloadNode = mapper.readTree(
            """{"schemaVersion":1,"kind":"FLASHCARD","front":"Límite","back":"Definición ε-δ","difficulty":"MEDIUM","sourceNoteIds":[]}"""
        )
        val item = StudyItemResponse(
            id = 42L,
            courseId = 1L,
            syllabusTopicId = 10L,
            kind = "FLASHCARD",
            payload = payloadNode,
            createdAt = Instant.parse("2026-10-25T20:00:00Z")
        )
        val json = mapper.readTree(mapper.writeValueAsString(item))

        assertEquals(42L, json["id"].asLong())
        assertEquals(1L, json["course_id"].asLong())
        assertEquals(10L, json["syllabus_topic_id"].asLong())
        assertEquals("FLASHCARD", json["kind"].asString())
        assertEquals("2026-10-25T20:00:00Z", json["created_at"].asString())

        assertFalse(json.has("courseId"))
        assertFalse(json.has("syllabusTopicId"))
        assertFalse(json.has("createdAt"))

        assertTrue(json["payload"].isObject)
        assertFalse(json["payload"].isString)
        assertEquals(1, json["payload"]["schemaVersion"].asInt())
        assertEquals("FLASHCARD", json["payload"]["kind"].asString())
        assertTrue(json["payload"].has("sourceNoteIds"))
    }

    @Test
    fun `contratos de registro de dispositivo y cierre de cola usan snake_case y nunca exponen fcm_token`() {
        val registerReq = mapper.readValue(
            """{"installation_id":"inst-12345678","fcm_token":"token-abcdef123456","platform":"ANDROID"}""",
            RegisterDeviceRequest::class.java,
        )
        assertEquals("inst-12345678", registerReq.installationId)
        assertEquals("token-abcdef123456", registerReq.fcmToken)
        assertEquals("ANDROID", registerReq.platform)
        assertFalse(registerReq.toString().contains("token-abcdef123456"))

        val rotateReq = mapper.readValue(
            """{"fcm_token":"token-rotated-9876543210"}""",
            UpdateDeviceTokenRequest::class.java,
        )
        assertEquals("token-rotated-9876543210", rotateReq.fcmToken)
        assertFalse(rotateReq.toString().contains("token-rotated-9876543210"))

        val deviceResp = DeviceResponse(
            id = 7L,
            userId = 1L,
            installationId = "inst-12345678",
            platform = "ANDROID",
            active = true,
            tokenRegistered = true,
            registeredAt = "2026-10-27T20:00:00Z",
            updatedAt = "2026-10-27T20:05:00Z",
        )
        val deviceJson = mapper.readTree(mapper.writeValueAsString(deviceResp))
        assertEquals(7L, deviceJson["id"].asLong())
        assertEquals(1L, deviceJson["user_id"].asLong())
        assertEquals("inst-12345678", deviceJson["installation_id"].asString())
        assertTrue(deviceJson["is_active"].asBoolean())
        assertTrue(deviceJson["token_registered"].asBoolean())
        assertFalse(deviceJson.has("fcm_token"))
        assertFalse(deviceJson.has("fcmToken"))

        val syncReq = mapper.readValue(
            """{"device_id":7,"idempotency_key":"550e8400-e29b-41d4-a716-446655440000","uploaded_count":3,"pending_count":0,"failed_count":0,"note_ids":[10,11]}""",
            SyncCompleteRequest::class.java,
        )
        assertEquals(7L, syncReq.deviceId)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", syncReq.idempotencyKey)
        assertEquals(3, syncReq.uploadedCount)
        assertEquals(listOf(10L, 11L), syncReq.noteIds)

        val syncResp = SyncCompleteResponse(
            syncEntryId = 15L,
            notificationId = 22L,
            deviceId = 7L,
            idempotencyKey = "550e8400-e29b-41d4-a716-446655440000",
            queueStatus = "DONE",
            notificationStatus = "ACCEPTED",
            duplicate = false,
            uploadedCount = 3,
            completedAt = "2026-10-27T20:06:00Z",
        )
        val syncJson = mapper.readTree(mapper.writeValueAsString(syncResp))
        assertEquals(15L, syncJson["sync_entry_id"].asLong())
        assertEquals(22L, syncJson["notification_id"].asLong())
        assertEquals(7L, syncJson["device_id"].asLong())
        assertEquals("ACCEPTED", syncJson["notification_status"].asString())
        assertEquals(3, syncJson["uploaded_count"].asInt())
    }

    @Test
    fun `SyllabusDocumentResponse serializa sus campos y temas en snake_case`() {
        val doc = SyllabusDocumentResponse(
            courseId = 5L,
            fileName = "silabo.pdf",
            sizeBytes = 2048L,
            parsedAt = Instant.parse("2026-11-16T20:00:00Z"),
            syllabusVersion = 2,
            topics = listOf(
                SyllabusTopicResponse(
                    id = 10L,
                    courseId = 5L,
                    parentId = null,
                    code = "U1",
                    title = "Límites",
                    orderIndex = 1
                )
            )
        )
        val json = mapper.readTree(mapper.writeValueAsString(doc))

        assertEquals(5L, json["course_id"].asLong())
        assertEquals("silabo.pdf", json["file_name"].asString())
        assertEquals(2048L, json["size_bytes"].asLong())
        assertEquals("2026-11-16T20:00:00Z", json["parsed_at"].asString())
        assertEquals(2, json["syllabus_version"].asInt())
        assertEquals(1, json["topics"].size())
        assertEquals(1, json["topics"][0]["order_index"].asInt())
        assertFalse(json.has("courseId"))
        assertFalse(json.has("fileName"))
        assertFalse(json.has("syllabusVersion"))
    }
}

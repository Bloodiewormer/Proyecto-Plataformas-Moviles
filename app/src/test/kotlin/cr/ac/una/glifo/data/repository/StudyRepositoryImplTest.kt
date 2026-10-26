package cr.ac.una.glifo.data.repository

import com.google.gson.JsonParser
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.local.dao.StudyItemDao
import cr.ac.una.glifo.data.datasource.local.entity.StudyItemEntity
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.data.dto.StudyItemResponse
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.model.QuizResultRow
import cr.ac.una.glifo.domain.model.SyllabusTopic
import cr.ac.una.glifo.domain.repository.CourseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class StudyRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val dao = mockk<StudyItemDao>(relaxed = true)
    private val courseRepository = mockk<CourseRepository> {
        coEvery { getCourseTopics(1L) } returns Result.Success(
            listOf(
                SyllabusTopic(id = 1L, courseId = 1L, code = "2.1", title = "Definición de límite"),
                SyllabusTopic(id = 2L, courseId = 1L, code = "2.2", title = "Continuidad")
            )
        )
    }

    private val repository = StudyRepositoryImpl(
        api = api,
        studyItemDao = dao,
        courseRepository = courseRepository
    )

    private fun httpException(code: Int, body: String = ""): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    private val flashcardPayload = """
        {
          "schemaVersion": 1,
          "kind": "FLASHCARD",
          "front": "Definición de límite",
          "back": "Para todo ε > 0 existe δ > 0",
          "difficulty": "MEDIUM"
        }
    """.trimIndent()

    private val mcPayload = """
        {
          "schemaVersion": 1,
          "kind": "MULTIPLE_CHOICE",
          "stem": "¿Qué propiedad tiene una función continua en un compacto?",
          "options": [
            { "id": "a", "text": "Alcanza máximo y mínimo", "isCorrect": true },
            { "id": "b", "text": "Siempre es constante", "isCorrect": false }
          ],
          "explanation": "Teorema de Weierstrass.",
          "shuffleOptions": false,
          "difficulty": "MEDIUM"
        }
    """.trimIndent()

    private val remoteFlashcard = StudyItemResponse(
        id = 1L,
        courseId = 1L,
        syllabusTopicId = 1L,
        kind = "FLASHCARD",
        payload = JsonParser.parseString(flashcardPayload).asJsonObject,
        createdAt = "2026-10-25T20:00:00Z"
    )

    private val remoteMc = StudyItemResponse(
        id = 2L,
        courseId = 1L,
        syllabusTopicId = 2L,
        kind = "MULTIPLE_CHOICE",
        payload = JsonParser.parseString(mcPayload).asJsonObject,
        createdAt = "2026-10-25T20:00:00Z"
    )

    @Test
    fun `network success caches study items in Room and returns mapped flashcards and quiz`() = runTest {
        val savedSlot = slot<List<StudyItemEntity>>()
        coEvery { api.getStudyItems(1L, null) } returns ApiResponse(data = listOf(remoteFlashcard, remoteMc))
        coEvery { dao.upsertAll(capture(savedSlot)) } returns Unit

        val flashcardsResult = repository.getFlashcards(1L)
        val quizResult = repository.getQuizQuestions(1L)

        assertTrue(flashcardsResult is Result.Success)
        val flashcards = (flashcardsResult as Result.Success).value
        assertEquals(1, flashcards.size)
        assertEquals("Definición de límite", flashcards.single().question)
        assertEquals("2.1 · Definición de límite", flashcards.single().source)

        assertTrue(quizResult is Result.Success)
        val questions = (quizResult as Result.Success).value
        assertEquals(1, questions.size)
        assertEquals("2.2", questions.single().topicTag)
        assertEquals(0, questions.single().correctIndex)

        assertEquals(2, savedSlot.captured.size)
        assertEquals(listOf(1L, 2L), savedSlot.captured.map { it.id })
    }

    @Test
    fun `NoConnection returns cached items from Room when available`() = runTest {
        val cachedEntities = listOf(
            StudyItemEntity(
                id = 1L,
                courseId = 1L,
                syllabusTopicId = 1L,
                kind = "FLASHCARD",
                payloadJson = flashcardPayload
            ),
            StudyItemEntity(
                id = 2L,
                courseId = 1L,
                syllabusTopicId = 2L,
                kind = "MULTIPLE_CHOICE",
                payloadJson = mcPayload
            )
        )
        coEvery { api.getStudyItems(1L, null) } throws IOException("Sin conexión")
        coEvery { dao.itemsForCourse(1L) } returns cachedEntities

        val flashcardsResult = repository.getFlashcards(1L)
        val quizResult = repository.getQuizQuestions(1L)

        assertTrue(flashcardsResult is Result.Success)
        assertEquals("Definición de límite", (flashcardsResult as Result.Success).value.single().question)

        assertTrue(quizResult is Result.Success)
        assertEquals("¿Qué propiedad tiene una función continua en un compacto?", (quizResult as Result.Success).value.single().prompt)
    }

    @Test
    fun `NoConnection with empty Room cache returns AppError NoConnection`() = runTest {
        coEvery { api.getStudyItems(1L, null) } throws IOException("Sin conexión")
        coEvery { dao.itemsForCourse(1L) } returns emptyList()

        val result = repository.getFlashcards(1L)

        assertEquals(Result.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `HTTP 502 from AI provider maps to AppError Server`() = runTest {
        coEvery { api.generateStudyItems(1L, any()) } throws httpException(
            502,
            """{"status":502,"code":"AI_PROVIDER_ERROR","message":"Error al comunicarse con el proveedor de IA"}"""
        )

        val result = repository.generateStudyItems(1L, listOf(1L, 2L))

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Server)
        assertEquals("502", (error as AppError.Server).code)
        assertEquals("Error al comunicarse con el proveedor de IA", error.message)
    }

    @Test
    fun `generateStudyItems sends request to API and stores returned items in Room`() = runTest {
        val requestSlot = slot<GenerateStudyItemsRequest>()
        coEvery { api.generateStudyItems(1L, capture(requestSlot)) } returns ApiResponse(
            data = listOf(remoteFlashcard, remoteMc),
            meta = mapOf("generatedTopics" to 2, "reusedTopics" to 0, "generatedItems" to 2)
        )

        val result = repository.generateStudyItems(1L, listOf(1L, 2L), itemsPerTopic = 3)

        assertEquals(Result.Success(Unit), result)
        assertEquals(listOf(1L, 2L), requestSlot.captured.topicIds)
        assertEquals(3, requestSlot.captured.itemsPerTopic)
        coVerify(exactly = 1) { dao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `getQuizResult without saved result returns null instead of fake dummy data`() = runTest {
        val result = repository.getQuizResult(99L)

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).value)
    }

    @Test
    fun `saveQuizResult and getQuizResult store and return the real result per course`() = runTest {
        val realResult = QuizResultInfo(
            score = 2,
            total = 3,
            headline = "Repasá: 2.2 · Continuidad",
            subtext = "Fallaste 1 de 3. Los 1 fallos son del mismo tema.",
            review = listOf(
                QuizResultRow(true, "1 · 2.1 · Definición de límite"),
                QuizResultRow(true, "2 · 2.1 · Definición de límite"),
                QuizResultRow(false, "3 · 2.2 · Continuidad")
            ),
            rescheduledCount = null,
            daysUntilReturn = null
        )

        repository.saveQuizResult(1L, realResult)

        val storedForCourse1 = (repository.getQuizResult(1L) as Result.Success).value
        val storedForCourse2 = (repository.getQuizResult(2L) as Result.Success).value

        assertEquals(realResult, storedForCourse1)
        assertNull(storedForCourse2)
    }
}

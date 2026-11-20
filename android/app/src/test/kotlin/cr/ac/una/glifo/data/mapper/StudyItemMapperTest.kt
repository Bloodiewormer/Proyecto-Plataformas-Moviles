package cr.ac.una.glifo.data.mapper

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import cr.ac.una.glifo.data.dto.ApiResponse
import cr.ac.una.glifo.data.dto.StudyItemResponse
import cr.ac.una.glifo.domain.model.SyllabusTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StudyItemMapperTest {

    private val gson = Gson()

    private val sampleTopics = listOf(
        SyllabusTopic(id = 1L, courseId = 1L, code = "2.1", title = "Definición de límite", orderIndex = 1),
        SyllabusTopic(id = 2L, courseId = 1L, code = "2.2", title = "Continuidad en compactos", orderIndex = 2)
    )

    private fun item(
        id: Long = 1L,
        courseId: Long = 1L,
        topicId: Long = 1L,
        kind: String,
        payloadJson: String
    ): StudyItemResponse = StudyItemResponse(
        id = id,
        courseId = courseId,
        syllabusTopicId = topicId,
        kind = kind,
        payload = JsonParser.parseString(payloadJson).asJsonObject,
        createdAt = "2026-10-25T20:00:00Z"
    )

    @Test
    fun `gson deserializes real backend StudyItemResponse json without null fields`() {
        val rawJson = """
            {
              "data": [
                {
                  "id": 10,
                  "course_id": 1,
                  "syllabus_topic_id": 2,
                  "kind": "FLASHCARD",
                  "payload": {
                    "schemaVersion": 1,
                    "kind": "FLASHCARD",
                    "front": "Definición épsilon-delta del límite",
                    "back": "Para todo ε > 0 existe δ > 0",
                    "difficulty": "MEDIUM"
                  },
                  "created_at": "2026-10-25T20:35:00Z"
                }
              ],
              "meta": {
                "generatedTopics": 1,
                "reusedTopics": 0,
                "generatedItems": 1
              }
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponse<List<StudyItemResponse>>>() {}.type
        val parsed: ApiResponse<List<StudyItemResponse>> = gson.fromJson(rawJson, type)

        assertEquals(1, parsed.data.size)
        val dto = parsed.data.single()
        assertEquals(10L, dto.id)
        assertEquals(1L, dto.courseId)
        assertEquals(2L, dto.syllabusTopicId)
        assertEquals("FLASHCARD", dto.kind)
        assertNotNull(dto.payload)
        assertEquals("Definición épsilon-delta del límite", dto.payload.get("front").asString)
    }

    @Test
    fun `maps FLASHCARD with topic label from course topics and fallback when missing`() {
        val flashcardItem = item(
            id = 1L,
            topicId = 1L,
            kind = "FLASHCARD",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "FLASHCARD",
                  "front": "¿Qué significa que f sea continua en a?",
                  "back": "Que lim_{x->a} f(x) = f(a)",
                  "latex": "\\lim_{x \\to a} f(x) = f(a)",
                  "hint": "Compara el límite con el valor",
                  "sourceNoteIds": [1],
                  "difficulty": "EASY"
                }
            """.trimIndent()
        )
        val unknownTopicItem = item(
            id = 2L,
            topicId = 99L,
            kind = "FLASHCARD",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "FLASHCARD",
                  "front": "Pregunta sin tema cargado",
                  "back": "Respuesta"
                }
            """.trimIndent()
        )

        val flashcards = StudyItemMapper.toFlashcards(
            items = listOf(flashcardItem, unknownTopicItem),
            topics = sampleTopics
        )

        assertEquals(2, flashcards.size)
        assertEquals("¿Qué significa que f sea continua en a?", flashcards[0].question)
        assertEquals("Que lim_{x->a} f(x) = f(a)", flashcards[0].answer)
        assertEquals("2.1 · Definición de límite", flashcards[0].source)
        assertEquals("Tema 99", flashcards[1].source)
    }

    @Test
    fun `maps MULTIPLE_CHOICE without shuffle preserving original option order and correctIndex`() {
        val mcItem = item(
            id = 3L,
            topicId = 2L,
            kind = "MULTIPLE_CHOICE",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "MULTIPLE_CHOICE",
                  "stem": "¿Qué condición garantiza que una función continua alcance máximo en S?",
                  "options": [
                    { "id": "a", "text": "S abierto y acotado", "isCorrect": false },
                    { "id": "b", "text": "S cerrado y acotado", "isCorrect": true },
                    { "id": "c", "text": "S conexo", "isCorrect": false }
                  ],
                  "explanation": "Teorema de Weierstrass sobre compactos.",
                  "shuffleOptions": false,
                  "difficulty": "MEDIUM"
                }
            """.trimIndent()
        )

        val questions = StudyItemMapper.toQuizQuestions(listOf(mcItem), sampleTopics)

        assertEquals(1, questions.size)
        val q = questions.single()
        assertEquals("2.2", q.topicTag)
        assertEquals("2.2 · Continuidad en compactos", q.topicLabel)
        assertEquals(listOf("S abierto y acotado", "S cerrado y acotado", "S conexo"), q.options)
        assertEquals(1, q.correctIndex)
        assertEquals("S cerrado y acotado", q.options[q.correctIndex])
    }

    @Test
    fun `maps MULTIPLE_CHOICE with shuffleOptions true and recalculates correctIndex`() {
        val mcItem = item(
            id = 4L,
            topicId = 2L,
            kind = "MULTIPLE_CHOICE",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "MULTIPLE_CHOICE",
                  "stem": "Selecciona la opción correcta:",
                  "options": [
                    { "id": "a", "text": "Opción A", "isCorrect": false },
                    { "id": "b", "text": "Opción B (correcta)", "isCorrect": true },
                    { "id": "c", "text": "Opción C", "isCorrect": false },
                    { "id": "d", "text": "Opción D", "isCorrect": false }
                  ],
                  "explanation": "B es la correcta.",
                  "shuffleOptions": true,
                  "difficulty": "HARD"
                }
            """.trimIndent()
        )

        repeat(10) { seed ->
            val q = StudyItemMapper.toQuizQuestions(
                items = listOf(mcItem),
                topics = sampleTopics,
                random = Random(seed)
            ).single()

            assertEquals(4, q.options.size)
            assertTrue(q.correctIndex in 0..3)
            assertEquals("Opción B (correcta)", q.options[q.correctIndex])
        }
    }

    @Test
    fun `maps TRUE_FALSE with correctIndex 0 for true and 1 for false`() {
        val tfTrue = item(
            id = 5L,
            topicId = 1L,
            kind = "TRUE_FALSE",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "TRUE_FALSE",
                  "statement": "Toda función derivable en un punto es continua en ese punto.",
                  "answer": true,
                  "explanation": "La derivabilidad implica continuidad.",
                  "difficulty": "EASY"
                }
            """.trimIndent()
        )
        val tfFalse = item(
            id = 6L,
            topicId = 1L,
            kind = "TRUE_FALSE",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "TRUE_FALSE",
                  "statement": "Toda función continua en un punto es derivable en ese punto.",
                  "answer": false,
                  "explanation": "f(x) = |x| es continua en 0 pero no derivable.",
                  "difficulty": "EASY"
                }
            """.trimIndent()
        )

        val questions = StudyItemMapper.toQuizQuestions(listOf(tfTrue, tfFalse), sampleTopics)

        assertEquals(2, questions.size)
        assertEquals(listOf("Verdadero", "Falso"), questions[0].options)
        assertEquals(0, questions[0].correctIndex)
        assertEquals(listOf("Verdadero", "Falso"), questions[1].options)
        assertEquals(1, questions[1].correctIndex)
    }

    @Test
    fun `seed item without sourceNoteIds shuffleOptions or hint is accepted`() {
        val seedFlashcard = item(
            id = 1L,
            topicId = 1L,
            kind = "FLASHCARD",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "FLASHCARD",
                  "front": "Definición épsilon-delta del límite",
                  "back": "Para todo ε > 0 existe δ > 0 tal que 0 < |x - a| < δ implica |f(x) - L| < ε",
                  "latex": "\\forall \\varepsilon > 0\\ \\exists \\delta > 0",
                  "difficulty": "MEDIUM"
                }
            """.trimIndent()
        )
        val seedMc = item(
            id = 2L,
            topicId = 2L,
            kind = "MULTIPLE_CHOICE",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "MULTIPLE_CHOICE",
                  "stem": "¿Qué condición garantiza un máximo en S?",
                  "options": [
                    { "id": "a", "text": "Abierto", "isCorrect": false },
                    { "id": "b", "text": "Cerrado y acotado", "isCorrect": true }
                  ],
                  "explanation": "Compacidad en R^n.",
                  "difficulty": "MEDIUM"
                }
            """.trimIndent()
        )

        val flashcards = StudyItemMapper.toFlashcards(listOf(seedFlashcard), sampleTopics)
        val questions = StudyItemMapper.toQuizQuestions(listOf(seedMc), sampleTopics)

        assertEquals(1, flashcards.size)
        assertEquals(1, questions.size)
        assertEquals(1, questions.single().correctIndex)
    }

    @Test
    fun `ignores unknown kind and items missing mandatory fields without failing the list`() {
        val unknownKind = item(
            id = 10L,
            topicId = 1L,
            kind = "OPEN_ENDED",
            payloadJson = """{"prompt": "Explica qué es un límite"}"""
        )
        val missingBackFlashcard = item(
            id = 11L,
            topicId = 1L,
            kind = "FLASHCARD",
            payloadJson = """{"schemaVersion": 1, "kind": "FLASHCARD", "front": "Solo frente"}"""
        )
        val validFlashcard = item(
            id = 12L,
            topicId = 1L,
            kind = "FLASHCARD",
            payloadJson = """{"schemaVersion": 1, "kind": "FLASHCARD", "front": "Frente válido", "back": "Reverso válido"}"""
        )
        val missingExplanationMc = item(
            id = 13L,
            topicId = 2L,
            kind = "MULTIPLE_CHOICE",
            payloadJson = """
                {
                  "stem": "Pregunta incompleta",
                  "options": [
                    { "id": "a", "text": "A", "isCorrect": true },
                    { "id": "b", "text": "B", "isCorrect": false }
                  ]
                }
            """.trimIndent()
        )
        val missingAnswerTf = item(
            id = 14L,
            topicId = 2L,
            kind = "TRUE_FALSE",
            payloadJson = """{"statement": "Enunciado sin answer", "explanation": "Explicación"}"""
        )
        val validTf = item(
            id = 15L,
            topicId = 2L,
            kind = "TRUE_FALSE",
            payloadJson = """{"statement": "Enunciado válido", "answer": true, "explanation": "Explicación"}"""
        )

        val allItems = listOf(
            unknownKind,
            missingBackFlashcard,
            validFlashcard,
            missingExplanationMc,
            missingAnswerTf,
            validTf
        )

        val flashcards = StudyItemMapper.toFlashcards(allItems, sampleTopics)
        val quizQuestions = StudyItemMapper.toQuizQuestions(allItems, sampleTopics)

        assertEquals(1, flashcards.size)
        assertEquals("Frente válido", flashcards.single().question)
        assertEquals(1, quizQuestions.size)
        assertEquals("Enunciado válido", quizQuestions.single().prompt)
    }

    @Test
    fun `TRUE_FALSE options and missing topic fallback use localized string resources when context is given`() {
        val context = io.mockk.mockk<android.content.Context> {
            io.mockk.every { getString(cr.ac.una.glifo.R.string.study_tf_true) } returns "True"
            io.mockk.every { getString(cr.ac.una.glifo.R.string.study_tf_false) } returns "False"
            io.mockk.every { getString(cr.ac.una.glifo.R.string.study_topic_fallback, 99L) } returns "Topic 99"
        }
        val tfItem = item(
            id = 20L,
            topicId = 99L,
            kind = "TRUE_FALSE",
            payloadJson = """
                {
                  "schemaVersion": 1,
                  "kind": "TRUE_FALSE",
                  "statement": "Every differentiable function is continuous.",
                  "answer": true,
                  "explanation": "Differentiability implies continuity."
                }
            """.trimIndent()
        )

        val question = StudyItemMapper.toQuizQuestionOrNull(tfItem, topic = null, context = context)!!

        assertEquals(listOf("True", "False"), question.options)
        assertEquals("Topic 99", question.topicTag)
        assertEquals("Topic 99", question.topicLabel)
    }
}

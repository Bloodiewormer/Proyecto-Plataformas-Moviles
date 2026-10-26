package cr.ac.una.glifo.data.mapper

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import cr.ac.una.glifo.data.datasource.local.entity.StudyItemEntity
import cr.ac.una.glifo.data.dto.StudyItemResponse
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.model.SyllabusTopic
import kotlin.random.Random

/**
 * Mapeador de ítems de estudio (IA-02, #44).
 *
 * Despacha manualmente el `payload` JSONB según `kind` (`FLASHCARD`, `MULTIPLE_CHOICE`, `TRUE_FALSE`)
 * siguiendo D-19 (sin sealed resolver de Gson). Ignora únicamente los ítems cuyo `kind` sea
 * desconocido o a los que les falte un campo obligatorio de su tipo, sin hacer caer la lista.
 */
object StudyItemMapper {

    const val KIND_FLASHCARD = "FLASHCARD"
    const val KIND_MULTIPLE_CHOICE = "MULTIPLE_CHOICE"
    const val KIND_TRUE_FALSE = "TRUE_FALSE"

    fun toEntity(response: StudyItemResponse): StudyItemEntity = StudyItemEntity(
        id = response.id,
        courseId = response.courseId,
        syllabusTopicId = response.syllabusTopicId,
        kind = response.kind,
        payloadJson = response.payload.toString(),
        createdAt = response.createdAt
    )

    fun toResponseOrNull(entity: StudyItemEntity): StudyItemResponse? {
        val payloadObj = runCatching {
            JsonParser.parseString(entity.payloadJson).asJsonObject
        }.getOrNull() ?: return null

        return StudyItemResponse(
            id = entity.id,
            courseId = entity.courseId,
            syllabusTopicId = entity.syllabusTopicId,
            kind = entity.kind,
            payload = payloadObj,
            createdAt = entity.createdAt
        )
    }

    fun toFlashcards(
        items: List<StudyItemResponse>,
        topics: List<SyllabusTopic> = emptyList()
    ): List<Flashcard> {
        val topicsById = topics.associateBy { it.id }
        return items.mapNotNull { item ->
            toFlashcardOrNull(item, topicsById[item.syllabusTopicId])
        }
    }

    fun entitiesToFlashcards(
        entities: List<StudyItemEntity>,
        topics: List<SyllabusTopic> = emptyList()
    ): List<Flashcard> = toFlashcards(
        items = entities.mapNotNull(::toResponseOrNull),
        topics = topics
    )

    fun toQuizQuestions(
        items: List<StudyItemResponse>,
        topics: List<SyllabusTopic> = emptyList(),
        random: Random = Random.Default
    ): List<QuizQuestion> {
        val topicsById = topics.associateBy { it.id }
        return items.mapNotNull { item ->
            toQuizQuestionOrNull(item, topicsById[item.syllabusTopicId], random)
        }
    }

    fun entitiesToQuizQuestions(
        entities: List<StudyItemEntity>,
        topics: List<SyllabusTopic> = emptyList(),
        random: Random = Random.Default
    ): List<QuizQuestion> = toQuizQuestions(
        items = entities.mapNotNull(::toResponseOrNull),
        topics = topics,
        random = random
    )

    fun toFlashcardOrNull(
        item: StudyItemResponse,
        topic: SyllabusTopic? = null
    ): Flashcard? {
        if (item.kind.trim().uppercase() != KIND_FLASHCARD) return null
        val payload = item.payload
        val front = payload.nonBlankString("front") ?: return null
        val back = payload.nonBlankString("back") ?: return null
        val (_, topicLabel) = resolveTopicLabels(item.syllabusTopicId, topic)

        return Flashcard(
            question = front,
            answer = back,
            source = topicLabel
        )
    }

    fun toQuizQuestionOrNull(
        item: StudyItemResponse,
        topic: SyllabusTopic? = null,
        random: Random = Random.Default
    ): QuizQuestion? {
        val normalizedKind = item.kind.trim().uppercase()
        val (topicTag, topicLabel) = resolveTopicLabels(item.syllabusTopicId, topic)

        return when (normalizedKind) {
            KIND_MULTIPLE_CHOICE -> parseMultipleChoice(item.payload, topicTag, topicLabel, random)
            KIND_TRUE_FALSE -> parseTrueFalse(item.payload, topicTag, topicLabel)
            else -> null
        }
    }

    private fun parseMultipleChoice(
        payload: JsonObject,
        topicTag: String,
        topicLabel: String,
        random: Random
    ): QuizQuestion? {
        val stem = payload.nonBlankString("stem") ?: return null
        val explanation = payload.nonBlankString("explanation") ?: return null
        val optionsElement = payload.get("options") ?: return null
        if (!optionsElement.isJsonArray) return null

        val parsedOptions = optionsElement.asJsonArray.mapNotNull { element ->
            parseMcOption(element)
        }
        if (parsedOptions.size < 2) return null
        if (parsedOptions.none { it.isCorrect }) return null

        val shouldShuffle = payload.booleanOrNull("shuffleOptions") == true
        val finalOptions = if (shouldShuffle) parsedOptions.shuffled(random) else parsedOptions
        val correctIndex = finalOptions.indexOfFirst { it.isCorrect }
        if (correctIndex < 0) return null

        return QuizQuestion(
            topicTag = topicTag,
            topicLabel = topicLabel,
            prompt = stem,
            options = finalOptions.map { it.text },
            correctIndex = correctIndex,
            explanation = explanation
        )
    }

    private fun parseMcOption(element: JsonElement): McOption? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val text = obj.nonBlankString("text") ?: return null
        val isCorrect = obj.booleanOrNull("isCorrect") ?: return null
        return McOption(text = text, isCorrect = isCorrect)
    }

    private fun parseTrueFalse(
        payload: JsonObject,
        topicTag: String,
        topicLabel: String
    ): QuizQuestion? {
        val statement = payload.nonBlankString("statement") ?: return null
        val answer = payload.booleanOrNull("answer") ?: return null
        val explanation = payload.nonBlankString("explanation") ?: return null

        return QuizQuestion(
            topicTag = topicTag,
            topicLabel = topicLabel,
            prompt = statement,
            options = listOf("Verdadero", "Falso"),
            correctIndex = if (answer) 0 else 1,
            explanation = explanation
        )
    }

    private fun resolveTopicLabels(
        syllabusTopicId: Long,
        topic: SyllabusTopic?
    ): Pair<String, String> {
        val fallback = "Tema $syllabusTopicId"
        if (topic == null) return fallback to fallback

        val code = topic.code.trim()
        val title = topic.title.trim()

        val tag = code.ifBlank { title.ifBlank { fallback } }
        val label = when {
            code.isNotBlank() && title.isNotBlank() ->
                if (title.startsWith(code)) title else "$code · $title"
            title.isNotBlank() -> title
            code.isNotBlank() -> code
            else -> fallback
        }
        return tag to label
    }

    private data class McOption(
        val text: String,
        val isCorrect: Boolean
    )

    private fun JsonObject.nonBlankString(memberName: String): String? {
        val element = get(memberName) ?: return null
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isString) return null
        return element.asString.trim().takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.booleanOrNull(memberName: String): Boolean? {
        val element = get(memberName) ?: return null
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isBoolean) return null
        return element.asBoolean
    }
}

fun StudyItemResponse.toEntity(): StudyItemEntity = StudyItemMapper.toEntity(this)
fun List<StudyItemResponse>.toFlashcards(topics: List<SyllabusTopic> = emptyList()): List<Flashcard> =
    StudyItemMapper.toFlashcards(this, topics)
fun List<StudyItemResponse>.toQuizQuestions(
    topics: List<SyllabusTopic> = emptyList(),
    random: Random = Random.Default
): List<QuizQuestion> = StudyItemMapper.toQuizQuestions(this, topics, random)

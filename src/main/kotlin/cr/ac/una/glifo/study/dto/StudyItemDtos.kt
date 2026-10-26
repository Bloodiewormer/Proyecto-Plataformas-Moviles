package cr.ac.una.glifo.study.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import tools.jackson.databind.JsonNode
import java.time.Instant

data class GenerateStudyItemsRequest(
    @JsonProperty("topic_ids")
    @JsonAlias("topicIds")
    @field:NotEmpty(message = "Debés indicar al menos un tema")
    @field:Size(max = 10, message = "El lote admite como máximo 10 temas")
    val topicIds: List<Long>,

    @JsonProperty("items_per_topic")
    @JsonAlias("itemsPerTopic")
    @field:Min(value = 1, message = "Se debe generar al menos 1 ítem por tema")
    @field:Max(value = 5, message = "Se pueden generar como máximo 5 ítems por tema")
    val itemsPerTopic: Int = 3
)

data class StudyItemResponse(
    val id: Long,
    @JsonProperty("course_id") val courseId: Long,
    @JsonProperty("syllabus_topic_id") val syllabusTopicId: Long,
    val kind: String,
    val payload: JsonNode,
    @JsonProperty("created_at") val createdAt: Instant
)

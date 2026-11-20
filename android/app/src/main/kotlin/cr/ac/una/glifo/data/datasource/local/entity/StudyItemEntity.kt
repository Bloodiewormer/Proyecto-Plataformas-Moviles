package cr.ac.una.glifo.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Caché local en Room de los ítems de estudio de un curso (IA-02, #44).
 * El contenido polimórfico (`FLASHCARD`, `MULTIPLE_CHOICE`, `TRUE_FALSE`) se almacena en
 * [payloadJson] como JSON serializado y se despacha manualmente en `StudyItemMapper` (D-19).
 */
@Entity(
    tableName = "study_items",
    indices = [Index(value = ["course_id"])]
)
data class StudyItemEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "course_id")
    val courseId: Long,
    @ColumnInfo(name = "syllabus_topic_id")
    val syllabusTopicId: Long,
    val kind: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null
)

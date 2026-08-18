package cr.ac.una.glifo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "study_items")
data class StudyItemEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "course_id")
    val courseId: Long,
    @ColumnInfo(name = "syllabus_topic_id")
    val syllabusTopicId: Long,
    val kind: String,
    val payload: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)

package cr.ac.una.glifo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "course_id")
    val courseId: Long,
    @ColumnInfo(name = "syllabus_topic_id")
    val syllabusTopicId: Long?,
    @ColumnInfo(name = "class_date")
    val classDate: String,
    val title: String,
    val status: String,
    val content: String?,
    @ColumnInfo(name = "content_generated_at")
    val contentGeneratedAt: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)

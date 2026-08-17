package cr.ac.una.glifo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
class CourseEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val code: String,
    val joinCode: String,
    val emoji: String?,
    val syllabusStatus: String?,
    val isSynced: Boolean = true
)

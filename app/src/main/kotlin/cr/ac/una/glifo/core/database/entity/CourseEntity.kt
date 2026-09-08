package cr.ac.una.glifo.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val code: String,
    @ColumnInfo(name = "join_code")
    val joinCode: String,
    @ColumnInfo(name = "syllabus_status")
    val syllabusStatus: String? = null,
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true
)
